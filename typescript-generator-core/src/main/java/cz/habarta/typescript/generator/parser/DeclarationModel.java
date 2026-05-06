
package cz.habarta.typescript.generator.parser;

import java.util.List;
import org.jspecify.annotations.Nullable;


public abstract class DeclarationModel {

    protected final Class<?> origin;
    protected final @Nullable List<String> comments;

    public DeclarationModel(Class<?> origin, @Nullable List<String> comments) {
        this.origin = origin;
        this.comments = comments;
    }

    public Class<?> getOrigin() {
        return origin;
    }

    public @Nullable List<String> getComments() {
        return comments;
    }

    public abstract DeclarationModel withComments(@Nullable List<String> comments);

}
