
package cz.habarta.typescript.generator.maven;

import cz.habarta.typescript.generator.ClassMapping;
import cz.habarta.typescript.generator.DateMapping;
import cz.habarta.typescript.generator.EnumMapping;
import cz.habarta.typescript.generator.Input;
import cz.habarta.typescript.generator.Jackson2Configuration;
import cz.habarta.typescript.generator.JsonLibrary;
import cz.habarta.typescript.generator.Logger;
import cz.habarta.typescript.generator.ModuleDependency;
import cz.habarta.typescript.generator.OptionalProperties;
import cz.habarta.typescript.generator.OptionalPropertiesDeclaration;
import cz.habarta.typescript.generator.Output;
import cz.habarta.typescript.generator.RestNamespacing;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.StringQuotes;
import cz.habarta.typescript.generator.TypeScriptFileType;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import cz.habarta.typescript.generator.TypeScriptOutputKind;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import org.apache.maven.artifact.DependencyResolutionRequiredException;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

/**
 * Generates TypeScript declaration file from specified java classes.
 * For more information see README and Wiki on GitHub.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE, threadSafe = true)
public class GenerateMojo extends AbstractMojo {

    /**
     * Path and name of generated TypeScript file.
     */
    @Parameter
    private File outputFile;

    /**
     * Output file format, can be:
     * <ul>
     * <li><code>declarationFile</code> (.d.ts)</li>
     * <li><code>implementationFile</code> (.ts)</li>
     * </ul>
     * Setting this parameter to <code>implementationFile</code> allows to generate runnable TypeScript code.<br>
     * Default value is <code>declarationFile</code>.
     */
    @Parameter
    private TypeScriptFileType outputFileType;

    /**
     * Kind of generated TypeScript output. Allowed values are:
     * <ul>
     * <li><code>global</code> - means that declarations will be in global scope or namespace (no module)</li>
     * <li><code>module</code> - means that generated file will contain top-level <code>export</code> declarations</li>
     * <li><code>ambientModule</code> - means that generated declarations will be wrapped in <code>declare module "mod" { }</code> declaration</li>
     * </ul>
     * Required parameter.
     * For more information see <a href="http://vojtechhabarta.github.io/typescript-generator/doc/ModulesAndNamespaces.html">Modules and Namespaces</a> Wiki page.
     */
    @Parameter(required = true)
    public TypeScriptOutputKind outputKind;

    /**
     * Name of generated ambient module.<br>
     * Used when {@link #outputKind} is set to <code>ambientModule</code>.
     */
    @Parameter
    private String module;

    /**
     * Generates specified namespace.<br>
     * Not recommended to combine with modules.<br>
     * Default is no namespace.
     */
    @Parameter
    private String namespace;

    /**
     * Generates TypeScript namespaces from Java packages. Default is <code>false</code>.
     */
    @Parameter
    private boolean mapPackagesToNamespaces;

    /**
     * Turns proper module into UMD (Universal Module Definition) with specified namespace.
     * Only applicable to declaration files.
     */
    @Parameter
    private String umdNamespace;

    /**
     * List of modules (generated by typescript-generator!) on which currently generated module depends on.
     * Each item of this list has
     * <ul>
     * <li><code>importFrom</code> - (required) module name in generated <code>import</code> statement, can be relative path</li>
     * <li><code>importAs</code> - (required) name that will be used when referring to the imports</li>
     * <li><code>infoJson</code> - (required) file path to the module info JSON generated by preceding typescript-generator run, see {@link #generateInfoJson} parameter</li>
     * <li><code>npmPackageName</code> - (required when generating package.json) NPM dependency package name</li>
     * <li><code>npmVersionRange</code> - (required when generating package.json) NPM dependency version (or other identification)</li>
     * </ul>
     * Only applicable when {@link #outputKind} is set to <code>module</code>.
     */
    @Parameter
    private List<ModuleDependency> moduleDependencies;

    /**
     * Classes to process.
     */
    @Parameter
    private List<String> classes;

    /**
     * Classes to process specified using glob patterns
     * so it is possible to specify package or class name suffix.
     * Glob patterns support two wildcards:
     * <ul>
     * <li>Single <code>*</code> wildcard matches any character except for <code>.</code> and <code>$</code>.</li>
     * <li>Double <code>**</code> wildcard matches any character.</li>
     * </ul>
     * For more information and examples see <a href="https://github.com/vojtechhabarta/typescript-generator/wiki/Class-Names-Glob-Patterns">Class Names Glob Patterns</a> Wiki page.
     */
    @Parameter
    private List<String> classPatterns;

    /**
     * Classes to process specified by annotations.
     */
    @Parameter
    private List<String> classesWithAnnotations;

    /**
     * Classes to process specified by implemented interface.
     */
    @Parameter
    private List<String> classesImplementingInterfaces;

    /**
     * Classes to process specified by extended superclasses.
     */
    @Parameter
    private List<String> classesExtendingClasses;

    /**
     * Scans specified JAX-RS {@link javax.ws.rs.core.Application} for classes to process.
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
     * For more information and examples see <a href="https://github.com/vojtechhabarta/typescript-generator/wiki/Class-Names-Glob-Patterns">Class Names Glob Patterns</a> Wiki page.
     */
    @Parameter
    private List<String> excludeClassPatterns;

    /**
     * If this list is not empty then only properties with any of these annotations will be included.
     */
    @Parameter
    private List<String> includePropertyAnnotations;

    /**
     * Properties with any of these annotations will be excluded.
     */
    @Parameter
    private List<String> excludePropertyAnnotations;

    /**
     * Library used in JSON classes.
     * Supported values are:
     * <ul>
     * <li><code>jackson1</code> - annotations from `org.codehaus.jackson.annotate` package</li>
     * <li><code>jackson2</code> - annotations from `com.fasterxml.jackson.annotation` package</li>
     * <li><code>jaxb</code> - annotations from `javax.xml.bind.annotation` package<li>
     * </ul>
     * Required parameter, recommended value is <code>jackson2</code>.
     */
    @Parameter(required = true)
    private JsonLibrary jsonLibrary;

    /**
     * Specifies Jackson 2 global configuration.
     * Description of individual parameters is in
     * <a href="https://github.com/vojtechhabarta/typescript-generator/blob/master/typescript-generator-core/src/main/java/cz/habarta/typescript/generator/Jackson2Configuration.java">Jackson2Configuration</a>
     * class on GitHub (latest version).
     */
    @Parameter
    private Jackson2Configuration jackson2Configuration;

    /**
     * <b>Deprecated</b>, use {@link #optionalProperties} parameter.
     */
    @Deprecated
    @Parameter
    private boolean declarePropertiesAsOptional;

    /**
     * Specifies how properties are defined to be optional.
     * Supported values are:
     * <ul>
     * <li><code>useSpecifiedAnnotations</code> - annotations specified using {@link #optionalAnnotations} parameter</li>
     * <li><code>useLibraryDefinition</code> - examples: <code>@JsonProperty(required = false)</code> when using <code>jackson2</code> library
     *   or <code>@XmlElement(required = false)</code> when using <code>jaxb</code> library</li>
     * <li><code>all</code> - all properties are optional</li>
     * </ul>
     * Default value is <code>useSpecifiedAnnotations</code>.
     */
    @Parameter
    private OptionalProperties optionalProperties;

    /**
     * Specifies how optional properties will be declared in generated file.
     * This parameter applies to properties detected as optional.
     * The detection can be specified using {@link #optionalProperties} parameter.
     * Supported values are:
     * <ul>
     * <li><code>questionMark</code> - property will be marked using <code>?</code> character as optional</li>
     * <li><code>questionMarkAndNullableType</code> - property will be optional and it will also have union with <code>null</code> value</li>
     * <li><code>nullableType</code> - property will not be optional but its type will be union with <code>null</code> value</li>
     * <li><code>nullableAndUndefinableType</code> - property will not be optional but its type will be union with <code>null</code> and <code>undefined</code> values</li>
     * <li><code>undefinableType</code> - property will not be optional but its type will be union with <code>undefined</code> value</li>
     * </ul>
     * Default value is <code>questionMark</code>.
     */
    @Parameter
    private OptionalPropertiesDeclaration optionalPropertiesDeclaration;

    /**
     * If <code>true</code> declared properties will be <code>readonly</code>.
     */
    @Parameter
    private boolean declarePropertiesAsReadOnly;

    /**
     * Prefix which will be removed from names of classes, interfaces, enums.
     * For example if set to <code>Json</code> then mapping for <code>JsonData</code> will be <code>Data</code>.
     */
    @Parameter
    private String removeTypeNamePrefix;

    /**
     * Suffix which will be removed from names of classes, interfaces, enums.
     * For example if set to <code>JSON</code> then mapping for <code>DataJSON</code> will be <code>Data</code>.
     */
    @Parameter
    private String removeTypeNameSuffix;

    /**
     * Prefix which will be added to names of classes, interfaces, enums.
     * For example if set to <code>I</code> then mapping for <code>Data</code> will be <code>IData</code>.
     */
    @Parameter
    private String addTypeNamePrefix;

    /**
     * Suffix which will be added to names of classes, interfaces, enums.
     * For example if set to <code>Data</code> then mapping for <code>Person</code> will be <code>PersonData</code>.
     */
    @Parameter
    private String addTypeNameSuffix;

    /**
     * Specifies custom TypeScript names for Java classes.
     * Multiple mappings can be specified, each using this format: <code>javaClassName:typescriptName</code>.
     * This takes precedence over other naming settings.
     * For more information and example see <a href="https://github.com/vojtechhabarta/typescript-generator/wiki/Type-Mapping">Type Mapping</a> Wiki page.
     */
    @Parameter
    private List<String> customTypeNaming;

    /**
     * Specifies JavaScript function for getting custom TypeScript names for Java classes.
     * Function can return undefined if default name should be used.<br>
     * Function signature: <code>function getName(className: string, classSimpleName: string): string | null | undefined;</code><br>
     * Example function: <code>function(name, simpleName) { if (name.startsWith('cz.')) return 'Test' + simpleName; }</code>
     */
    @Parameter
    private String customTypeNamingFunction;

    /**
     * List of files which will be referenced using triple-slash directive: <code>/// &lt;reference path="file" /></code>.
     * This can be used with {@link #customTypeMappings} to provide needed TypeScript types.
     */
    @Parameter
    private List<String> referencedFiles;

    /**
     * List of import declarations which will be added to generated output.
     * This can be used with {@link #customTypeMappings} to provide needed TypeScript types.
     */
    @Parameter
    private List<String> importDeclarations;

    /**
     * List of custom mappings.
     * Each item specifies TypeScript type which will be used for particular Java class.
     * Item format is: <code>javaClassName:typescriptType</code>.
     * For example mapping Joda-Time {@link org.joda.time.LocalDateTime} to <code>string</code> would be added as <code>org.joda.time.LocalDateTime:string</code>.
     * Mappings of generic classes must use syntax <code>com.package.MyGenericClass&lt;T1,T2&gt;:TsGenericType2&lt;T1,T2&gt;</code>.
     * Instead of <code>&lt;T1,T2&gt;</code> it is also possible to use <code>[T1,T2]</code>.
     */
    @Parameter
    private List<String> customTypeMappings;

    /**
     * List of custom type aliases.
     * Each item it this list specifies type alias name (possibly with generic parameters) and its definition.
     * Item format is: <code>name:definition</code>.
     * For example for <code>Unwrap&lt;T>:T</code> following type alias will be generated <code>type Unwrap&lt;T> = T</code>.
     * Other examples: <code>SimpleName:VeryLongGeneratedName</code>, <code>StringID&lt;T>:string</code>.
     */
    @Parameter
    private List<String> customTypeAliases;

    /**
     * Specifies how {@link java.util.Date} will be mapped.
     * Supported values are:
     * <ul>
     * <li><code>asDate</code> - type <code>Date</code></li>
     * <li><code>asNumber</code> - type <code>number</code></li>
     * <li><code>asString</code> - type <code>string</code></li>
     * </ul>
     * Default value is <code>asDate</code>.
     */
    @Parameter
    private DateMapping mapDate;

    /**
     * Specifies how enums will be mapped.
     * Supported values are:
     * <ul>
     * <li><code>asUnion</code> - creates type alias to union of string enum values</li>
     * <li><code>asInlineUnion</code> - creates union of enum values on places where the enum is used</li>
     * <li><code>asEnum</code> - creates string enum. Requires TypeScript 2.4</li>
     * <li><code>asNumberBasedEnum</code> - creates enum of named number values</li>
     * </ul>
     * Default value is <code>asUnion</code>.
     */
    @Parameter
    private EnumMapping mapEnum;

    /**
     * If <code>true</code> generated enums will not have <code>const</code> keyword.<br>
     * This can be used only in implementation files.
     */
    @Parameter
    private boolean nonConstEnums;

    /**
     * If this list is not empty, then generated enums will not have <code>const</code> keyword,
     * if the enum contains one of the annotations defined in this list.
     * See {@link #nonConstEnums}
     *
     */
    @Parameter
    private List<String> nonConstEnumAnnotations;

    /**
     * Specifies whether Java classes will be mapped to TypeScript classes or interfaces.
     * Java interfaces are always mapped as TypeScript interfaces.
     * Supported values are:
     * <ul>
     * <li><code>asInterfaces</code></li>
     * <li><code>asClasses</code></li>
     * </ul>
     * Default value is <code>asInterfaces</code>.<br>
     * Value <code>asClasses</code> can only be used in implementation files (.ts).
     * It is also possible to generate mix of classes and interfaces by setting this parameter to <code>asClasses</code> value
     * and specifying which classes should be mapped as classes using <code>mapClassesAsClassesPatterns</code> parameter.
     */
    @Parameter
    private ClassMapping mapClasses;

    /**
     * Specifies which Java classes should be mapped as TypeScript classes.
     * Classes which are matched by any of these patters are mapped as classes otherwise they are mapped as interfaces.
     * This parameter can only be used when <code>mapClasses</code> parameter is set to <code>asClasses</code> value.
     */
    @Parameter
    private List<String> mapClassesAsClassesPatterns;

    /**
     * If <code>true</code> tagged unions will not be generated for Jackson 2 polymorphic types.
     */
    @Parameter
    private boolean disableTaggedUnions;

    /**
     * If <code>true</code> Swagger annotations will not be used.
     */
    @Parameter
    private boolean ignoreSwaggerAnnotations;

    /**
     * If <code>true</code> interface for JAX-RS REST application will be generated.
     */
    @Parameter
    private boolean generateJaxrsApplicationInterface;

    /**
     * If <code>true</code> client for JAX-RS REST application will be generated.
     */
    @Parameter
    private boolean generateJaxrsApplicationClient;

    /**
     * If <code>true</code> interface for Spring REST application will be generated.
     */
    @Parameter
    private boolean generateSpringApplicationInterface;

    /**
     * If <code>true</code> client for Spring REST application will be generated.
     */
    @Parameter
    private boolean generateSpringApplicationClient;

    /**
     * If <code>true</code> Spring REST application will be loaded and scanned for classes to process.
     * It is needed to specify application class using another parameter (for example {@link #classes}).
     */
    @Parameter
    private boolean scanSpringApplication;

    /**
     * If defined Spring interface/client will be only generated for classes which possess this annotation.
     * By default all classes with @RestController annotation are processed.
     */
    @Parameter
    private String springControllerAnnotation;

    /**
     * Deprecated, use {@link #restNamespacing}.
     */
    @Deprecated
    @Parameter
    private RestNamespacing jaxrsNamespacing;

    /**
     * Deprecated, use {@link #restNamespacingAnnotation}.
     */
    @Deprecated
    @Parameter
    private String jaxrsNamespacingAnnotation;

    /**
     * Specifies how JAX-RS REST operations will be grouped into objects.
     * Supported values are:
     * <ul>
     * <li><code>singleObject</code> - means that one object with all operations will be generated</li>
     * <li><code>perResource</code> - means that for each root resource one object will be generated</li>
     * <li><code>byAnnotation</code> - means that operations will be grouped by annotation specified using {@link #jaxrsNamespacingAnnotation}</li>
     * </ul>
     * Default value is <code>singleObject</code>.
     */
    @Parameter
    private RestNamespacing restNamespacing;

    /**
     * Specifies annotation used for grouping JAX-RS REST operations.
     * Format is <code>annotationClass#annotationElement</code> where
     * <code>annotationClass</code> is fully-qualified class name and <code>annotationElement</code> is element name and defaults to <code>value</code>.
     * Examples:
     * <ul>
     * <li><code>io.swagger.annotations.Api</code></li>
     * <li><code>io.swagger.annotations.Api#value</code></li>
     * </ul>
     */
    @Parameter
    private String restNamespacingAnnotation;

    /**
     * Specifies HTTP response type in REST application.
     * Default value is <code>Promise&lt;R></code> which means data object returned asynchronously.
     * This parameter is useful for example when underlying HTTP response object (like <code>XMLHttpRequest</code> or <code>AxiosPromise</code>)
     * is returned instead of actual response data.
     */
    @Parameter
    private String restResponseType;

    /**
     * Specifies HTTP request options type in REST application.
     * By default no <code>options</code> parameter is generated.
     * Useful when passing additional parameters to underlying HTTP request method (like jQuery ajax settings or <code>AxiosRequestConfig</code>).
     * Can be specific (for example <code>AxiosRequestConfig</code>) or generic (for example <code>&lt;O></code>).
     */
    @Parameter
    private String restOptionsType;

    /**
     * Specifies custom class implementing {@link cz.habarta.typescript.generator.TypeProcessor}.
     * This allows to customize how Java types are mapped to TypeScript.
     * For example it is possible to implement TypeProcessor
     * for {@link com.google.common.base.Optional} from guava.
     */
    @Parameter
    private String customTypeProcessor;

    /**
     * If <code>true</code> TypeScript declarations (interfaces, properties) will be sorted alphabetically.
     */
    @Parameter
    private boolean sortDeclarations;

    /**
     * If <code>true</code> TypeScript type declarations (interfaces) will be sorted alphabetically.
     */
    @Parameter
    private boolean sortTypeDeclarations;

    /**
     * If <code>true</code> generated file will not contain comment at the top.
     * By default there is a comment with timestamp and typescript-generator version.
     * So it might be useful to suppress this comment if the file is in source control and is regenerated in build.
     */
    @Parameter
    private boolean noFileComment;

    /**
     * If <code>true</code> generated file will not be prevented from linting by TSLint.
     * By default there is a {@code tslint:disable} comment that will force TSLint to ignore the generated file.
     * This can be enabled to suppress this comment so that the file can be linted by TSLint.
     */
    @Parameter
    private boolean noTslintDisable;

    /**
     * If <code>true</code> generated file will not be prevented from linting by ESLint.
     * By default there is a {@code eslint-disable} comment that will force ESLint to ignore the generated file.
     * This can be enabled to suppress this comment so that the file can be linted by ESLint.
     */
    @Parameter
    private boolean noEslintDisable;

    /**
     * List of Javadoc XML files to search for documentation comments.
     * These files should be created using <code>com.github.markusbernhardt.xmldoclet.XmlDoclet</code> from <code>com.github.markusbernhardt:xml-doclet</code> artifact.
     * Javadoc comments are added to output declarations as JSDoc comments.
     * For more information see <a href="https://github.com/vojtechhabarta/typescript-generator/wiki/Javadoc">Javadoc</a> Wiki page.
     */
    @Parameter
    private List<File> javadocXmlFiles;

    /**
     * List of extensions specified as fully qualified class name.
     * Known extensions:
     * <ul>
     * <li><code>cz.habarta.typescript.generator.ext.AxiosClientExtension}</code>
     *   - generates client for JAX-RS service using Axios library, see <a href="https://github.com/vojtechhabarta/typescript-generator/wiki/JAX-RS-Application">JAX RS Application</a>Wiki page</li>
     * <li><code>cz.habarta.typescript.generator.ext.BeanPropertyPathExtension}</code>
     *   - generates type-safe property path getters</li>
     * <li><code>cz.habarta.typescript.generator.ext.TypeGuardsForJackson2PolymorphismExtension}</code></li>
     * </ul>
     * Parameter {@link #extensionsWithConfiguration} can be used in case extension needs some configuration.
     */
    @Parameter
    private List<String> extensions;

    /**
     * List of extensions with their configurations.
     * This parameter has the same purpose as {@link #extensions} parameter.
     * Each item of this list has
     * <ul>
     * <li><code>className</code> - required fully-qualified class name of the extension</li>
     * <li><code>configuration</code> - optional <code>Map</code> with <code>String</code> keys and <code>String</code> values</li>
     * </ul>
     */
    @Parameter
    private List<Settings.ConfiguredExtension> extensionsWithConfiguration;

    /**
     * The presence of any annotation in this list on a JSON property will cause
     * the typescript-generator to treat that property as optional when generating
     * the corresponding TypeScript interface.
     * Example optional annotation: <code>javax.annotation.Nullable</code>
     */
    @Parameter
    private List<String> optionalAnnotations;

    /**
     * If <code>true</code> JSON file describing generated module will be generated.
     * In following typescript-generator run this allows to generate another module which could depend on currently generated module.
     * Generated JSON file contains mapping from Java classes to TypeScript types which typescript-generator needs 
     * when the module is referenced from another module using {@link #moduleDependencies} parameter.
     * Only applicable when {@link #outputKind} is set to <code>module</code>.
     */
    @Parameter
    private boolean generateInfoJson;

    /**
     * If <code>true</code> NPM <code>package.json</code> will be generated.
     * Only applicable when {@link #outputKind} is set to <code>module</code>.
     * NPM package name and version can be specified using {@link #npmName} and {@link #npmVersion} parameters.
     */
    @Parameter
    private boolean generateNpmPackageJson;

    /**
     * Specifies NPM package name.<br>
     * Only applicable when {@link #generateNpmPackageJson} parameter is <code>true</code>.<br>
     * Default value is <code>${project.artifactId}</code>.
     */
    @Parameter
    private String npmName;

    /**
     * Specifies NPM package version.<br>
     * Only applicable when {@link #generateNpmPackageJson} parameter is <code>true</code>.<br>
     * Default value is <code>1.0.0</code>.
     */
    @Parameter
    private String npmVersion;

    /**
     * Specifies NPM "build" script.<br>
     * Only applicable when {@link #generateNpmPackageJson} parameter is <code>true</code> and generating implementation file (.ts).<br>
     * Default value is <code>tsc --module umd --moduleResolution node --typeRoots --target es5 --lib es6 --declaration --sourceMap $outputFile</code>.
     */
    @Parameter
    private String npmBuildScript;

    /**
     * Specifies how strings will be quoted.
     * Supported values are:
     * <ul>
     * <li><code>doubleQuotes</code></li>
     * <li><code>singleQuotes</code></li>
     * </ul>
     * Default value is <code>doubleQuotes</code>.
     */
    @Parameter
    private StringQuotes stringQuotes;

    /**
     * Specifies indentation string.
     * In Maven pom.xml file it is needed to set <code>xml:space</code> attribute to <code>preserve</code>.<br>
     * Example: <code><![CDATA[<indentString xml:space="preserve">  </indentString>]]></code>.
     */
    @Parameter
    private String indentString;

    /**
     * <b>Deprecated</b>, use {@link #loggingLevel} parameter.
     */
    @Parameter
    @Deprecated
    private boolean displaySerializerWarning;

    /**
     * <b>Deprecated</b>, see {@link #jackson2ModuleDiscovery} and {@link #jackson2Modules} parameters.
     */
    @Deprecated
    @Parameter
    private boolean disableJackson2ModuleDiscovery;

    /**
     * Turns on Jackson2 automatic module discovery.
     */
    @Parameter
    private boolean jackson2ModuleDiscovery;

    /**
     * Specifies Jackson2 modules to use.
     */
    @Parameter
    private List<String> jackson2Modules;

    /**
     * <b>Deprecated</b>, use {@link #loggingLevel} parameter.
     */
    @Parameter
    @Deprecated
    private boolean debug;

    /**
     * Specifies level of logging output.
     * Supported values are:
     * <ul>
     * <li><code>Debug</code></li>
     * <li><code>Verbose</code></li>
     * <li><code>Info</code></li>
     * <li><code>Warning</code></li>
     * <li><code>Error</code></li>
     * </ul>
     * Default value is <code>Verbose</code>.
     */
    @Parameter
    private Logger.Level loggingLevel;

    @Parameter(defaultValue = "${project}", readonly = true, required = true)
    private MavenProject project;

    @Parameter(defaultValue = "${project.build.directory}", readonly = true, required = true)
    private String projectBuildDirectory;

    @Override
    public void execute() {
        TypeScriptGenerator.setLogger(new Logger(loggingLevel));
        TypeScriptGenerator.printVersion();

        // class loader
        final List<URL> urls = new ArrayList<>();
        try {
            for (String element : project.getCompileClasspathElements()) {
                urls.add(new File(element).toURI().toURL());
            }
        } catch (DependencyResolutionRequiredException | IOException e) {
            throw new RuntimeException(e);
        }

        try (URLClassLoader classLoader = Settings.createClassLoader(project.getArtifactId(), urls.toArray(new URL[0]), Thread.currentThread().getContextClassLoader())) {

            // Settings
            final Settings settings = new Settings();
            if (outputFileType != null) {
                settings.outputFileType = outputFileType;
            }
            settings.outputKind = outputKind;
            settings.module = module;
            settings.namespace = namespace;
            settings.mapPackagesToNamespaces = mapPackagesToNamespaces;
            settings.umdNamespace = umdNamespace;
            settings.moduleDependencies = moduleDependencies;
            settings.setExcludeFilter(excludeClasses, excludeClassPatterns);
            settings.jsonLibrary = jsonLibrary;
            settings.setJackson2Configuration(classLoader, jackson2Configuration);
            settings.declarePropertiesAsOptional = declarePropertiesAsOptional;
            settings.optionalProperties = optionalProperties;
            settings.optionalPropertiesDeclaration = optionalPropertiesDeclaration;
            settings.declarePropertiesAsReadOnly = declarePropertiesAsReadOnly;
            settings.removeTypeNamePrefix = removeTypeNamePrefix;
            settings.removeTypeNameSuffix = removeTypeNameSuffix;
            settings.addTypeNamePrefix = addTypeNamePrefix;
            settings.addTypeNameSuffix = addTypeNameSuffix;
            settings.customTypeNaming = Settings.convertToMap(customTypeNaming);
            settings.customTypeNamingFunction = customTypeNamingFunction;
            settings.referencedFiles = referencedFiles;
            settings.importDeclarations = importDeclarations;
            settings.customTypeMappings = Settings.convertToMap(customTypeMappings);
            settings.customTypeAliases = Settings.convertToMap(customTypeAliases);
            settings.mapDate = mapDate;
            settings.mapEnum = mapEnum;
            settings.nonConstEnums = nonConstEnums;
            settings.loadNonConstEnumAnnotations(classLoader, nonConstEnumAnnotations);
            settings.mapClasses = mapClasses;
            settings.mapClassesAsClassesPatterns = mapClassesAsClassesPatterns;
            settings.disableTaggedUnions = disableTaggedUnions;
            settings.ignoreSwaggerAnnotations = ignoreSwaggerAnnotations;
            settings.generateJaxrsApplicationInterface = generateJaxrsApplicationInterface;
            settings.generateJaxrsApplicationClient = generateJaxrsApplicationClient;
            settings.generateSpringApplicationInterface = generateSpringApplicationInterface;
            settings.generateSpringApplicationClient = generateSpringApplicationClient;
            settings.scanSpringApplication = scanSpringApplication;
            settings.springControllerAnnotation = springControllerAnnotation;
            settings.jaxrsNamespacing = jaxrsNamespacing;
            settings.setJaxrsNamespacingAnnotation(classLoader, jaxrsNamespacingAnnotation);
            settings.restNamespacing = restNamespacing;
            settings.setRestNamespacingAnnotation(classLoader, restNamespacingAnnotation);
            settings.restResponseType = restResponseType;
            settings.setRestOptionsType(restOptionsType);
            settings.loadCustomTypeProcessor(classLoader, customTypeProcessor);
            settings.sortDeclarations = sortDeclarations;
            settings.sortTypeDeclarations = sortTypeDeclarations;
            settings.noFileComment = noFileComment;
            settings.noTslintDisable = noTslintDisable;
            settings.noEslintDisable = noEslintDisable;
            settings.javadocXmlFiles = javadocXmlFiles;
            settings.loadExtensions(classLoader, extensions, extensionsWithConfiguration);
            settings.loadIncludePropertyAnnotations(classLoader, includePropertyAnnotations);
            settings.loadExcludePropertyAnnotations(classLoader, excludePropertyAnnotations);
            settings.loadOptionalAnnotations(classLoader, optionalAnnotations);
            settings.generateInfoJson = generateInfoJson;
            settings.generateNpmPackageJson = generateNpmPackageJson;
            settings.npmName = npmName == null && generateNpmPackageJson ? project.getArtifactId() : npmName;
            settings.npmVersion = npmVersion == null && generateNpmPackageJson ? settings.getDefaultNpmVersion() : npmVersion;
            settings.npmBuildScript = npmBuildScript;
            settings.setStringQuotes(stringQuotes);
            settings.setIndentString(indentString);
            settings.displaySerializerWarning = displaySerializerWarning;
            settings.debug = debug;
            settings.disableJackson2ModuleDiscovery = disableJackson2ModuleDiscovery;
            settings.jackson2ModuleDiscovery = jackson2ModuleDiscovery;
            settings.loadJackson2Modules(classLoader, jackson2Modules);
            settings.classLoader = classLoader;

            final Input.Parameters parameters = new Input.Parameters();
            parameters.classNames = classes;
            parameters.classNamePatterns = classPatterns;
            parameters.classesWithAnnotations = classesWithAnnotations;
            parameters.classesImplementingInterfaces = classesImplementingInterfaces;
            parameters.classesExtendingClasses = classesExtendingClasses;
            parameters.jaxrsApplicationClassName = classesFromJaxrsApplication;
            parameters.automaticJaxrsApplication = classesFromAutomaticJaxrsApplication;
            parameters.isClassNameExcluded = settings.getExcludeFilter();
            parameters.classLoader = classLoader;
            parameters.debug = loggingLevel == Logger.Level.Debug;

            final File output = outputFile != null
                    ? outputFile
                    : new File(new File(projectBuildDirectory, "typescript-generator"), project.getArtifactId() + settings.getExtension());
            settings.validateFileName(output);

            new TypeScriptGenerator(settings).generateTypeScript(Input.from(parameters), Output.to(output));

        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
