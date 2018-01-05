
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.compiler.Symbol;
import java.util.*;


public class TsDeclarationModel implements Comparable<TsDeclarationModel> {

    protected final Class<?> origin;
    protected final TsBeanCategory category;
    protected final Symbol name;
    protected final List<String> comments;

    public TsDeclarationModel(Class<?> origin, TsBeanCategory category, Symbol name, List<String> comments) {
        this.origin = origin;
        this.category = category;
        this.name = name;
        this.comments = comments;
    }

    public Class<?> getOrigin() {
        return origin;
    }

    public TsBeanCategory getCategory() {
        return category;
    }

    public Symbol getName() {
        return name;
    }

    public List<String> getComments() {
        return comments;
    }

    @Override
    public int compareTo(TsDeclarationModel o) {
        final int categoryResult = compare(this.category, o.category);
        if (categoryResult != 0) {
            return categoryResult;
        }
        final int nameResult = compare(this.name.getFullName(), o.name.getFullName());
        if (nameResult != 0) {
            return nameResult;
        }
        return 0;
    }

    /**
     * Natural order with null last.
     */
    private static <T extends Comparable<T>> int compare(T o1, T o2) {
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
