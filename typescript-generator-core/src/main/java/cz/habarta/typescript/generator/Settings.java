
package cz.habarta.typescript.generator;

import java.util.*;


public class Settings {
    public String newline = String.format("%n");
    public String indentString = "    ";
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
    public boolean noFileComment = false;
}
