package cz.habarta.typescript.generator;

import java.lang.reflect.Type;

public interface JavaToTypescriptTypeParser {
    TsType typeFromJava(Type javaType, JavaToTypescriptTypeParser fallback);
}
