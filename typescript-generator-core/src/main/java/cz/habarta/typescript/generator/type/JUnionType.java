
package cz.habarta.typescript.generator.type;

import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;


public class JUnionType implements Type {

    private final List<Type> types;

    public JUnionType(Type... types) {
        this(Arrays.asList(types));
    }

    public JUnionType(List<Type> types) {
        this.types = types;
    }

    public List<Type> getTypes() {
        return types;
    }

    @Override
    public String toString() {
        return "(" +
                types.stream().map(Type::toString).collect(Collectors.joining(" | "))
                + ")";
    }

}
