
package cz.habarta.typescript.generator.compiler;

import java.lang.reflect.Field;
import java.util.List;
import org.jspecify.annotations.Nullable;


public class EnumMemberModel {

    private final String propertyName;
    private final @Nullable Object/*String|Number*/ enumValue;
    private final @Nullable Field originalField;
    private final @Nullable List<String> comments;

    public EnumMemberModel(String propertyName, String enumValue, @Nullable Field originalField, @Nullable List<String> comments) {
        this(propertyName, (Object) enumValue, originalField, comments);
    }

    public EnumMemberModel(String propertyName, @Nullable Number enumValue, @Nullable Field originalField, @Nullable List<String> comments) {
        this(propertyName, (Object) enumValue, originalField, comments);
    }

    private EnumMemberModel(String propertyName, @Nullable Object enumValue, @Nullable Field originalField, @Nullable List<String> comments) {
        this.propertyName = propertyName;
        this.enumValue = enumValue;
        this.originalField = originalField;
        this.comments = comments;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public @Nullable Object getEnumValue() {
        return enumValue;
    }

    public @Nullable Field getOriginalField() {
        return originalField;
    }

    public @Nullable List<String> getComments() {
        return comments;
    }

    public EnumMemberModel withPropertyName(String propertyName) {
        return new EnumMemberModel(propertyName, enumValue, originalField, comments);
    }

    public EnumMemberModel withComments(@Nullable List<String> comments) {
        return new EnumMemberModel(propertyName, enumValue, originalField, comments);
    }

}
