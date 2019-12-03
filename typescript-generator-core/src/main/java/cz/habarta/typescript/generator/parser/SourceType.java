
package cz.habarta.typescript.generator.parser;

import java.lang.reflect.Type;
import kotlin.reflect.KType;


public class SourceType<T extends Type> {
    
    public final T type;
    public final KType kType;
    public final Class<?> usedInClass;
    public final String usedInMember;

    public SourceType(T type) {
        this (type, null, null, null);
    }

    public SourceType(T type, Class<?> usedInClass, String usedInMember) {
        this (type, null, null, null);
    }

    public SourceType(T type, KType kType, Class<?> usedInClass, String usedInMember) {
        this.type = type;
        this.kType = kType;
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
