
package cz.habarta.typescript.generator.compiler;

import java.util.List;


public class EnumMemberModel<T> {
    
    private final String propertyName;
    private final T enumValue;
    private final List<String> comments;

    public EnumMemberModel(String propertyName, T enumValue, List<String> comments) {
        this.propertyName = propertyName;
        this.enumValue = enumValue;
        this.comments = comments;
    }

    public String getPropertyName() {
        return propertyName;
    }

    public T getEnumValue() {
        return enumValue;
    }

    public List<String> getComments() {
        return comments;
    }

    public EnumMemberModel<T> withComments(List<String> comments) {
        return new EnumMemberModel<>(propertyName, enumValue, comments);
    }

}
