
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
    public JavaToTypescriptTypeParser customTypeParser = new JavaToTypescriptTypeParser() {
        @Override
        public TsType typeFromJava(Type javaType, JavaToTypescriptTypeParser fallback) {
            return null;
        }
    };
    public String defaultCustomTypePrefix = "";
}
