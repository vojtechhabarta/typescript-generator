package cz.habarta.typescript.generator.gradle;

import org.jetbrains.annotations.NotNull;
import sun.misc.Unsafe;

import java.io.File;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;

public class GradlePluginClasspathProvider {
    public static List<String> getClasspath(File projectDir) throws NoSuchFieldException, IllegalAccessException {
        List<String> list = new ArrayList<>(Arrays.asList(GradlePluginClasspathProvider.getUrls(ClassLoader.getSystemClassLoader())).stream().map(URL::getFile).collect(Collectors.toList()));
        list.addAll(buildDirs(projectDir.toString()));
        return list;
    }

    public static URL[] getUrls(ClassLoader classLoader) throws NoSuchFieldException, IllegalAccessException {
        System.out.println(classLoader.getClass().getName());
        if (classLoader instanceof URLClassLoader) {
            return ((URLClassLoader) classLoader).getURLs();
        }

        // jdk9
        if (classLoader.getClass().getName().startsWith("jdk.internal.loader.ClassLoaders$")) {
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

            Field mapField = ucpField.getType().getDeclaredField("lmap");
            long mapFieldOffset = unsafe.objectFieldOffset(mapField);
            Map<String, Object> map = (Map<String, Object>) unsafe.getObject(ucpObject, mapFieldOffset);
            List<URL> all = new ArrayList<>();
            all.addAll(path);
            all.addAll(map.keySet().stream().map(url -> {
                try {
                    return new URL(url);
                } catch (MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }).collect(Collectors.toSet()));
            return all.toArray(new URL[0]);
        }
        return null;
    }

    @NotNull
    private static List<String> buildDirs(String sampleGradleDir) {
        List<String> buildDirs = new ArrayList<>();
        buildDirs.add(sampleGradleDir + File.separator +"build" + File.separator + "classes" + File.separator + "java" + File.separator + "main" + File.separator);
        buildDirs.add(sampleGradleDir + File.separator +"build"+ File.separator +"classes"+ File.separator +"groovy"+ File.separator +"main"+ File.separator);
        buildDirs.add(sampleGradleDir + File.separator +"build"+ File.separator +"classes"+ File.separator +"scala"+ File.separator +"main"+ File.separator);
        buildDirs.add(sampleGradleDir + File.separator +"build"+ File.separator +"classes"+ File.separator +"kotlin"+ File.separator +"main"+ File.separator);;
        return buildDirs;
    }

}