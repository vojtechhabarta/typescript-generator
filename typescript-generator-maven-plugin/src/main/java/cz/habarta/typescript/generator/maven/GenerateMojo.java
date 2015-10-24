
package cz.habarta.typescript.generator.maven;

import cz.habarta.typescript.generator.*;
import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.logging.Logger;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

/**
 * Generates TypeScript declaration file from specified java classes.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GenerateMojo extends AbstractMojo {

    private final Logger logger = Logger.getLogger(getClass().getName());

    /**
     * Output TypeScript declaration file.
     */
    @Parameter(required = true)
    private File outputFile;

    /**
     * JSON classes to process.
     */
    @Parameter(required = true)
    private List<String> classes;

    /**
     * Library used in JSON classes.
     * Supported values are 'jackson1', 'jackson2'.
     * Default value is 'jackson1'.
     */
    @Parameter
    private JsonLibrary jsonLibrary;

    /**
     * @deprecated Use {@link #namespace} instead.
     */
    @Parameter
    @Deprecated
    private String moduleName;

    /**
     * TypeScript namespace (previously called "internal module") of generated declarations.
     */
    @Parameter
    private String namespace;

    /**
     * TypeScript module name (previously called "external module") of generated declarations.
     */
    @Parameter
    private String module;

    /**
     * If true declared properties will be optional.
     */
    @Parameter
    private boolean declarePropertiesAsOptional;

    /**
     * Suffix which will be removed from names of classes, interfaces, enums.
     * Type names which don't end with this suffix will not change.
     */
    @Parameter
    private String removeTypeNameSuffix;

    /**
     * Specifies how {@link java.util.Date} will be mapped.
     * Supported values are 'asDate', 'asNumber, 'asString'.
     * Default value is 'asDate'.
     */
    @Parameter
    private DateMapping mapDate;


    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() {
        try {
            logger.info("outputFile: " + outputFile);
            logger.info("classes: " + classes);

            // class loader
            final List<URL> urls = new ArrayList<>();
            for (String element : project.getCompileClasspathElements()) {
                urls.add(new File(element).toURI().toURL());
            }
            URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());

            // classes
            final List<Class<?>> classList = new ArrayList<>();
            for (String className : classes) {
                classList.add(classLoader.loadClass(className));
            }

            final Settings settings = new Settings();
            settings.jsonLibrary = jsonLibrary;
            settings.namespace = namespace != null ? namespace : moduleName;
            settings.module = module;
            settings.declarePropertiesAsOptional = declarePropertiesAsOptional;
            settings.removeTypeNameSuffix = removeTypeNameSuffix;
            settings.mapDate = mapDate;
            TypeScriptGenerator.generateTypeScript(classList, settings, outputFile);

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
