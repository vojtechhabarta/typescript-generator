
package cz.habarta.typescript.generator.maven;

import cz.habarta.typescript.generator.*;
import java.io.*;
import java.net.*;
import java.util.*;
import org.apache.maven.artifact.*;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.*;
import org.apache.maven.project.MavenProject;

/**
 * Generates TypeScript declaration file from specified java classes.
 * For more information see README and Wiki on GitHub.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GenerateMojo extends AbstractMojo {

    /**
     * Path and name of generated TypeScript file.
     * Required parameter.
     */
    @Parameter(required = true)
    private File outputFile;

    /**
     * Output file format, can be 'declarationFile' (.d.ts) or 'implementationFile' (.ts).
     * Setting this parameter to 'implementationFile' allows to generate runnable TypeScript code.
     * Default value is 'declarationFile'.
     */
    @Parameter
    private TypeScriptFileType outputFileType;

    /**
     * Kind of generated TypeScript output, allowed values are 'global', 'module' or 'ambientModule'.
     * Value 'global' means that declarations will be in global scope or namespace (no module).
     * Value 'module' means that generated file will contain top-level 'export' declarations.
     * Value 'ambientModule' means that generated declarations will be wrapped in 'declare module "mod" { }' declaration.
     * Required parameter.
     * For more information see Wiki page 'http://vojtechhabarta.github.io/typescript-generator/doc/ModulesAndNamespaces.html'.
     */
    @Parameter(required = true)
    private TypeScriptOutputKind outputKind;

    /**
     * Name of generated ambient module.
     * Used when 'outputKind' is set to 'ambientModule'.
     */
    @Parameter
    private String module;

    /**
     * Generates specified namespace. Not recommended to combine with modules. Default is no namespace.
     */
    @Parameter
    private String namespace;

    /**
     * Turns proper module into UMD (Universal Module Definition) with specified namespace.
     * Only applicable to declaration files.
     */
    @Parameter
    private String umdNamespace;

    /**
     * JSON classes to process.
     */
    @Parameter
    private List<String> classes;

    /**
     * JSON classes to process specified using glob patterns
     * so it is possible to specify package or class name suffix.
     * Glob patterns support two wildcards:
     * Single "*" wildcard matches any character except for "." and "$".
     * Double "**" wildcard matches any character.
     * For more information and examples see Wiki page 'https://github.com/vojtechhabarta/typescript-generator/wiki/Class-Names-Glob-Patterns'.
     */
    @Parameter
    private List<String> classPatterns;

    /**
     * Scans specified JAX-RS {@link javax.ws.rs.core.Application} for JSON classes to process.
     * Parameter contains fully-qualified class name.
     * It is possible to exclude particular REST resource classes using {@link #excludeClasses} parameter.
     */
    @Parameter
    private String classesFromJaxrsApplication;

    /**
     * Scans JAX-RS resources for JSON classes to process.
     * It is possible to exclude particular REST resource classes using {@link #excludeClasses} parameter.
     */
    @Parameter
    private boolean classesFromAutomaticJaxrsApplication;

    /**
     * List of classes excluded from processing.
     */
    @Parameter
    private List<String> excludeClasses;

    /**
     * Excluded classes specified using glob patterns.
     */
    @Parameter
    private List<String> excludeClassPatterns;

    /**
     * If this list is not empty then TypeScript will only be generated for
     * methods with one of the annotations defined in this list
     */
    @Parameter
    private List<String> includePropertyAnnotations;

    /**
     * Library used in JSON classes.
     * Supported values are
     * 'jackson1' (annotations from 'org.codehaus.jackson.annotate' package),
     * 'jackson2' (annotations from 'com.fasterxml.jackson.annotation' package),
     * `jaxb` (annotations from 'javax.xml.bind.annotation' package).
     * Required parameter, recommended value is 'jackson2'.
     */
    @Parameter(required = true)
    private JsonLibrary jsonLibrary;

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
     * Specifies custom TypeScript names for Java classes.
     * Multiple mappings can be specified, each using this format: "javaClassName:typescriptName".
     * This takes precedence over other naming settings.
     */
    @Parameter
    private List<String> customTypeNaming;

    /**
     * Specifies JavaScript function for getting custom TypeScript names for Java classes.
     * Function can return undefined if default name should be used.
     * Function signature: <code>function getName(className: string, classSimpleName: string): string | null | undefined;</code>
     * Example function: <code>function(name, simpleName) { if (name.startsWith('cz.')) return 'Test' + simpleName; }</code>
     */
    @Parameter
    private String customTypeNamingFunction;

    /**
     * List of files which will be referenced using triple-slash directive: /// &lt;reference path="file" />.
     * This can be used with "customTypeMappings" to provide needed TypeScript types.
     */
    @Parameter
    private List<String> referencedFiles;

    /**
     * List of import declarations which will be added to generated output.
     * This can be used with "customTypeMappings" to provide needed TypeScript types.
     */
    @Parameter
    private List<String> importDeclarations;

    /**
     * List of custom mappings.
     * Each item specifies TypeScript type which will be used for particular Java class.
     * Item format is: "javaClass:typescriptType".
     * For example mapping "ZonedDateTime" to "string" would be added as "java.time.ZonedDateTime:string".
     */
    @Parameter
    private List<String> customTypeMappings;

    /**
     * Specifies how {@link java.util.Date} will be mapped.
     * Supported values are 'asDate', 'asNumber', 'asString'.
     * Default value is 'asDate'.
     */
    @Parameter
    private DateMapping mapDate;

    /**
     * Specifies how enums will be mapped.
     * Supported values are 'asUnion', 'asInlineUnion', 'asNumberBasedEnum'.
     * Default value is 'asUnion'.
     * Value 'asUnion' creates type alias to union of string enum values.
     * Value 'asInlineUnion' creates union of enum values on places where the enum is used.
     * Value 'asNumberBasedEnum' creates enum of named number values.
     */
    @Parameter
    private EnumMapping mapEnum;

    /**
     * Specifies whether classes will be mapped to classes or interfaces.
     * Supported values are 'asInterfaces', 'asClasses'.
     * Default value is 'asInterfaces'.
     * Value 'asClasses' can only be used in implementation files (.ts).
     */
    @Parameter
    private ClassMapping mapClasses;

    /**
     * If true tagged unions will not be generated for Jackson 2 polymorphic types.
     */
    @Parameter
    private boolean disableTaggedUnions;

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
     * If true TypeScript type declarations (interfaces) will be sorted alphabetically.
     */
    @Parameter
    private boolean sortTypeDeclarations;

    /**
     * If true generated file will not contain comment at the top.
     * By default there is a comment with timestamp and typescript-generator version.
     * So it might be useful to suppress this comment if the file is in source control and is regenerated in build.
     */
    @Parameter
    private boolean noFileComment;

    /**
     * List of Javadoc XML files to search for documentation comments.
     * These files should be created using "com.github.markusbernhardt.xmldoclet.XmlDoclet" (com.github.markusbernhardt:xml-doclet).
     * Javadoc comments are added to output declarations as JSDoc comments.
     * For more information see Wiki page 'https://github.com/vojtechhabarta/typescript-generator/wiki/Javadoc'.
     */
    @Parameter
    private List<File> javadocXmlFiles;

    /**
     * List of extensions specified as fully qualified class name.
     * Known extensions:
     * cz.habarta.typescript.generator.ext.TypeGuardsForJackson2PolymorphismExtension
     */
    @Parameter
    private List<String> extensions;

    /**
     * The presence of any annotation in this list on a JSON property will cause
     * the typescript-generator to treat that property as optional when generating
     * the corresponding TypeScript interface.
     * Example optional annotation: @javax.annotation.Nullable
     */
    @Parameter
    private List<String> optionalAnnotations;

    /**
     * Specifies how strings will be quoted.
     * Supported values are 'doubleQuotes', 'singleQuotes'.
     * Default value is 'doubleQuotes'.
     */
    @Parameter
    private StringQuotes stringQuotes;

    /**
     * Display warnings when bean serializer is not found.
     */
    @Parameter(defaultValue = "true")
    private boolean displaySerializerWarning;

    /**
     * Turns off Jackson2 automatic module discovery.
     */
    @Parameter
    private boolean disableJackson2ModuleDiscovery;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Override
    public void execute() {
        try {
            TypeScriptGenerator.printVersion();

            // class loader
            final List<URL> urls = new ArrayList<>();
            for (String element : project.getCompileClasspathElements()) {
                urls.add(new File(element).toURI().toURL());
            }
            final URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader());

            // Settings
            final Settings settings = new Settings();
            if (outputFileType != null) {
                settings.outputFileType = outputFileType;
            }
            settings.outputKind = outputKind;
            settings.module = module;
            settings.namespace = namespace;
            settings.umdNamespace = umdNamespace;
            settings.setExcludeFilter(excludeClasses, excludeClassPatterns);
            settings.jsonLibrary = jsonLibrary;
            settings.declarePropertiesAsOptional = declarePropertiesAsOptional;
            settings.removeTypeNamePrefix = removeTypeNamePrefix;
            settings.removeTypeNameSuffix = removeTypeNameSuffix;
            settings.addTypeNamePrefix = addTypeNamePrefix;
            settings.addTypeNameSuffix = addTypeNameSuffix;
            settings.customTypeNaming = Settings.convertToMap(customTypeNaming);
            settings.customTypeNamingFunction = customTypeNamingFunction;
            settings.referencedFiles = referencedFiles;
            settings.importDeclarations = importDeclarations;
            settings.customTypeMappings = Settings.convertToMap(customTypeMappings);
            settings.mapDate = mapDate;
            settings.mapEnum = mapEnum;
            settings.mapClasses = mapClasses;
            settings.disableTaggedUnions = disableTaggedUnions;
            settings.loadCustomTypeProcessor(classLoader, customTypeProcessor);
            settings.sortDeclarations = sortDeclarations;
            settings.sortTypeDeclarations = sortTypeDeclarations;
            settings.noFileComment = noFileComment;
            settings.javadocXmlFiles = javadocXmlFiles;
            settings.loadExtensions(classLoader, extensions);
            settings.loadIncludePropertyAnnotations(classLoader, includePropertyAnnotations);
            settings.loadOptionalAnnotations(classLoader, optionalAnnotations);
            settings.setStringQuotes(stringQuotes);
            settings.displaySerializerWarning = displaySerializerWarning;
            settings.disableJackson2ModuleDiscovery = disableJackson2ModuleDiscovery;
            settings.classLoader = classLoader;
            settings.validateFileName(outputFile);

            // TypeScriptGenerator
            new TypeScriptGenerator(settings).generateTypeScript(
                    Input.fromClassNamesAndJaxrsApplication(classes, classPatterns, classesFromJaxrsApplication, classesFromAutomaticJaxrsApplication, settings.getExcludeFilter(), classLoader),
                    Output.to(outputFile)
            );

        } catch (DependencyResolutionRequiredException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}
