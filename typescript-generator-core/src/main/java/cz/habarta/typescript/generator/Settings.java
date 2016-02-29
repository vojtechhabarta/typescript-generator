
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.emitter.EmitterExtension;
import java.io.File;
import java.util.*;


public class Settings {
    public String newline = String.format("%n");
    public String quotes = "\"";
    public String indentString = "    ";
    public TypeScriptFormat outputFileType = TypeScriptFormat.declarationFile;
    public JsonLibrary jsonLibrary = JsonLibrary.jackson1;
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
    public List<EmitterExtension> extensions = new ArrayList<>();

    public void validate() {
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
