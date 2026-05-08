
package cz.habarta.typescript.generator.parser;

import java.lang.reflect.Type;
import org.jspecify.annotations.Nullable;


public class SourceType<T extends Type> {

    public final T type;
    public final @Nullable Class<?> usedInClass;
    public final @Nullable String usedInMember;

    public SourceType(T type) {
        this(type, null, null);
    }

    public SourceType(T type, @Nullable Class<?> usedInClass, @Nullable String usedInMember) {
        this.type = type;
        this.usedInClass = usedInClass;
        this.usedInMember = usedInMember;
    }

    @SuppressWarnings("unchecked")
    public SourceType<Class<?>> asSourceClass() {
        Class.class.cast(this.type);
        return (SourceType<Class<?>>) this;
    }

    @Override
    public String toString() {
        return type.toString();
    }

}
