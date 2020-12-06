
package cz.habarta.typescript.generator.compiler;

import java.util.List;


public class EnumMemberModel {
    
    private final String propertyName;
    private final Object/*String|Number*/ enumValue;
    private final List<String> comments;

    public EnumMemberModel(String propertyName, String enumValue, List<String> comments) {
        this(propertyName, (Object)enumValue, comments);
    }

    public EnumMemberModel(String propertyName, Number enumValue, List<String> comments) {
        this(propertyName, (Object)enumValue, comments);
    }

    private EnumMemberModel(String propertyName, Object enumValue, List<String> comments) {
        this.propertyName = propertyName;
        this.enumValue = enumValue;
        this.comments = comments;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Object getEnumValue() {
        return enumValue;
    }

    public List<String> getComments() {
        return comments;
    }

    public EnumMemberModel withPropertyName(String propertyName) {
        return new EnumMemberModel(propertyName, enumValue, comments);
    }

    public EnumMemberModel withComments(List<String> comments) {
        return new EnumMemberModel(propertyName, enumValue, comments);
    }

}
