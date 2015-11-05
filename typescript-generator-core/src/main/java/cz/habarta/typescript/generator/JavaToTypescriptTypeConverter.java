package cz.habarta.typescript.generator;

import java.lang.reflect.Type;

public abstract class JavaToTypescriptTypeConverter {
    public abstract TsType typeFromJava(Type javaType, JavaToTypescriptTypeConverter fallback);
    public TsType typeFromJava(Type javaType) {
        return typeFromJava(javaType, null);
    }
}
