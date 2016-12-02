
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.compiler.Symbol;
import java.util.*;


public class TsDeclarationModel implements Comparable<TsDeclarationModel> {

    protected final Class<?> origin;
    protected final Symbol name;
    protected final List<String> comments;

    public TsDeclarationModel(Symbol name, List<String> comments) {
        this(null, name, comments);
    }

    public TsDeclarationModel(Class<?> origin, Symbol name, List<String> comments) {
        this.origin = origin;
        this.name = name;
        this.comments = comments;
    }

    public Class<?> getOrigin() {
        return origin;
    }

    public Symbol getName() {
        return name;
    }

    public List<String> getComments() {
        return comments;
    }

    @Override
    public int compareTo(TsDeclarationModel o) {
        return name.toString().compareTo(o.name.toString());
    }

    @Override
    public String toString() {
        return String.format("Declaration `%s`", name);
    }

}
