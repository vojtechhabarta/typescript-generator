
package cz.habarta.typescript.generator;


public class Settings {
    public String newline = String.format("%n");
    public String indentString = "    ";
    public JsonLibrary jsonLibrary = JsonLibrary.jackson1;
    public String namespace = null;
    public String module = null;
    public boolean declarePropertiesAsOptional = false;
    public String removeTypeNameSuffix = null;
    public DateMapping mapDate = DateMapping.asDate;
    public TypeProcessor customTypeProcessor = null;
}
