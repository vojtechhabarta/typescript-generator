
package cz.habarta.typescript.generator.parser;

import java.util.List;


public class DeclarationModel {

    private final Class<?> origin;
    private final List<String> comments;

    public DeclarationModel(Class<?> origin, List<String> comments) {
        this.origin = origin;
        this.comments = comments;
    }

    public Class<?> getOrigin() {
        return origin;
    }

    public List<String> getComments() {
        return comments;
    }

}
