
package cz.habarta.typescript.generator.gradle;

import cz.habarta.typescript.generator.*;
import cz.habarta.typescript.generator.Input;
import java.io.*;
import java.net.*;
import java.util.*;
import org.gradle.api.*;
import org.gradle.api.tasks.*;


public class GenerateTask extends DefaultTask {

    public String outputFile;
    public List<String> classes;
    public String classesFromJaxrsApplication;
    public JsonLibrary jsonLibrary;
    public String namespace;
    public String module;
    public boolean declarePropertiesAsOptional;
    public String removeTypeNamePrefix;
    public String removeTypeNameSuffix;
    public String addTypeNamePrefix;
    public String addTypeNameSuffix;
    public DateMapping mapDate;
    public String customTypeProcessor;
    public boolean sortDeclarations;
    public boolean noFileComment;


    @TaskAction
    public void generate() throws Exception {
        if (outputFile == null) {
            throw new RuntimeException("Please specify 'outputFile' property.");
        }
        System.out.println("outputFile: " + outputFile);

        // class loader
        final List<URL> urls = new ArrayList<>();
        for (Task task : getProject().getTasksByName("compileJava", false)) {
            for (File file : task.getOutputs().getFiles()) {
                urls.add(file.toURI().toURL());
            }
        }
        for (File file : getProject().getConfigurations().getAt("compile").getFiles()) {
            urls.add(file.toURI().toURL());
        }
        URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());

        // input
        final Input input = Input.fromClassNamesAndJaxrsApplication(classes, classesFromJaxrsApplication, classLoader);

        final Settings settings = new Settings();
        settings.jsonLibrary = jsonLibrary;
        settings.namespace = namespace;
        settings.module = module;
        settings.declarePropertiesAsOptional = declarePropertiesAsOptional;
        settings.removeTypeNamePrefix = removeTypeNamePrefix;
        settings.removeTypeNameSuffix = removeTypeNameSuffix;
        settings.addTypeNamePrefix = addTypeNamePrefix;
        settings.addTypeNameSuffix = addTypeNameSuffix;
        settings.mapDate = mapDate;
        if (customTypeProcessor != null) {
            settings.customTypeProcessor = (TypeProcessor) classLoader.loadClass(customTypeProcessor).newInstance();
        }
        settings.sortDeclarations = sortDeclarations;
        settings.noFileComment = noFileComment;
        new TypeScriptGenerator(settings).generateTypeScript(input, getProject().file(outputFile));
    }

}
