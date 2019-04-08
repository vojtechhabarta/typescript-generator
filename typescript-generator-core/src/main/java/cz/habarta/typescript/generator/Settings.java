
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.databind.Module;
import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.emitter.EmitterExtension;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.parser.JaxrsApplicationParser;
import cz.habarta.typescript.generator.parser.RestApplicationParser;
import cz.habarta.typescript.generator.util.Pair;
import cz.habarta.typescript.generator.util.Utils;
import java.io.File;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.function.Predicate;
import java.util.regex.Pattern;


/**
 * See cz.habarta.typescript.generator.maven.GenerateMojo
 * @see <a href="https://github.com/vojtechhabarta/typescript-generator">README.md</a> on GitHub or in project root directory
 * @see <a href="https://github.com/vojtechhabarta/typescript-generator/wiki">Wiki</a> on GitHub
 */
public class Settings {
    public String newline = String.format("%n");
    public String quotes = "\"";
    public String indentString = "    ";
    public TypeScriptFileType outputFileType = TypeScriptFileType.declarationFile;
    public TypeScriptOutputKind outputKind = null;
    public String module = null;
    public String namespace = null;
    public boolean mapPackagesToNamespaces = false;
    public String umdNamespace = null;
    public List<ModuleDependency> moduleDependencies = new ArrayList<>();
    private LoadedModuleDependencies loadedModuleDependencies = null;
    public JsonLibrary jsonLibrary = null;
    public Jackson2ConfigurationResolved jackson2Configuration = null;
    private Predicate<String> excludeFilter = null;
    @Deprecated public boolean declarePropertiesAsOptional = false;
    public OptionalProperties optionalProperties; // default is OptionalProperties.useSpecifiedAnnotations
    public OptionalPropertiesDeclaration optionalPropertiesDeclaration; // default is OptionalPropertiesDeclaration.questionMark
    public boolean declarePropertiesAsReadOnly = false;
    public String removeTypeNamePrefix = null;
    public String removeTypeNameSuffix = null;
    public String addTypeNamePrefix = null;
    public String addTypeNameSuffix = null;
    public Map<String, String> customTypeNaming = new LinkedHashMap<>();
    public String customTypeNamingFunction = null;
    public List<String> referencedFiles = new ArrayList<>();
    public List<String> importDeclarations = new ArrayList<>();
    public Map<String, String> customTypeMappings = new LinkedHashMap<>();
    public DateMapping mapDate; // default is DateMapping.asDate
    public EnumMapping mapEnum; // default is EnumMapping.asUnion
    public boolean nonConstEnums = false;
    public List<Class<? extends Annotation>> nonConstEnumAnnotations = new ArrayList<>();
    public ClassMapping mapClasses; // default is ClassMapping.asInterfaces
    public List<String> mapClassesAsClassesPatterns;
    private Predicate<String> mapClassesAsClassesFilter = null;
    public boolean disableTaggedUnions = false;
    public boolean ignoreSwaggerAnnotations = false;
    public boolean generateJaxrsApplicationInterface = false;
    public boolean generateJaxrsApplicationClient = false;
    public boolean generateSpringApplicationInterface = false;
    public boolean generateSpringApplicationClient = false;
    public boolean scanSpringApplication;
    @Deprecated public RestNamespacing jaxrsNamespacing;
    @Deprecated public Class<? extends Annotation> jaxrsNamespacingAnnotation = null;
    @Deprecated public String jaxrsNamespacingAnnotationElement;  // default is "value"
    public RestNamespacing restNamespacing;
    public Class<? extends Annotation> restNamespacingAnnotation = null;
    public String restNamespacingAnnotationElement;  // default is "value"
    public String restResponseType = null;
    public String restOptionsType = null;
    public boolean restOptionsTypeIsGeneric;
    private List<RestApplicationParser.Factory> restApplicationParserFactories;
    public TypeProcessor customTypeProcessor = null;
    public boolean sortDeclarations = false;
    public boolean sortTypeDeclarations = false;
    public boolean noFileComment = false;
    public boolean noTslintDisable = false;
    public boolean noEslintDisable = false;
    public List<File> javadocXmlFiles = null;
    public List<EmitterExtension> extensions = new ArrayList<>();
    public List<Class<? extends Annotation>> includePropertyAnnotations = new ArrayList<>();
    public List<Class<? extends Annotation>> excludePropertyAnnotations = new ArrayList<>();
    public List<Class<? extends Annotation>> optionalAnnotations = new ArrayList<>();
    public boolean generateInfoJson = false;
    public boolean generateNpmPackageJson = false;
    public String npmName = null;
    public String npmVersion = null;
    public Map<String, String> npmPackageDependencies = new LinkedHashMap<>();
    public String typescriptVersion = "^2.4";
    public String npmBuildScript = null;
    @Deprecated public boolean displaySerializerWarning;
    @Deprecated public boolean debug;
    @Deprecated public boolean disableJackson2ModuleDiscovery = false;
    public boolean jackson2ModuleDiscovery = false;
    public List<Class<? extends Module>> jackson2Modules = new ArrayList<>();
    public ClassLoader classLoader = null;

