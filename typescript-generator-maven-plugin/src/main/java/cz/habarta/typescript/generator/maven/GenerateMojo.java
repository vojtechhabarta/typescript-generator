
package cz.habarta.typescript.generator.maven;

import cz.habarta.typescript.generator.*;
import java.io.File;
import java.io.FileOutputStream;
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
     * Deprecated. Use "namespace" parameter.
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
     * Prefix which will be removed from names of classes, interfaces, enums.
     * For example if set to "Json" then mapping for "JsonData" will be "Data".
     */
    @Parameter
    private String removeTypeNamePrefix;

    /**
     * Suffix which will be removed from names of classes, interfaces, enums.
     * For example if set to "JSON" then mapping for "DataJSON" will be "Data".
     */
    @Parameter
    private String removeTypeNameSuffix;

    /**
     * Prefix which will be added to names of classes, interfaces, enums.
     * For example if set to "I" then mapping for "Data" will be "IData".
     */
    @Parameter
    private String addTypeNamePrefix;

    /**
     * Suffix which will be added to names of classes, interfaces, enums.
     * For example if set to "Data" then mapping for "Person" will be "PersonData".
     */
    @Parameter
    private String addTypeNameSuffix;

    /**
     * Specifies how {@link java.util.Date} will be mapped.
     * Supported values are 'asDate', 'asNumber, 'asString'.
     * Default value is 'asDate'.
     */
    @Parameter
    private DateMapping mapDate;

    /**
     * Specifies custom class implementing {@link cz.habarta.typescript.generator.TypeProcessor}.
     * This allows to customize how Java types are mapped to TypeScript.
     * For example it is possible to implement TypeProcessor
     * for {@link com.google.common.base.Optional} from guava or for Java 8 date/time classes.
     */
    @Parameter
    private String customTypeProcessor;

    /**
     * If true TypeScript declarations (interfaces, properties) will be sorted alphabetically.
     */
    @Parameter
    private boolean sortDeclarations;

    /**
     * If true generated file will not contain comment at the top.
     * By default there is a comment with timestamp and typescript-generator version.
     * So it might be useful to suppress this comment if the file is in source control and is regenerated in build.
     */
    @Parameter
    private boolean noFileComment;


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
            settings.removeTypeNamePrefix = removeTypeNamePrefix;
            settings.removeTypeNameSuffix = removeTypeNameSuffix;
            settings.addTypeNamePrefix = addTypeNamePrefix;
            settings.addTypeNameSuffix = addTypeNameSuffix;
            settings.mapDate = mapDate;
            settings.customTypeProcessor = (TypeProcessor) classLoader.loadClass(customTypeProcessor).newInstance();
            settings.sortDeclarations = sortDeclarations;
            settings.noFileComment = noFileComment;
            new TypeScriptGenerator(settings).generateTypeScript(classList, new FileOutputStream(outputFile));

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
