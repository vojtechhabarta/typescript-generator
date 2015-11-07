
package cz.habarta.typescript.generator;

import java.lang.reflect.Type;

public class Settings {
    public String newline = String.format("%n");
    public String indentString = "    ";
    public JsonLibrary jsonLibrary = JsonLibrary.jackson1;
    public String namespace = null;
    public String module = null;
    public boolean declarePropertiesAsOptional = false;
    public String removeTypeNameSuffix = null;
    public DateMapping mapDate = DateMapping.asDate;
    public JavaToTypescriptTypeConverter customTypeParser = new JavaToTypescriptTypeConverter() {
        @Override
        public TsType typeFromJava(Type javaType, JavaToTypescriptTypeConverter fallback) {
            return null;
        }
    };
    public String defaultCustomTypePrefix = "";
    public String declarationPrefix = "";
    public int initialIndentationLevel = 0;
}
