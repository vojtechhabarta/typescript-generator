
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.databind.Module;
import cz.habarta.typescript.generator.emitter.Emitter;
import cz.habarta.typescript.generator.emitter.EmitterExtension;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.util.Predicate;
import java.io.File;
import java.lang.annotation.Annotation;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.regex.Pattern;


/**
 * @see cz.habarta.typescript.generator.maven.GenerateMojo
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
    public JsonLibrary jsonLibrary = null;
    private Predicate<String> excludeFilter = null;
    @Deprecated public boolean declarePropertiesAsOptional = false;
    public OptionalProperties optionalProperties; // default is OptionalProperties.useSpecifiedAnnotations
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
    public ClassMapping mapClasses; // default is ClassMapping.asInterfaces
    public boolean disableTaggedUnions = false;
    public boolean ignoreSwaggerAnnotations = false;
    public boolean generateJaxrsApplicationInterface = false;
    public boolean generateJaxrsApplicationClient = false;
    public JaxrsNamespacing jaxrsNamespacing;
    public Class<? extends Annotation> jaxrsNamespacingAnnotation = null;
    public String jaxrsNamespacingAnnotationElement;  // default is "value"
    public String restResponseType = null;
    public String restOptionsType = null;
    public boolean restOptionsTypeIsGeneric;
    public TypeProcessor customTypeProcessor = null;
    public boolean sortDeclarations = false;
    public boolean sortTypeDeclarations = false;
    public boolean noFileComment = false;
    public List<File> javadocXmlFiles = null;
    public List<EmitterExtension> extensions = new ArrayList<>();
    public List<Class<? extends Annotation>> includePropertyAnnotations = new ArrayList<>();
    public List<Class<? extends Annotation>> optionalAnnotations = new ArrayList<>();
    public boolean generateNpmPackageJson = false;
    public String npmName = null;
    public String npmVersion = null;
    public Map<String, String> npmPackageDependencies = new LinkedHashMap<>();
    public String typescriptVersion = "^2.4";
    public boolean displaySerializerWarning = true;
    @Deprecated public boolean disableJackson2ModuleDiscovery = false;
    public boolean jackson2ModuleDiscovery = false;
    public List<Class<? extends Module>> jackson2Modules = new ArrayList<>();
    public ClassLoader classLoader = null;

    private boolean defaultStringEnumsOverriddenByExtension = false;


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

    public void loadCustomTypeProcessor(ClassLoader classLoader, String customTypeProcessor) {
        if (customTypeProcessor != null) {
            this.customTypeProcessor = loadInstance(classLoader, customTypeProcessor, TypeProcessor.class);
        }
    }

    public void loadExtensions(ClassLoader classLoader, List<String> extensions) {
        if (extensions != null) {
            this.extensions = loadInstances(classLoader, extensions, EmitterExtension.class);
        }
    }

    public void loadIncludePropertyAnnotations(ClassLoader classLoader, List<String> includePropertyAnnotations) {
        if (includePropertyAnnotations != null) {
            this.includePropertyAnnotations = loadClasses(classLoader, includePropertyAnnotations, Annotation.class);
        }
    }

    public void loadOptionalAnnotations(ClassLoader classLoader, List<String> optionalAnnotations) {
        if (optionalAnnotations != null) {
            this.optionalAnnotations = loadClasses(classLoader, optionalAnnotations, Annotation.class);
        }
    }

    public void loadJackson2Modules(ClassLoader classLoader, List<String> jackson2Modules) {
        if (jackson2Modules != null) {
            this.jackson2Modules = loadClasses(classLoader, jackson2Modules, Module.class);
        }
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
        if (umdNamespace != null && !Emitter.isValidIdentifierName(umdNamespace)) {
            throw new RuntimeException("Value of 'umdNamespace' parameter is not valid identifier: " + umdNamespace + ". " + seeLink());
        }
        if (jsonLibrary == null) {
            throw new RuntimeException("Required 'jsonLibrary' parameter is not configured.");
        }
        for (EmitterExtension extension : extensions) {
            final String extensionName = extension.getClass().getSimpleName();
            final DeprecationText deprecation = extension.getClass().getAnnotation(DeprecationText.class);
            if (deprecation != null) {
                System.out.println(String.format("Warning: Extension '%s' is deprecated: %s", extensionName, deprecation.value()));
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
        if (nonConstEnums && outputFileType != TypeScriptFileType.implementationFile) {
            throw new RuntimeException("Non-const enums can only be used in implementation files but 'outputFileType' parameter is not set to 'implementationFile'.");
        }
        if (mapClasses == ClassMapping.asClasses && outputFileType != TypeScriptFileType.implementationFile) {
            throw new RuntimeException("'mapClasses' parameter is set to 'asClasses' which generates runtime code but 'outputFileType' parameter is not set to 'implementationFile'.");
        }
        if (generateJaxrsApplicationClient && outputFileType != TypeScriptFileType.implementationFile) {
            throw new RuntimeException("'generateJaxrsApplicationClient' can only be used when generating implementation file ('outputFileType' parameter is 'implementationFile').");
        }
        final boolean generateJaxrs = generateJaxrsApplicationClient || generateJaxrsApplicationInterface;
        if (jaxrsNamespacing != null && !generateJaxrs) {
            throw new RuntimeException("'jaxrsNamespacing' parameter can only be used when generating JAX-RS client or interface.");
        }
        if (jaxrsNamespacingAnnotation != null && jaxrsNamespacing != JaxrsNamespacing.byAnnotation) {
            throw new RuntimeException("'jaxrsNamespacingAnnotation' parameter can only be used when 'jaxrsNamespacing' parameter is set to 'byAnnotation'.");
        }
        if (jaxrsNamespacingAnnotation == null && jaxrsNamespacing == JaxrsNamespacing.byAnnotation) {
            throw new RuntimeException("'jaxrsNamespacingAnnotation' must be specified when 'jaxrsNamespacing' parameter is set to 'byAnnotation'.");
        }
        if (restResponseType != null && !generateJaxrs) {
            throw new RuntimeException("'restResponseType' parameter can only be used when generating JAX-RS client or interface.");
        }
        if (restOptionsType != null && !generateJaxrs) {
            throw new RuntimeException("'restOptionsType' parameter can only be used when generating JAX-RS client or interface.");
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
        }

        if (declarePropertiesAsOptional) {
            System.out.println("Warning: Parameter 'declarePropertiesAsOptional' is deprecated. Use 'optionalProperties' parameter.");
            if (optionalProperties == null) {
                optionalProperties = OptionalProperties.all;
            }
        }
        if (disableJackson2ModuleDiscovery) {
            System.out.println("Warning: Parameter 'disableJackson2ModuleDiscovery' was removed. See 'jackson2ModuleDiscovery' and 'jackson2Modules' parameters.");
        }
    }

    private static void reportConfigurationChange(String extensionName, String parameterName, String parameterValue) {
        System.out.println(String.format("Configuration: '%s' extension set '%s' parameter to '%s'", extensionName, parameterName, parameterValue));
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
        final List<Pattern> patterns = Input.globsToRegexps(excludedClassPatterns != null ? excludedClassPatterns : Collections.<String>emptyList());
        return new Predicate<String>() {
            @Override
            public boolean test(String className) {
                return names.contains(className) || Input.classNameMatches(className, patterns);
            }
        };
    }

    public void setJaxrsNamespacingAnnotation(ClassLoader classLoader, String jaxrsNamespacingAnnotation) {
        if (jaxrsNamespacingAnnotation != null) {
            final String[] split = jaxrsNamespacingAnnotation.split("#");
            final String className = split[0];
            final String elementName = split.length > 1 ? split[1] : "value";
            this.jaxrsNamespacingAnnotation = loadClass(classLoader, className, Annotation.class);
            this.jaxrsNamespacingAnnotationElement = elementName;
        }
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

    public boolean areDefaultStringEnumsOverriddenByExtension() {
        return defaultStringEnumsOverriddenByExtension;
    }

    private String seeLink() {
        return "For more information see 'http://vojtechhabarta.github.io/typescript-generator/doc/ModulesAndNamespaces.html'.";
    }

    private static <T> List<Class<? extends T>> loadClasses(ClassLoader classLoader, List<String> classNames, Class<T> requiredClassType) {
        if (classNames == null) {
            return null;
        }
        final List<Class<? extends T>> classes = new ArrayList<>();
        for (String className : classNames) {
            classes.add(loadClass(classLoader, className, requiredClassType));
        }
        return classes;
    }

    private static <T> Class<? extends T> loadClass(ClassLoader classLoader, String className, Class<T> requiredClassType) {
        try {
            System.out.println("Loading class " + className);
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
            return null;
        }
        final List<T> instances = new ArrayList<>();
        for (String className : classNames) {
            instances.add(loadInstance(classLoader, className, requiredType));
        }
        return instances;
    }

    private static <T> T loadInstance(ClassLoader classLoader, String className, Class<T> requiredType) {
        try {
            System.out.println("Loading class " + className);
            return requiredType.cast(classLoader.loadClass(className).newInstance());
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

}