    private boolean defaultStringEnumsOverriddenByExtension = false;

    public static class ConfiguredExtension {
        public String className;
        public Map<String, String> configuration;
    }

    private static class TypeScriptGeneratorURLClassLoader extends URLClassLoader {

        private final String name;

        public TypeScriptGeneratorURLClassLoader(String name, URL[] urls, ClassLoader parent) {
            super(urls, parent);
            this.name = name;
        }

        @Override
        public String toString() {
            return "TsGenURLClassLoader{" + name + ", parent: " + getParent() + "}";
        }

    }

    public static URLClassLoader createClassLoader(String name, URL[] urls, ClassLoader parent) {
        return new TypeScriptGeneratorURLClassLoader(name, urls, parent);
    }

    public void setStringQuotes(StringQuotes quotes) {
        this.quotes = quotes == StringQuotes.singleQuotes ? "'" : "\"";
    }

    public void setIndentString(String indentString) {
        this.indentString = indentString != null ? indentString : "    ";
    }

    public void setJackson2Configuration(ClassLoader classLoader, Jackson2Configuration configuration) {
        if (configuration != null) {
            jackson2Configuration = Jackson2ConfigurationResolved.from(configuration, classLoader);
        }
    }

    public void loadCustomTypeProcessor(ClassLoader classLoader, String customTypeProcessor) {
        if (customTypeProcessor != null) {
            this.customTypeProcessor = loadInstance(classLoader, customTypeProcessor, TypeProcessor.class);
        }
    }

    public void loadExtensions(ClassLoader classLoader, List<String> extensions, List<Settings.ConfiguredExtension> extensionsWithConfiguration) {
        this.extensions = new ArrayList<>();
        this.extensions.addAll(loadInstances(classLoader, extensions, EmitterExtension.class));
        if (extensionsWithConfiguration != null) {
            for (ConfiguredExtension configuredExtension : extensionsWithConfiguration) {
                final EmitterExtension emitterExtension = loadInstance(classLoader, configuredExtension.className, EmitterExtension.class);
                if (emitterExtension instanceof Extension) {
                    final Extension extension = (Extension) emitterExtension;
                    extension.setConfiguration(Utils.mapFromNullable(configuredExtension.configuration));
                }
                this.extensions.add(emitterExtension);
            }
        }
    }

    public void loadNonConstEnumAnnotations(ClassLoader classLoader, List<String> stringAnnotations) {
        this.nonConstEnumAnnotations = loadClasses(classLoader, stringAnnotations, Annotation.class);
    }

    public void loadIncludePropertyAnnotations(ClassLoader classLoader, List<String> includePropertyAnnotations) {
        this.includePropertyAnnotations = loadClasses(classLoader, includePropertyAnnotations, Annotation.class);
    }

    public void loadExcludePropertyAnnotations(ClassLoader classLoader, List<String> excludePropertyAnnotations) {
        this.excludePropertyAnnotations = loadClasses(classLoader, excludePropertyAnnotations, Annotation.class);
    }

    public void loadOptionalAnnotations(ClassLoader classLoader, List<String> optionalAnnotations) {
        this.optionalAnnotations = loadClasses(classLoader, optionalAnnotations, Annotation.class);
    }

    public void loadJackson2Modules(ClassLoader classLoader, List<String> jackson2Modules) {
        this.jackson2Modules = loadClasses(classLoader, jackson2Modules, Module.class);
    }

