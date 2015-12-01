package cz.habarta.typescript.generator;

import java.lang.reflect.Type;

public abstract class JavaToTypeScriptTypeConverter {
    public abstract TsType typeFromJava(Type javaType, JavaToTypeScriptTypeConverter fallback);
    public TsType typeFromJava(Type javaType) {
        return typeFromJava(javaType, null);
    }
}
