
package cz.habarta.typescript.generator.util;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;


public class UnionType implements Type {

    public final List<Type> types;

    public UnionType(Type... types) {
        this(Arrays.asList(types));
    }

    public UnionType(List<Type> types) {
        this.types = types;
    }

}