    public static Map<String, String> convertToMap(List<String> mappings) {
        final Map<String, String> result = new LinkedHashMap<>();
        if (mappings != null) {
            for (String mapping : mappings) {
                final String[] values = mapping.split(":", 2);
                if (values.length < 2) {
                    throw new RuntimeException("Invalid mapping format: " + mapping);
                }
                result.put(values[0].trim(), values[1].trim());
            }
        }
        return result;
    }

    public void validate() {
        if (outputKind == null) {
            throw new RuntimeException("Required 'outputKind' parameter is not configured. " + seeLink());
        }
        if (outputKind == TypeScriptOutputKind.ambientModule && outputFileType == TypeScriptFileType.implementationFile) {
            throw new RuntimeException("Ambient modules are not supported in implementation files. " + seeLink());
        }
        if (outputKind == TypeScriptOutputKind.ambientModule && module == null) {
            throw new RuntimeException("'module' parameter must be specified for ambient module. " + seeLink());
        }
        if (outputKind != TypeScriptOutputKind.ambientModule && module != null) {
            throw new RuntimeException("'module' parameter is only applicable to ambient modules. " + seeLink());
        }
        if (outputKind != TypeScriptOutputKind.module && umdNamespace != null) {
            throw new RuntimeException("'umdNamespace' parameter is only applicable to modules. " + seeLink());
        }
        if (outputFileType == TypeScriptFileType.implementationFile && umdNamespace != null) {
            throw new RuntimeException("'umdNamespace' parameter is not applicable to implementation files. " + seeLink());
        }
        if (umdNamespace != null && !ModelCompiler.isValidIdentifierName(umdNamespace)) {
            throw new RuntimeException("Value of 'umdNamespace' parameter is not valid identifier: " + umdNamespace + ". " + seeLink());
        }
        if (jsonLibrary == null) {
            throw new RuntimeException("Required 'jsonLibrary' parameter is not configured.");
        }
        if (jackson2Configuration != null && jsonLibrary != JsonLibrary.jackson2) {
            throw new RuntimeException("'jackson2Configuration' parameter is only applicable to 'jackson2' library.");
        }
        for (EmitterExtension extension : extensions) {
            final String extensionName = extension.getClass().getSimpleName();
            final DeprecationText deprecation = extension.getClass().getAnnotation(DeprecationText.class);
            if (deprecation != null) {
                TypeScriptGenerator.getLogger().warning(String.format("Extension '%s' is deprecated: %s", extensionName, deprecation.value()));
            }
            final EmitterExtensionFeatures features = extension.getFeatures();
            if (features.generatesRuntimeCode && outputFileType != TypeScriptFileType.implementationFile) {
                throw new RuntimeException(String.format("Extension '%s' generates runtime code but 'outputFileType' parameter is not set to 'implementationFile'.", extensionName));
            }
            if (features.generatesModuleCode && outputKind != TypeScriptOutputKind.module) {
                throw new RuntimeException(String.format("Extension '%s' generates code as module but 'outputKind' parameter is not set to 'module'.", extensionName));
            }
            if (!features.worksWithPackagesMappedToNamespaces && mapPackagesToNamespaces) {
                throw new RuntimeException(String.format("Extension '%s' doesn't work with 'mapPackagesToNamespaces' parameter.", extensionName));
            }
            if (features.generatesJaxrsApplicationClient) {
                reportConfigurationChange(extensionName, "generateJaxrsApplicationClient", "true");
                generateJaxrsApplicationClient = true;
            }
            if (features.restResponseType != null) {
                reportConfigurationChange(extensionName, "restResponseType", features.restResponseType);
                restResponseType = features.restResponseType;
            }
            if (features.restOptionsType != null) {
                reportConfigurationChange(extensionName, "restOptionsType", features.restOptionsType);
                setRestOptionsType(features.restOptionsType);
            }
            if (features.npmPackageDependencies != null) {
                npmPackageDependencies.putAll(features.npmPackageDependencies);
            }
            if (features.overridesStringEnums) {
                defaultStringEnumsOverriddenByExtension = true;
            }
        }
        if ((nonConstEnums || !nonConstEnumAnnotations.isEmpty()) && outputFileType != TypeScriptFileType.implementationFile) {
            throw new RuntimeException("Non-const enums can only be used in implementation files but 'outputFileType' parameter is not set to 'implementationFile'.");
        }
        if (mapClasses == ClassMapping.asClasses && outputFileType != TypeScriptFileType.implementationFile) {
            throw new RuntimeException("'mapClasses' parameter is set to 'asClasses' which generates runtime code but 'outputFileType' parameter is not set to 'implementationFile'.");
        }
        if (mapClassesAsClassesPatterns != null && mapClasses != ClassMapping.asClasses) {
            throw new RuntimeException("'mapClassesAsClassesPatterns' parameter can only be used when 'mapClasses' parameter is set to 'asClasses'.");
        }
        if (generateJaxrsApplicationClient && outputFileType != TypeScriptFileType.implementationFile) {
            throw new RuntimeException("'generateJaxrsApplicationClient' can only be used when generating implementation file ('outputFileType' parameter is 'implementationFile').");
        }
        if (generateSpringApplicationClient && outputFileType != TypeScriptFileType.implementationFile) {
            throw new RuntimeException("'generateSpringApplicationClient' can only be used when generating implementation file ('outputFileType' parameter is 'implementationFile').");
        }
        if (jaxrsNamespacing != null) {
            TypeScriptGenerator.getLogger().warning("Parameter 'jaxrsNamespacing' is deprecated. Use 'restNamespacing' parameter.");
            if (restNamespacing == null) {
                restNamespacing = jaxrsNamespacing;
            }
        }
        if (jaxrsNamespacingAnnotation != null) {
            TypeScriptGenerator.getLogger().warning("Parameter 'jaxrsNamespacingAnnotation' is deprecated. Use 'restNamespacingAnnotation' parameter.");
            if (restNamespacingAnnotation == null) {
                restNamespacingAnnotation = jaxrsNamespacingAnnotation;
            }
        }
        if (restNamespacing != null && !isGenerateRest()) {
            throw new RuntimeException("'restNamespacing' parameter can only be used when generating REST client or interface.");
        }
        if (restNamespacingAnnotation != null && restNamespacing != RestNamespacing.byAnnotation) {
            throw new RuntimeException("'restNamespacingAnnotation' parameter can only be used when 'restNamespacing' parameter is set to 'byAnnotation'.");
        }
        if (restNamespacingAnnotation == null && restNamespacing == RestNamespacing.byAnnotation) {
            throw new RuntimeException("'restNamespacingAnnotation' must be specified when 'restNamespacing' parameter is set to 'byAnnotation'.");
        }
        if (restResponseType != null && !isGenerateRest()) {
            throw new RuntimeException("'restResponseType' parameter can only be used when generating REST client or interface.");
        }
        if (restOptionsType != null && !isGenerateRest()) {
            throw new RuntimeException("'restOptionsType' parameter can only be used when generating REST client or interface.");
        }
        if (generateInfoJson && outputKind != TypeScriptOutputKind.module) {
            throw new RuntimeException("'generateInfoJson' can only be used when generating proper module ('outputKind' parameter is 'module').");
        }
        if (generateNpmPackageJson && outputKind != TypeScriptOutputKind.module) {
            throw new RuntimeException("'generateNpmPackageJson' can only be used when generating proper module ('outputKind' parameter is 'module').");
        }
        if (generateNpmPackageJson) {
            if (npmName == null || npmVersion == null) {
                throw new RuntimeException("'npmName' and 'npmVersion' must be specified when generating NPM 'package.json'.");
            }
        }
        if (!generateNpmPackageJson) {
            if (npmName != null || npmVersion != null) {
                throw new RuntimeException("'npmName' and 'npmVersion' is only applicable when generating NPM 'package.json'.");
            }
            if (npmBuildScript != null) {
                throw new RuntimeException("'npmBuildScript' is only applicable when generating NPM 'package.json'.");
            }
        }
        if (npmBuildScript != null && outputFileType != TypeScriptFileType.implementationFile) {
            throw new RuntimeException("'npmBuildScript' can only be used when generating implementation file ('outputFileType' parameter is 'implementationFile').");
        }
        getModuleDependencies();

        if (declarePropertiesAsOptional) {
            TypeScriptGenerator.getLogger().warning("Parameter 'declarePropertiesAsOptional' is deprecated. Use 'optionalProperties' parameter.");
            if (optionalProperties == null) {
                optionalProperties = OptionalProperties.all;
            }
        }
        if (disableJackson2ModuleDiscovery) {
            TypeScriptGenerator.getLogger().warning("Parameter 'disableJackson2ModuleDiscovery' was removed. See 'jackson2ModuleDiscovery' and 'jackson2Modules' parameters.");
        }
        if (displaySerializerWarning) {
            TypeScriptGenerator.getLogger().warning("Parameter 'displaySerializerWarning' was removed. Please use 'loggingLevel' parameter, these messages have 'Verbose' level.");
        }
        if (debug) {
            TypeScriptGenerator.getLogger().warning("Parameter 'debug' was removed. Please set 'loggingLevel' parameter to 'Debug'.");
        }
    }

