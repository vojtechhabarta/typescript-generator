package cz.habarta.typescript.generator.gradle;

import org.apache.commons.io.FileUtils;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import sun.misc.Unsafe;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertTrue;


public class BuildLogicFunctionalTest {


    String sampleGradle = "/Users/igor/typescript-generator/sample-gradle";
    File sourceDir = new File(sampleGradle + "/src");
    @TempDir
    File testProjectDir;
    private File buildFile;
    private File classpathFile;

    @BeforeEach
    public void setup() throws IOException {
        // settingsFile = new File(testProjectDir, "settings.gradle");

        buildFile = new File(testProjectDir, "build.gradle");
        classpathFile = new File(new File(BuildLogicFunctionalTest.class.getResource("/build.gradle.template").getPath()).getParent(), "plugin-under-test-metadata.properties");
    }


    @Test
    public void testConfigurationCache() throws IOException {
        writeFile(classpathFile, "implementation-classpath=" + getClasspath(testProjectDir).stream().collect(Collectors.joining(File.pathSeparator)));
        FileUtils.copyToFile(getClass().getResourceAsStream("/build.gradle.template"), buildFile);
        FileUtils.copyDirectory(sourceDir, new File(testProjectDir, "src"));
        assertTrue(runGradle("assemble").getOutput().contains("BUILD SUCCESSFUL"));
        BuildResult generateTypeScript = runGradle("generateTypeScript");
        assertTrue(generateTypeScript.getOutput().contains("BUILD SUCCESSFUL"));
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

    private static List<String> getClasspath(File projectDir) {
        List<String> list = new ArrayList<>(Arrays.asList(getUrls(ClassLoader.getSystemClassLoader())).stream().map(URL::getFile).collect(Collectors.toList()));
        list.addAll(buildDirs(projectDir.toString()));
        return list;
    }

    @NotNull
    private static List<String> buildDirs(String sampleGradle) {
        List<String> projectBuildDirs = new ArrayList<>();
        projectBuildDirs.add(sampleGradle + "/build/classes/java/main/");
        projectBuildDirs.add(sampleGradle + "/build/classes/groovy/main/");
        projectBuildDirs.add(sampleGradle + "/build/classes/scala/main/");
        projectBuildDirs.add(sampleGradle + "/build/classes/kotlin/main/");
        return projectBuildDirs;
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

    public static URL[] getUrls(ClassLoader classLoader) {
        if (classLoader instanceof URLClassLoader) {
            return ((URLClassLoader) classLoader).getURLs();
        }

        // jdk9
        if (classLoader.getClass().getName().startsWith("jdk.internal.loader.ClassLoaders$")) {
            try {
                Field field = Unsafe.class.getDeclaredField("theUnsafe");
                field.setAccessible(true);
                Unsafe unsafe = (Unsafe) field.get(null);

                // jdk.internal.loader.ClassLoaders.AppClassLoader.ucp
                Field ucpField = classLoader.getClass().getSuperclass().getDeclaredField("ucp");
                long ucpFieldOffset = unsafe.objectFieldOffset(ucpField);
                Object ucpObject = unsafe.getObject(classLoader, ucpFieldOffset);

                // jdk.internal.loader.URLClassPath.path
                Field pathField = ucpField.getType().getDeclaredField("path");
                long pathFieldOffset = unsafe.objectFieldOffset(pathField);
                ArrayList<URL> path = (ArrayList<URL>) unsafe.getObject(ucpObject, pathFieldOffset);

                return path.toArray(new URL[path.size()]);
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
        return null;
    }
}

