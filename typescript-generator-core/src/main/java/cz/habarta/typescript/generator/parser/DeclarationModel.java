
package cz.habarta.typescript.generator.parser;

import java.util.List;


public abstract class DeclarationModel {

    protected final Class<?> origin;
    protected final List<String> comments;

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

    public abstract DeclarationModel withComments(List<String> comments);

}