    private static void reportConfigurationChange(String extensionName, String parameterName, String parameterValue) {
        TypeScriptGenerator.getLogger().info(String.format("Configuration: '%s' extension set '%s' parameter to '%s'", extensionName, parameterName, parameterValue));
    }

    public String getExtension() {
        return outputFileType == TypeScriptFileType.implementationFile ? ".ts" : ".d.ts";
    }

    public void validateFileName(File outputFile) {
        if (outputFileType == TypeScriptFileType.declarationFile && !outputFile.getName().endsWith(".d.ts")) {
            throw new RuntimeException("Declaration file must have 'd.ts' extension: " + outputFile);
        }
        if (outputFileType == TypeScriptFileType.implementationFile && (!outputFile.getName().endsWith(".ts") || outputFile.getName().endsWith(".d.ts"))) {
            throw new RuntimeException("Implementation file must have 'ts' extension: " + outputFile);
        }
    }

    public String getDefaultNpmVersion() {
        return "1.0.0";
    }

    public LoadedModuleDependencies getModuleDependencies() {
        if (loadedModuleDependencies == null) {
            loadedModuleDependencies = new LoadedModuleDependencies(this, moduleDependencies);
        }
        return loadedModuleDependencies;
    }

    public Predicate<String> getExcludeFilter() {
        if (excludeFilter == null) {
            setExcludeFilter(null, null);
        }
        return excludeFilter;
    }

