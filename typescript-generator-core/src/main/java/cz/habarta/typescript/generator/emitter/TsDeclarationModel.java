
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsType;
import java.util.*;


public class TsDeclarationModel implements Comparable<TsDeclarationModel> {

    protected final Class<?> origin;
    protected final TsType name;
    protected final List<String> comments;

    public TsDeclarationModel(TsType name, List<String> comments) {
        this(null, name, comments);
    }

    public TsDeclarationModel(Class<?> origin, TsType name, List<String> comments) {
        this.origin = origin;
        this.name = name;
        this.comments = comments;
    }

    public Class<?> getOrigin() {
        return origin;
    }

    public TsType getName() {
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
