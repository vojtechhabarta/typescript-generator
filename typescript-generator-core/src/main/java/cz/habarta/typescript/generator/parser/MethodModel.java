
package cz.habarta.typescript.generator.parser;

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;


public class MethodModel {

    private final Class<?> originClass;
    private final String name;
    private final List<MethodParameterModel> parameters;
    private final Type returnType;

    public MethodModel(Class<?> originClass, String name, List<MethodParameterModel> parameters, Type returnType) {
        this.originClass = originClass;
        this.name = name;
        this.parameters = parameters != null ? parameters : Collections.<MethodParameterModel>emptyList();
        this.returnType = returnType;
    }

    public Class<?> getOriginClass() {
        return originClass;
    }

    public String getName() {
        return name;
    }

    public List<MethodParameterModel> getParameters() {
        return parameters;
    }

    public Type getReturnType() {
        return returnType;
    }

}