    public void setExcludeFilter(List<String> excludedClasses, List<String> excludedClassPatterns) {
        this.excludeFilter = createExcludeFilter(excludedClasses, excludedClassPatterns);
    }

    public static Predicate<String> createExcludeFilter(List<String> excludedClasses, List<String> excludedClassPatterns) {
        final Set<String> names = new LinkedHashSet<>(excludedClasses != null ? excludedClasses : Collections.<String>emptyList());
        final List<Pattern> patterns = Utils.globsToRegexps(excludedClassPatterns != null ? excludedClassPatterns : Collections.<String>emptyList());
        return new Predicate<String>() {
            @Override
            public boolean test(String className) {
                return names.contains(className) || Utils.classNameMatches(className, patterns);
            }
        };
    }

    public Predicate<String> getMapClassesAsClassesFilter() {
        if (mapClassesAsClassesFilter == null) {
            final List<Pattern> patterns = Utils.globsToRegexps(mapClassesAsClassesPatterns);
            mapClassesAsClassesFilter = new Predicate<String>() {
                @Override
                public boolean test(String className) {
                    return mapClasses == ClassMapping.asClasses &&
                            (patterns == null || Utils.classNameMatches(className, patterns));
                }
            };
        }
        return mapClassesAsClassesFilter;
    }

    @Deprecated
    public void setJaxrsNamespacingAnnotation(ClassLoader classLoader, String jaxrsNamespacingAnnotation) {
        final Pair<Class<? extends Annotation>, String> pair = resolveRestNamespacingAnnotation(classLoader, jaxrsNamespacingAnnotation);
        if (pair != null) {
            this.jaxrsNamespacingAnnotation = pair.getValue1();
            this.jaxrsNamespacingAnnotationElement = pair.getValue2();
        }
    }

    public void setRestNamespacingAnnotation(ClassLoader classLoader, String restNamespacingAnnotation) {
        final Pair<Class<? extends Annotation>, String> pair = resolveRestNamespacingAnnotation(classLoader, restNamespacingAnnotation);
        if (pair != null) {
            this.restNamespacingAnnotation = pair.getValue1();
            this.restNamespacingAnnotationElement = pair.getValue2();
        }
    }

    private static Pair<Class<? extends Annotation>, String> resolveRestNamespacingAnnotation(ClassLoader classLoader, String restNamespacingAnnotation) {
        if (restNamespacingAnnotation == null) {
            return null;
        }
        final String[] split = restNamespacingAnnotation.split("#");
        final String className = split[0];
        final String elementName = split.length > 1 ? split[1] : "value";
        final Class<? extends Annotation> annotationClass = loadClass(classLoader, className, Annotation.class);
        return Pair.of(annotationClass, elementName);
    }

