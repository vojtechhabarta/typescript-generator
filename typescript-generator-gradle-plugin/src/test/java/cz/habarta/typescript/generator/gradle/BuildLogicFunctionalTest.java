package cz.habarta.typescript.generator.gradle;

import static cz.habarta.typescript.generator.gradle.GradlePluginClasspathProvider.getClasspath;
import java.io.BufferedWriter;
import java.io.File;
import static java.io.File.pathSeparator;
import static java.io.File.separator;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

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
        String classpath = "implementation-classpath=" + String.join(pathSeparator, getClasspath(testProjectDir));
        System.out.println("Classpath: " + classpath);
        writeFile(classpathFile, classpath);
        FileUtils.copyToFile(buildGradleTemplateUrl().openStream(), buildFile);
        FileUtils.copyDirectory(sourceDir, new File(testProjectDir, "src"));

        assertTrue(runGradle("assemble").getOutput().contains("BUILD SUCCESSFUL"));
        BuildResult generateTypeScript = runGradle("generateTypeScript");
        assertTrue(generateTypeScript.getOutput().contains("BUILD SUCCESSFUL"));

        String testFileName = testProjectDir.getName() + ".d.ts";
        String testFilePath = testProjectDir + separator + "build" + separator + "typescript-generator" + separator + testFileName;
        String schema = FileUtils.readFileToString(new File(testFilePath) , StandardCharsets.UTF_8);
        assertThat(schema, containsString("export interface Person {"));
        assertThat(schema, containsString("export interface PersonGroovy {"));
        assertThat(schema, containsString("export interface PersonKt {"));
        assertThat(schema, containsString("export interface PersonScala {"));

    }

    private BuildResult runGradle(String task) {
        return GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withGradleVersion("8.2.1")
                .withPluginClasspath()
                .withArguments(
                       // "-Dorg.gradle.jvmargs='-agentlib:jdwp=transport=dt_socket,server=y,suspend=y,address=*:5005'",
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

