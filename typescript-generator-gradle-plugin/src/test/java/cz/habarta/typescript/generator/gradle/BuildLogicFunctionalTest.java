package cz.habarta.typescript.generator.gradle;

import org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;

import static cz.habarta.typescript.generator.gradle.GradlePluginClasspathProvider.getClasspath;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.containsString;


public class BuildLogicFunctionalTest {

    String sampleGradle = "../../typescript-generator/sample-gradle";
    File sourceDir = new File(sampleGradle + "/src");
    @TempDir
    File testProjectDir;
    private File buildFile;
    private File classpathFile;

    @BeforeEach
    public void setup() {
        buildFile = new File(testProjectDir, "build.gradle");
        classpathFile = new File(buildGradleTemplate().getParent(), "plugin-under-test-metadata.properties");
    }

    @Test
    public void shouldWorkWithConfigurationCache() throws IOException, NoSuchFieldException, IllegalAccessException {
        String classpath = "implementation-classpath=" + String.join(File.pathSeparator, getClasspath(testProjectDir));
        System.out.println("Classpath: " + classpath);
        writeFile(classpathFile, classpath);
        FileUtils.copyToFile(buildGradleTemplateUrl().openStream(), buildFile);
        FileUtils.copyDirectory(sourceDir, new File(testProjectDir, "src"));

        assertTrue(runGradle("assemble").getOutput().contains("BUILD SUCCESSFUL"));
        BuildResult generateTypeScript = runGradle("generateTypeScript");
        assertTrue(generateTypeScript.getOutput().contains("BUILD SUCCESSFUL"));

        String testFileName = testProjectDir.getName() + ".d.ts";
        String testFilePath = testProjectDir.toString() + "/build/typescript-generator/" + testFileName;
        String schema = FileUtils.readFileToString(new File(testFilePath) , StandardCharsets.UTF_8);
        assertThat(schema, containsString("export interface Person {\n"));
        assertThat(schema, containsString("export interface PersonGroovy {\n"));
        assertThat(schema, containsString("export interface PersonKt {\n"));
        assertThat(schema, containsString("export interface PersonScala {\n"));

    }

    private BuildResult runGradle(String task) {
        return GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withGradleVersion("8.2.1")
                .withPluginClasspath()
                .withArguments(
                        "--stacktrace",
                        "--info",
                        "--configuration-cache",
                        task
                )
                .build();
    }

    @NotNull
    private static File buildGradleTemplate() {
        return new File(buildGradleTemplateUrl().getPath());
    }

    @Nullable
    private static URL buildGradleTemplateUrl() {
        return BuildLogicFunctionalTest.class.getResource("/build.gradle.template");
    }

    private void writeFile(File destination, String content) throws IOException {
        try (BufferedWriter output = new BufferedWriter(new FileWriter(destination))) {
            output.write(content);
        }
    }
}