    public void setRestOptionsType(String restOptionsType) {
        if (restOptionsType != null) {
            if (restOptionsType.startsWith("<") && restOptionsType.endsWith(">")) {
                this.restOptionsType = restOptionsType.substring(1, restOptionsType.length() - 1);
                this.restOptionsTypeIsGeneric = true;
            } else {
                this.restOptionsType = restOptionsType;
                this.restOptionsTypeIsGeneric = false;
            }
        }
    }

    public List<RestApplicationParser.Factory> getRestApplicationParserFactories() {
        if (restApplicationParserFactories == null) {
            final List<RestApplicationParser.Factory> factories = new ArrayList<>();
            if (isGenerateJaxrs() || !isGenerateSpring()) {
                factories.add(new JaxrsApplicationParser.Factory());
            }
            if (isGenerateSpring()) {
                final String springClassName = "cz.habarta.typescript.generator.spring.SpringApplicationParser$Factory";
                final Class<?> springClass;
                try {
                    springClass = Class.forName(springClassName);
                } catch (ClassNotFoundException e) {
                    throw new RuntimeException("'generateStringApplicationInterface' or 'generateStringApplicationClient' parameter "
                            + "was specified but '" + springClassName + "' was not found. "
                            + "Please add 'cz.habarta.typescript-generator:typescript-generator-spring' artifact "
                            + "to typescript-generator plugin dependencies (not module dependencies).");
                }
                try {
                    final Object instance = springClass.getConstructor().newInstance();
                    factories.add((RestApplicationParser.Factory) instance);
                } catch (ReflectiveOperationException e) {
                    throw new RuntimeException(e);
                }
            }
            restApplicationParserFactories = factories;
        }
        return restApplicationParserFactories;
    }

    public boolean isGenerateJaxrs() {
        return generateJaxrsApplicationInterface || generateJaxrsApplicationClient;
    }

    public boolean isGenerateSpring() {
        return generateSpringApplicationInterface || generateSpringApplicationClient;
    }

    public boolean isGenerateRest() {
        return isGenerateJaxrs() || isGenerateSpring();
    }

    public boolean areDefaultStringEnumsOverriddenByExtension() {
        return defaultStringEnumsOverriddenByExtension;
    }

    private String seeLink() {
        return "For more information see 'http://vojtechhabarta.github.io/typescript-generator/doc/ModulesAndNamespaces.html'.";
    }

    private static <T> List<Class<? extends T>> loadClasses(ClassLoader classLoader, List<String> classNames, Class<T> requiredClassType) {
        if (classNames == null) {
            return Collections.emptyList();
        }
        final List<Class<? extends T>> classes = new ArrayList<>();
        for (String className : classNames) {
            classes.add(loadClass(classLoader, className, requiredClassType));
        }
        return classes;
    }

    static <T> Class<? extends T> loadClass(ClassLoader classLoader, String className, Class<T> requiredClassType) {
        Objects.requireNonNull(classLoader, "classLoader");
        Objects.requireNonNull(className, "className");
        Objects.requireNonNull(requiredClassType, "requiredClassType");
        try {
            TypeScriptGenerator.getLogger().verbose("Loading class " + className);
            final Class<?> loadedClass = classLoader.loadClass(className);
            if (requiredClassType.isAssignableFrom(loadedClass)) {
                @SuppressWarnings("unchecked")
                final Class<? extends T> castedClass = (Class<? extends T>) loadedClass;
                return castedClass;
            } else {
                throw new RuntimeException(String.format("Class '%s' is not assignable to '%s'.", loadedClass, requiredClassType));
            }
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static <T> List<T> loadInstances(ClassLoader classLoader, List<String> classNames, Class<T> requiredType) {
        if (classNames == null) {
            return Collections.emptyList();
        }
        final List<T> instances = new ArrayList<>();
        for (String className : classNames) {
            instances.add(loadInstance(classLoader, className, requiredType));
        }
        return instances;
    }

    private static <T> T loadInstance(ClassLoader classLoader, String className, Class<T> requiredType) {
        try {
            TypeScriptGenerator.getLogger().verbose("Loading class " + className);
            return requiredType.cast(classLoader.loadClass(className).newInstance());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

}
