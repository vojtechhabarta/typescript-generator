
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.emitter.EmitterExtension;
import java.io.File;
import java.lang.annotation.Annotation;
import java.util.*;


public class Settings {
    public String newline = String.format("%n");
    public String quotes = "\"";
    public String indentString = "    ";
    public TypeScriptFormat outputFileType = TypeScriptFormat.declarationFile;
    public JsonLibrary jsonLibrary = null;
    public String namespace = null;
    public String module = null;
    public List<String> excludedClassNames = null;
    public boolean declarePropertiesAsOptional = false;
    public String removeTypeNamePrefix = null;
    public String removeTypeNameSuffix = null;
    public String addTypeNamePrefix = null;
    public String addTypeNameSuffix = null;
    public DateMapping mapDate = DateMapping.asDate;
    public TypeProcessor customTypeProcessor = null;
    public boolean sortDeclarations = false;
    public boolean sortTypeDeclarations = false;
    public boolean noFileComment = false;
    public List<File> javadocXmlFiles = null;
    public List<EmitterExtension> extensions = new ArrayList<>();

    /**
     * The presence of any annotation in this list on a json property will cause the
     * typescript generator to treat that property as optional when generating the
     * corresponding typescript interface.
     * <p>
     * Note: When using a {@link cz.habarta.typescript.generator.parser.Jackson1Parser}
     * to generate your model, any annotation specified here will need to pass the
     * {@link org.codehaus.jackson.map.AnnotationIntrospector#isHandled} check performed
     * by the {@link org.codehaus.jackson.map.ObjectMapper} used to construct your model
     * parser. If you control the annotations in question, the easiest way to do this is
     * to annotate your annotations as
     * {@link org.codehaus.jackson.annotate.JacksonAnnotation}s.
     */
    public List<Class<? extends Annotation>> optionalAnnotations = new ArrayList<>();

    public void validate() {
        if (jsonLibrary == null) {
            throw new RuntimeException("Required 'jsonLibrary' is not configured.");
        }
        if (outputFileType != TypeScriptFormat.implementationFile) {
            for (EmitterExtension emitterExtension : extensions) {
                if (emitterExtension.generatesRuntimeCode()) {
                    throw new RuntimeException(String.format("Extension '%s' generates runtime code but 'outputFileType' is not set to 'implementationFile'.",
                            emitterExtension.getClass().getSimpleName()));
                }
            }
        }
    }

    public void validateFileName(File outputFile) {
        if (outputFileType == TypeScriptFormat.declarationFile && !outputFile.getName().endsWith(".d.ts")) {
            throw new RuntimeException("Declaration file must have 'd.ts' extension: " + outputFile);
        }
        if (outputFileType == TypeScriptFormat.implementationFile && (!outputFile.getName().endsWith(".ts") || outputFile.getName().endsWith(".d.ts"))) {
            throw new RuntimeException("Implementation file must have 'ts' extension: " + outputFile);
        }
        if (outputFileType == TypeScriptFormat.implementationFile && module != null && !outputFile.getName().equals(module + ".ts")) {
            throw new RuntimeException(String.format("Implementation file must be named '%s' when module name is '%s'.", module + ".ts", module));
        }
    }

}
