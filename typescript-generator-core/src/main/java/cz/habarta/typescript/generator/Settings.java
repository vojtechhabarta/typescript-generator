
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.compiler.SymbolTable.CustomTypeNamingFunction;
import cz.habarta.typescript.generator.emitter.EmitterExtension;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.parser.JaxrsApplicationParser;
import cz.habarta.typescript.generator.parser.RestApplicationParser;
import cz.habarta.typescript.generator.parser.TypeParser;
import cz.habarta.typescript.generator.util.Pair;
import cz.habarta.typescript.generator.util.Utils;
import java.io.File;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.TypeVariable;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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
    public GsonConfiguration gsonConfiguration = null;
    public JsonbConfiguration jsonbConfiguration = null;
    public List<String> additionalDataLibraries = new ArrayList<>();
    private LoadedDataLibraries loadedDataLibrariesClasses = null;
    private Predicate<String> excludeFilter = null;
    public OptionalProperties optionalProperties; // default is OptionalProperties.useSpecifiedAnnotations
    public OptionalPropertiesDeclaration optionalPropertiesDeclaration; // default is OptionalPropertiesDeclaration.questionMark
    public NullabilityDefinition nullabilityDefinition; // default is NullabilityDefinition.nullInlineUnion
    private TypeParser typeParser = null;
    public boolean declarePropertiesAsReadOnly = false;
    public String removeTypeNamePrefix = null;
    public String removeTypeNameSuffix = null;
    public String addTypeNamePrefix = null;
    public String addTypeNameSuffix = null;
    public Map<String, String> customTypeNaming = new LinkedHashMap<>();
    public String customTypeNamingFunction = null;
    public CustomTypeNamingFunction customTypeNamingFunctionImpl = null;
    public List<String> referencedFiles = new ArrayList<>();
    public List<String> importDeclarations = new ArrayList<>();
    public Map<String, String> customTypeMappings = new LinkedHashMap<>();
    private List<CustomTypeMapping> validatedCustomTypeMappings = null;
    public Map<String, String> customTypeAliases = new LinkedHashMap<>();
    private List<CustomTypeAlias> validatedCustomTypeAliases = null;
    public DateMapping mapDate; // default is DateMapping.asDate
    public MapMapping mapMap; // default is MapMapping.asIndexedArray
    public EnumMapping mapEnum; // default is EnumMapping.asUnion
    public IdentifierCasing enumMemberCasing; // default is IdentifierCasing.keepOriginal
    public boolean nonConstEnums = false;
    public List<Class<? extends Annotation>> nonConstEnumAnnotations = new ArrayList<>();
    public ClassMapping mapClasses; // default is ClassMapping.asInterfaces
    public List<String> mapClassesAsClassesPatterns;
    private Predicate<String> mapClassesAsClassesFilter = null;
    public boolean generateConstructors = false;
    public List<Class<? extends Annotation>> disableTaggedUnionAnnotations = new ArrayList<>();
    public boolean disableTaggedUnions = false;
    public boolean generateReadonlyAndWriteonlyJSDocTags = false;
    public boolean ignoreSwaggerAnnotations = false;
    public boolean generateJaxrsApplicationInterface = false;
    public boolean generateJaxrsApplicationClient = false;
    public boolean generateSpringApplicationInterface = false;
    public boolean generateSpringApplicationClient = false;
    public boolean scanSpringApplication;
    public RestNamespacing restNamespacing;
    public Class<? extends Annotation> restNamespacingAnnotation = null;
    public String restNamespacingAnnotationElement;  // default is "value"
    public String restResponseType = null;
    public String restOptionsType = null;
    public boolean restOptionsTypeIsGeneric;
    private List<RestApplicationParser.Factory> restApplicationParserFactories;
    public TypeProcessor customTypeProcessor = null;
    public RestMethodBuilder customRestMethodBuilder = null;
    public boolean sortDeclarations = false;
    public boolean sortTypeDeclarations = false;
    public boolean noFileComment = false;
    public boolean noTslintDisable = false;
    public boolean noEslintDisable = false;
    public boolean tsNoCheck = false;
    public List<File> javadocXmlFiles = null;
    public List<EmitterExtension> extensions = new ArrayList<>();
    public List<Class<? extends Annotation>> includePropertyAnnotations = new ArrayList<>();
    public List<Class<? extends Annotation>> excludePropertyAnnotations = new ArrayList<>();
    public List<Class<? extends Annotation>> optionalAnnotations = new ArrayList<>();
    public List<Class<? extends Annotation>> requiredAnnotations = new ArrayList<>();
    public List<Class<? extends Annotation>> nullableAnnotations = new ArrayList<>();
    public boolean primitivePropertiesRequired = false;
    public boolean generateInfoJson = false;
    public boolean generateNpmPackageJson = false;
    public String npmName = null;
    public String npmVersion = null;
    public Map<String, String> npmPackageDependencies = new LinkedHashMap<>();
    public Map<String, String> npmDevDependencies = new LinkedHashMap<>();
    public Map<String, String> npmPeerDependencies = new LinkedHashMap<>();
    public String typescriptVersion = "^2.4";
    public String npmTypescriptVersion = null;
    public String npmBuildScript = null;
    public boolean jackson2ModuleDiscovery = false;
    public List<Class<? extends Module>> jackson2Modules = new ArrayList<>();
    public ClassLoader classLoader = null;

    private boolean defaultStringEnumsOverriddenByExtension = false;

    public static class ConfiguredExtension {
        public String className;
        public Map<String, String> configuration;
    }

    public static class CustomTypeMapping {
        public final Class<?> rawClass;
        public final boolean matchSubclasses;
        public final GenericName javaType;
        public final GenericName tsType;

        public CustomTypeMapping(Class<?> rawClass, boolean matchSubclasses, GenericName javaType, GenericName tsType) {
            this.rawClass = rawClass;
            this.matchSubclasses = matchSubclasses;
            this.javaType = javaType;
            this.tsType = tsType;
        }

        @Override
        public String toString() {
            return Utils.objectToString(this);
        }
    }

    public static class CustomTypeAlias {
        public final GenericName tsType;
        public final String tsDefinition;

        public CustomTypeAlias(GenericName tsType, String tsDefinition) {
            this.tsType = tsType;
            this.tsDefinition = tsDefinition;
        }
    }

    public static class GenericName {
        public final String rawName;
        public final List<String> typeParameters;

        public GenericName(String rawName, List<String> typeParameters) {
            this.rawName = Objects.requireNonNull(rawName);
            this.typeParameters = typeParameters;
        }

        public int indexOfTypeParameter(String typeParameter) {
            return typeParameters != null ? typeParameters.indexOf(typeParameter) : -1;
        }
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

    public void loadCustomRestMethodBuilder(ClassLoader classLoader, String customRestMethodBuilder) {
        if (customRestMethodBuilder != null) {
            this.customRestMethodBuilder = loadInstance(classLoader, customRestMethodBuilder, RestMethodBuilder.class);
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

    public void loadRequiredAnnotations(ClassLoader classLoader, List<String> requiredAnnotations) {
        this.requiredAnnotations = loadClasses(classLoader, requiredAnnotations, Annotation.class);
    }

    public void loadNullableAnnotations(ClassLoader classLoader, List<String> nullableAnnotations) {
        this.nullableAnnotations = loadClasses(classLoader, nullableAnnotations, Annotation.class);
    }

    public void loadDisableTaggedUnionAnnotations(ClassLoader classLoader, List<String> disableTaggedUnionAnnotations) {
        this.disableTaggedUnionAnnotations = loadClasses(classLoader, disableTaggedUnionAnnotations, Annotation.class);
    }

    public void loadJackson2Modules(ClassLoader classLoader, List<String> jackson2Modules) {
        this.jackson2Modules = loadClasses(classLoader, jackson2Modules, Module.class);
    }

    public static Map<String, String> convertToMap(List<String> items, String itemName) {
        final Map<String, String> result = new LinkedHashMap<>();
        if (items != null) {
            for (String item : items) {
                final String[] values = item.split(":", 2);
                if (values.length < 2) {
                    throw new RuntimeException(String.format("Invalid '%s' format: %s", itemName, item));
                }
                result.put(values[0].trim(), values[1].trim());
            }
        }
        return result;
    }
    
    public void validate() {
        if (classLoader == null) {
            classLoader = Thread.currentThread().getContextClassLoader();
        }
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
        if (!generateNpmPackageJson && (!npmPackageDependencies.isEmpty() || !npmDevDependencies.isEmpty() || !npmPeerDependencies.isEmpty())) {
            throw new RuntimeException("'npmDependencies', 'npmDevDependencies' and 'npmPeerDependencies' parameters are only applicable when generating NPM 'package.json'.");
        }
        getValidatedCustomTypeMappings();
        getValidatedCustomTypeAliases();
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
            if (features.npmDevDependencies != null) {
                npmDevDependencies.putAll(features.npmDevDependencies);
            }
            if (features.npmPeerDependencies != null) {
                npmPeerDependencies.putAll(features.npmPeerDependencies);
            }
            if (features.overridesStringEnums) {
                defaultStringEnumsOverriddenByExtension = true;
            }
        }
        if (enumMemberCasing != null && mapEnum != EnumMapping.asEnum && mapEnum != EnumMapping.asNumberBasedEnum) {
            throw new RuntimeException("'enumMemberCasing' parameter can only be used when 'mapEnum' parameter is set to 'asEnum' or 'asNumberBasedEnum'.");
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
        if (generateConstructors && mapClasses != ClassMapping.asClasses) {
            throw new RuntimeException("'generateConstructors' parameter can only be used when 'mapClasses' parameter is set to 'asClasses'.");
        }
        checkAnnotationsHaveRuntimeRetention(this.nonConstEnumAnnotations);
        checkAnnotationsHaveRuntimeRetention(this.disableTaggedUnionAnnotations);
        checkAnnotationHasRuntimeRetention(this.restNamespacingAnnotation);
        checkAnnotationsHaveRuntimeRetention(this.includePropertyAnnotations);
        checkAnnotationsHaveRuntimeRetention(this.excludePropertyAnnotations);
        checkAnnotationsHaveRuntimeRetention(this.optionalAnnotations);
        checkAnnotationsHaveRuntimeRetention(this.requiredAnnotations);
        checkAnnotationsHaveRuntimeRetention(this.nullableAnnotations);
        for (Class<? extends Annotation> annotation : optionalAnnotations) {
            final Target target = annotation.getAnnotation(Target.class);
            final List<ElementType> elementTypes = target != null ? Arrays.asList(target.value()) : Arrays.asList();
            if (elementTypes.contains(ElementType.TYPE_PARAMETER) || elementTypes.contains(ElementType.TYPE_USE)) {
                TypeScriptGenerator.getLogger().info(String.format(
                        "Suggestion: annotation '%s' supports 'TYPE_PARAMETER' or 'TYPE_USE' target. Consider using 'nullableAnnotations' parameter instead of 'optionalAnnotations'.",
                        annotation.getName()));
            }
        }
        if (!optionalAnnotations.isEmpty() && !requiredAnnotations.isEmpty()) {
            throw new RuntimeException("Only one of 'optionalAnnotations' and 'requiredAnnotations' can be used at the same time.");
        }
        if (primitivePropertiesRequired && requiredAnnotations.isEmpty()) {
            throw new RuntimeException("'primitivePropertiesRequired' parameter can only be used with 'requiredAnnotations' parameter.");
        }
        for (Class<? extends Annotation> annotation : nullableAnnotations) {
            final Target target = annotation.getAnnotation(Target.class);
            final List<ElementType> elementTypes = target != null ? Arrays.asList(target.value()) : Arrays.asList();
            if (!elementTypes.contains(ElementType.TYPE_PARAMETER) && !elementTypes.contains(ElementType.TYPE_USE)) {
                throw new RuntimeException(String.format(
                        "'%s' annotation cannot be used as nullable annotation because it doesn't have 'TYPE_PARAMETER' or 'TYPE_USE' target.",
                        annotation.getName()));
            }
        }
        if (generateJaxrsApplicationClient && outputFileType != TypeScriptFileType.implementationFile) {
            throw new RuntimeException("'generateJaxrsApplicationClient' can only be used when generating implementation file ('outputFileType' parameter is 'implementationFile').");
        }
        if (generateSpringApplicationClient && outputFileType != TypeScriptFileType.implementationFile) {
            throw new RuntimeException("'generateSpringApplicationClient' can only be used when generating implementation file ('outputFileType' parameter is 'implementationFile').");
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
        if (generateInfoJson && (outputKind != TypeScriptOutputKind.module && outputKind != TypeScriptOutputKind.global)) {
            throw new RuntimeException("'generateInfoJson' can only be used when 'outputKind' parameter is 'module' or 'global'.");
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
            if (npmTypescriptVersion != null) {
                throw new RuntimeException("'npmTypescriptVersion' is only applicable when generating NPM 'package.json'.");
            }
            if (npmBuildScript != null) {
                throw new RuntimeException("'npmBuildScript' is only applicable when generating NPM 'package.json'.");
            }
        }
        if (npmTypescriptVersion != null && outputFileType != TypeScriptFileType.implementationFile) {
            throw new RuntimeException("'npmTypescriptVersion' can only be used when generating implementation file ('outputFileType' parameter is 'implementationFile').");
        }
        if (npmBuildScript != null && outputFileType != TypeScriptFileType.implementationFile) {
            throw new RuntimeException("'npmBuildScript' can only be used when generating implementation file ('outputFileType' parameter is 'implementationFile').");
        }
        getModuleDependencies();
        getLoadedDataLibraries();
    }

    public NullabilityDefinition getNullabilityDefinition() {
        return nullabilityDefinition != null ? nullabilityDefinition : NullabilityDefinition.nullInlineUnion;
    }

    public TypeParser getTypeParser() {
        if (typeParser == null) {
            typeParser = new TypeParser(nullableAnnotations);
        }
        return typeParser;
    }

    public List<CustomTypeMapping> getValidatedCustomTypeMappings() {
        if (validatedCustomTypeMappings == null) {
            validatedCustomTypeMappings = Utils.concat(
                    validateCustomTypeMappings(customTypeMappings, false),
                    getLoadedDataLibraries().typeMappings);
        }
        return validatedCustomTypeMappings;
    }

    private List<CustomTypeMapping> validateCustomTypeMappings(Map<String, String> customTypeMappings, boolean matchSubclasses) {
        final List<CustomTypeMapping> mappings = new ArrayList<>();
        for (Map.Entry<String, String> entry : customTypeMappings.entrySet()) {
            final String javaName = entry.getKey();
            final String tsName = entry.getValue();
            try {
                final GenericName genericJavaName = parseGenericName(javaName);
                final GenericName genericTsName = parseGenericName(tsName);
                validateTypeParameters(genericJavaName.typeParameters);
                validateTypeParameters(genericTsName.typeParameters);
                final Class<?> cls = loadClass(classLoader, genericJavaName.rawName, null);
                final int required = cls.getTypeParameters().length;
                final int specified = genericJavaName.typeParameters != null ? genericJavaName.typeParameters.size() : 0;
                if (specified != required) {
                    final String parameters = Stream.of(cls.getTypeParameters())
                            .map(TypeVariable::getName)
                            .collect(Collectors.joining(", "));
                    final String signature = cls.getName() + (parameters.isEmpty() ? "" : "<" + parameters + ">");
                    throw new RuntimeException(String.format(
                            "Wrong number of specified generic parameters, required: %s, found: %s. Correct format is: '%s'",
                            required, specified, signature));
                }
                mappings.add(new CustomTypeMapping(cls, matchSubclasses, genericJavaName, genericTsName));
            } catch (Exception e) {
                throw new RuntimeException(String.format("Failed to parse configured custom type mapping '%s:%s': %s", javaName, tsName, e.getMessage()), e);
            }
        }
        return mappings;
    }

    public List<CustomTypeAlias> getValidatedCustomTypeAliases() {
        if (validatedCustomTypeAliases == null) {
            validatedCustomTypeAliases = Utils.concat(
                    validateCustomTypeAliases(customTypeAliases),
                    getLoadedDataLibraries().typeAliases);
        }
        return validatedCustomTypeAliases;
    }

    public List<CustomTypeAlias> validateCustomTypeAliases(Map<String, String> customTypeAliases) {
        final List<CustomTypeAlias> aliases = new ArrayList<>();
        for (Map.Entry<String, String> entry : customTypeAliases.entrySet()) {
            final String tsName = entry.getKey();
            final String tsDefinition = entry.getValue();
            try {
                final GenericName genericTsName = parseGenericName(tsName);
                if (!ModelCompiler.isValidIdentifierName(genericTsName.rawName)) {
                    throw new RuntimeException(String.format("Invalid identifier: '%s'", genericTsName.rawName));
                }
                validateTypeParameters(genericTsName.typeParameters);
                aliases.add(new CustomTypeAlias(genericTsName, tsDefinition));
            } catch (Exception e) {
                throw new RuntimeException(String.format("Failed to parse configured custom type alias '%s:%s': %s", tsName, tsDefinition, e.getMessage()), e);
            }
        }
        return aliases;
    }

    private static GenericName parseGenericName(String name) {
        // Class<T1, T2>
        // Class[T1, T2]
        final Matcher matcher = Pattern.compile("([^<\\[]+)(<|\\[)([^>\\]]+)(>|\\])").matcher(name);
        final String rawName;
        final List<String> typeParameters;
        if (matcher.matches()) {  // is generic?
            rawName = matcher.group(1);
            typeParameters = Stream.of(matcher.group(3).split(","))
                    .map(String::trim)
                    .collect(Collectors.toList());
        } else {
            rawName = name;
            typeParameters = null;
        }
        return new GenericName(rawName, typeParameters);
    }

    private static void validateTypeParameters(List<String> typeParameters) {
        if (typeParameters == null) {
            return;
        }
        for (String typeParameter : typeParameters) {
            if (!ModelCompiler.isValidIdentifierName(typeParameter)) {
                throw new RuntimeException(String.format("Invalid generic type parameter: '%s'", typeParameter));
            }
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

    public LoadedDataLibraries getLoadedDataLibraries() {
        if (loadedDataLibrariesClasses == null) {
            loadedDataLibrariesClasses = loadDataLibrariesClasses();
        }
        return loadedDataLibrariesClasses;
    }

    private LoadedDataLibraries loadDataLibrariesClasses() {
        if (additionalDataLibraries == null) {
            return new LoadedDataLibraries();
        }
        final List<LoadedDataLibraries> loaded = new ArrayList<>();
        final ObjectMapper objectMapper = Utils.getObjectMapper();
        objectMapper.configure(JsonParser.Feature.ALLOW_COMMENTS, true);
        for (String library : additionalDataLibraries) {
            final String resource = "datalibrary/" + library + ".json";
            TypeScriptGenerator.getLogger().verbose("Loading resource " + resource);
            final InputStream inputStream = classLoader.getResourceAsStream(resource);
            if (inputStream == null) {
                throw new RuntimeException("Resource not found: " + resource);
            }
            final DataLibraryJson dataLibrary = Utils.loadJson(objectMapper, inputStream, DataLibraryJson.class);
            final Map<String, String> typeMappings = Utils.listFromNullable(dataLibrary.classMappings).stream()
                    .filter(mapping -> mapping.customType != null)
                    .collect(Utils.toMap(
                            mapping -> mapping.className,
                            mapping -> mapping.customType
                    ));
            final Map<String, String> typeAliases = Utils.listFromNullable(dataLibrary.typeAliases).stream()
                    .collect(Utils.toMap(
                            alias -> alias.name,
                            alias -> alias.definition
                    ));
            loaded.add(new LoadedDataLibraries(
                loadDataLibraryClasses(dataLibrary, DataLibraryJson.SemanticType.String),
                loadDataLibraryClasses(dataLibrary, DataLibraryJson.SemanticType.Number),
                loadDataLibraryClasses(dataLibrary, DataLibraryJson.SemanticType.Boolean),
                loadDataLibraryClasses(dataLibrary, DataLibraryJson.SemanticType.Date),
                loadDataLibraryClasses(dataLibrary, DataLibraryJson.SemanticType.Any),
                loadDataLibraryClasses(dataLibrary, DataLibraryJson.SemanticType.Void),
                loadDataLibraryClasses(dataLibrary, DataLibraryJson.SemanticType.List),
                loadDataLibraryClasses(dataLibrary, DataLibraryJson.SemanticType.Map),
                loadDataLibraryClasses(dataLibrary, DataLibraryJson.SemanticType.Optional),
                loadDataLibraryClasses(dataLibrary, DataLibraryJson.SemanticType.Wrapper),
                validateCustomTypeMappings(typeMappings, true),
                validateCustomTypeAliases(typeAliases)
            ));
        }
        return LoadedDataLibraries.join(loaded);
    }

    private List<Class<?>> loadDataLibraryClasses(DataLibraryJson dataLibrary, DataLibraryJson.SemanticType semanticType) {
        final List<String> classNames = dataLibrary.classMappings.stream()
                .filter(mapping -> mapping.semanticType == semanticType)
                .map(mapping -> mapping.className)
                .collect(Collectors.toList());
        return loadClasses(classLoader, classNames, null);
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
        names.add("java.lang.Record");
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
        try {
            TypeScriptGenerator.getLogger().verbose("Loading class " + className);
            final Pair<String, Integer> pair = parseArrayClassDimensions(className);
            final int arrayDimensions = pair.getValue2();
            final Class<?> loadedClass;
            if (arrayDimensions > 0) {
                final String componentTypeName = pair.getValue1();
                final Class<?> componentType = loadPrimitiveOrRegularClass(classLoader, componentTypeName);
                loadedClass = Utils.getArrayClass(componentType, arrayDimensions);
            } else {
                loadedClass = loadPrimitiveOrRegularClass(classLoader, className);
            }
            if (requiredClassType != null && !requiredClassType.isAssignableFrom(loadedClass)) {
                throw new RuntimeException(String.format("Class '%s' is not assignable to '%s'.", loadedClass, requiredClassType));
            }
            @SuppressWarnings("unchecked") 
            final Class<? extends T> castedClass = (Class<? extends T>) loadedClass;
            return castedClass;
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    private static void checkAnnotationsHaveRuntimeRetention(List<Class<? extends Annotation>> annotationClasses) {
        annotationClasses.forEach(Settings::checkAnnotationHasRuntimeRetention);
    }

    private static void checkAnnotationHasRuntimeRetention(Class<? extends Annotation> annotationClass) {
        if (annotationClass == null) {
            return;
        }
        final Retention retention = annotationClass.getAnnotation(Retention.class);
        if (retention == null || retention.value() != RetentionPolicy.RUNTIME) {
            TypeScriptGenerator.getLogger().warning(String.format(
                "Annotation '%s' has no effect because it doesn't have 'RUNTIME' retention.",
                annotationClass.getName()));
        }
    }

    private static Pair<String, Integer> parseArrayClassDimensions(String className) {
        int dimensions = 0;
        while (className.endsWith("[]")) {
            dimensions++;
            className = className.substring(0, className.length() - 2);
        }
        return Pair.of(className, dimensions);
    }

    private static Class<?> loadPrimitiveOrRegularClass(ClassLoader classLoader, String className) throws ClassNotFoundException {
        final Class<?> primitiveType = Utils.getPrimitiveType(className);
        return primitiveType != null
                ? primitiveType
                : classLoader.loadClass(className);
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
            return requiredType.cast(classLoader.loadClass(className).getConstructor().newInstance());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static int parseModifiers(String modifiers, int allowedModifiers) {
        return Stream.of(modifiers.split("\\|"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(s -> {
                try {
                    return javax.lang.model.element.Modifier.valueOf(s.toUpperCase(Locale.US));
                } catch (IllegalArgumentException e) {
                    throw new RuntimeException("Invalid modifier: " + s);
                }
            })
            .mapToInt(modifier -> {
                final int mod = Settings.modifierToBitMask(modifier);
                if ((mod & allowedModifiers) == 0) {
                    throw new RuntimeException("Modifier not allowed: " + modifier);
                }
                return mod;
            })
            .reduce(0, (a, b) -> a | b);
    }

    private static int modifierToBitMask(javax.lang.model.element.Modifier modifier) {
        switch (modifier) {
            case PUBLIC: return java.lang.reflect.Modifier.PUBLIC;
            case PROTECTED: return java.lang.reflect.Modifier.PROTECTED;
            case PRIVATE: return java.lang.reflect.Modifier.PRIVATE;
            case ABSTRACT: return java.lang.reflect.Modifier.ABSTRACT;
            // case DEFAULT: no equivalent
            case STATIC: return java.lang.reflect.Modifier.STATIC;
            case FINAL: return java.lang.reflect.Modifier.FINAL;
            case TRANSIENT: return java.lang.reflect.Modifier.TRANSIENT;
            case VOLATILE: return java.lang.reflect.Modifier.VOLATILE;
            case SYNCHRONIZED: return java.lang.reflect.Modifier.SYNCHRONIZED;
            case NATIVE: return java.lang.reflect.Modifier.NATIVE;
            case STRICTFP: return java.lang.reflect.Modifier.STRICT;
            default: return 0;
        }
    }

}
