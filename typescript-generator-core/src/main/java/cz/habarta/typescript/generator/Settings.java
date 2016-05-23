
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.emitter.EmitterExtension;
import java.io.File;
import java.lang.annotation.Annotation;
import java.util.*;


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
    public JsonLibrary jsonLibrary = null;
    public List<String> excludedClassNames = null;
    public boolean declarePropertiesAsOptional = false;
    public String removeTypeNamePrefix = null;
    public String removeTypeNameSuffix = null;
    public String addTypeNamePrefix = null;
    public String addTypeNameSuffix = null;
    public Map<String, String> customTypeNaming = new LinkedHashMap<>();
    public List<String> referencedFiles = new ArrayList<>();
    public List<String> importDeclarations = new ArrayList<>();
    public Map<String, String> customTypeMappings = new LinkedHashMap<>();
    public DateMapping mapDate = DateMapping.asDate;
    public TypeProcessor customTypeProcessor = null;
    public boolean sortDeclarations = false;
    public boolean sortTypeDeclarations = false;
    public boolean noFileComment = false;
    public List<File> javadocXmlFiles = null;
    public List<EmitterExtension> extensions = new ArrayList<>();
    public List<Class<? extends Annotation>> includePropertyAnnotations = new ArrayList<>();
    public List<Class<? extends Annotation>> optionalAnnotations = new ArrayList<>();
    public boolean experimentalInlineEnums = false;
    public boolean displaySerializerWarning = true;


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
        if (outputKind == TypeScriptOutputKind.ambientModule && module == null) {
            throw new RuntimeException("'module' parameter must be specified for ambient module. " + seeLink());
        }
        if (outputKind != TypeScriptOutputKind.ambientModule && module != null) {
            throw new RuntimeException("'module' parameter is only applicable to ambient modules. " + seeLink());
        }
        if (outputKind == TypeScriptOutputKind.ambientModule && outputFileType == TypeScriptFileType.implementationFile) {
            throw new RuntimeException("Ambient modules are not supported in implementation files. " + seeLink());
        }
        if (jsonLibrary == null) {
            throw new RuntimeException("Required 'jsonLibrary' parameter is not configured.");
        }
        if (outputFileType != TypeScriptFileType.implementationFile) {
            for (EmitterExtension emitterExtension : extensions) {
                if (emitterExtension.generatesRuntimeCode()) {
                    throw new RuntimeException(String.format("Extension '%s' generates runtime code but 'outputFileType' parameter is not set to 'implementationFile'.",
                            emitterExtension.getClass().getSimpleName()));
                }
            }
        }
    }

    public void validateFileName(File outputFile) {
        if (outputFileType == TypeScriptFileType.declarationFile && !outputFile.getName().endsWith(".d.ts")) {
            throw new RuntimeException("Declaration file must have 'd.ts' extension: " + outputFile);
        }
        if (outputFileType == TypeScriptFileType.implementationFile && (!outputFile.getName().endsWith(".ts") || outputFile.getName().endsWith(".d.ts"))) {
            throw new RuntimeException("Implementation file must have 'ts' extension: " + outputFile);
        }
    }

    private String seeLink() {
        return "For more information see 'http://vojtechhabarta.github.io/typescript-generator/doc/ModulesAndNamespaces.html'.";
    }

    private static <T> List<Class<? extends T>> loadClasses(ClassLoader classLoader, List<String> classNames, Class<T> requiredClassType) {
        if (classNames == null) {
            return null;
        }
        try {
            final List<Class<? extends T>> classes = new ArrayList<>();
            for (String className : classNames) {
                System.out.println("Loading class " + className);
                final Class<?> loadedClass = classLoader.loadClass(className);
                if (requiredClassType.isAssignableFrom(loadedClass)) {
                    @SuppressWarnings("unchecked")
                    final Class<? extends T> castedClass = (Class<? extends T>) loadedClass;
                    classes.add(castedClass);
                } else {
                    throw new RuntimeException(String.format("Class '%s' is not assignable to '%s'.", loadedClass, requiredClassType));
                }
            }
            return classes;
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
