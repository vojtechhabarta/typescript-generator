package cz.habarta.typescript.generator.gradle;

import static org.gradle.testkit.runner.GradleRunner.create;
import static org.junit.jupiter.api.Assertions.*;

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;


public class BuildLogicFunctionalTest {

    @TempDir File testProjectDir;
    private File settingsFile;
    private File buildFile;

    @BeforeEach
    public void setup() {
        // settingsFile = new File(testProjectDir, "settings.gradle");
        buildFile = new File(testProjectDir, "build.gradle");
    }



    @Test
    public void testConfigurationCache() throws IOException {

        String buildFileContent = "'prepareBuildGradleFile(\n" +
                "                plugins {\n" +
                "        id 'java'\n" +
                "                id 'groovy'\n" +
                "                id \"org.jetbrains.kotlin.jvm\" version \"1.8.10\"\n" +
                "                id 'scala'}\n" +
                "\n" +
                "\n" +
                "    apply plugin: cz.habarta.typescript.generator.gradle.TypeScriptGeneratorPlugin\n" +
                "\n" +
                "        version = '3.0'\n" +
                "        sourceCompatibility = 11\n" +
                "        targetCompatibility = 11\n" +
                "\n" +
                "        repositories {\n" +
                "        mavenCentral()\n" +
                "        }\n" +
                "\n" +
                "    generateTypeScript {\n" +
                "        classes = [\n" +
                "            'cz.habarta.typescript.generator.sample.Person',\n" +
                "            'cz.habarta.typescript.generator.sample.PersonGroovy',\n" +
                "            'cz.habarta.typescript.generator.sample.PersonKt',\n" +
                "            'cz.habarta.typescript.generator.sample.PersonScala',\n" +
                "        ]\n" +
                "        jsonLibrary = 'jackson2'\n" +
                "        outputKind = 'module'\n" +
                "        excludeClasses = [\n" +
                "            'groovy.lang.GroovyObject',\n" +
                "            'groovy.lang.MetaClass',\n" +
                "            'java.io.Serializable',\n" +
                "            'scala.Equals',\n" +
                "            'scala.Product',\n" +
                "            'scala.Serializable',\n" +
                "        ]\n" +
                "        jackson2Modules = [\n" +
                "            'com.fasterxml.jackson.module.scala.DefaultScalaModule',\n" +
                "            'com.fasterxml.jackson.module.kotlin.KotlinModule',\n" +
                "        ]\n" +
                "    }\n" +
                "\n" +
                "    build.dependsOn generateTypeScript";
        writeFile(buildFile, buildFileContent);

        BuildResult result = create()
                .withProjectDir(testProjectDir)
                .withGradleVersion("8.1")
                .withPluginClasspath()
                .withDebug(true)
                .withArguments(
                        "--stacktrace",
                        "--info",
                        "--configuration-cache",
                        "generateJava",
                        "build"
                )
                .build();

        assertTrue(result.getOutput().contains("BUILD SUCCESSFUL"));
    }

    private void writeFile(File destination, String content) throws IOException {
        BufferedWriter output = null;
        try {
            output = new BufferedWriter(new FileWriter(destination));
            output.write(content);
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }
}

