
package cz.habarta.typescript.generator.compiler;

import java.lang.reflect.Field;
import java.util.List;


public class EnumMemberModel {

    private final String propertyName;
    private final Object/*String|Number*/ enumValue;
    private final Field originalField;
    private final List<String> comments;

    public EnumMemberModel(String propertyName, String enumValue, Field originalField, List<String> comments) {
        this(propertyName, (Object)enumValue, originalField, comments);
    }

    public EnumMemberModel(String propertyName, Number enumValue, Field originalField, List<String> comments) {
        this(propertyName, (Object)enumValue, originalField, comments);
    }

    private EnumMemberModel(String propertyName, Object enumValue, Field originalField, List<String> comments) {
        this.propertyName = propertyName;
        this.enumValue = enumValue;
        this.originalField = originalField;
        this.comments = comments;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public Object getEnumValue() {
        return enumValue;
    }

    public Field getOriginalField() {
        return originalField;
    }

    public List<String> getComments() {
        return comments;
    }

    public EnumMemberModel withPropertyName(String propertyName) {
        return new EnumMemberModel(propertyName, enumValue, originalField, comments);
    }

    public EnumMemberModel withComments(List<String> comments) {
        return new EnumMemberModel(propertyName, enumValue, originalField, comments);
    }

}
