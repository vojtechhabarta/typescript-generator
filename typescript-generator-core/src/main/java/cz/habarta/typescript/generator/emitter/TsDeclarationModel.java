
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.compiler.Symbol;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;


public class TsDeclarationModel implements Comparable<TsDeclarationModel> {

    protected final @Nullable Class<?> origin;
    protected final @Nullable TsBeanCategory category;
    protected final Symbol name;
    protected final @Nullable List<String> comments;

    public TsDeclarationModel(@Nullable Class<?> origin, @Nullable TsBeanCategory category, Symbol name, @Nullable List<String> comments) {
        this.origin = origin;
        this.category = category;
        this.name = Objects.requireNonNull(name);
        this.comments = comments;
    }

    public @Nullable Class<?> getOrigin() {
        return origin;
    }

    public @Nullable TsBeanCategory getCategory() {
        return category;
    }

    public Symbol getName() {
        return name;
    }

    public @Nullable List<String> getComments() {
        return comments;
    }

    @Override
    public int compareTo(TsDeclarationModel o) {
        final int categoryResult = compare(this.category, o.category);
        if (categoryResult != 0) {
            return categoryResult;
        }
        final int nameResult = this.name.getFullName().compareToIgnoreCase(o.name.getFullName());
        if (nameResult != 0) {
            return nameResult;
        }
        return 0;
    }

    /**
     * Natural order with null last.
     */
    private static <T extends Comparable<T>> int compare(@Nullable T o1, @Nullable T o2) {
        if (o1 != null) {
            return o2 != null ? o1.compareTo(o2) : -1;
        } else {
            return o2 != null ? 1 : 0;
        }
    }

    @Override
    public String toString() {
        return String.format("Declaration `%s`", name);
    }

}
