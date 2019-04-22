/*
 * Copyright (C) 2010-2019 Evergage, Inc.
 * All rights reserved.
 */

package cz.habarta.typescript.generator;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import static java.util.stream.Collectors.toMap;

public class SAMTypeProcessor implements TypeProcessor {


    private EmitSAMStrictness emitSAMs;

    public SAMTypeProcessor(EmitSAMStrictness emitSAMs) {
        this.emitSAMs = emitSAMs;
    }

    @Override
    public Result processType(Type javaType, Context context) {

        if (javaType instanceof ParameterizedType) {
            final ParameterizedType parameterizedType = (ParameterizedType) javaType;
            if (parameterizedType.getRawType() instanceof Class && isValid((Class<?>) parameterizedType.getRawType())) {
                final Class<?> javaClass = (Class<?>) parameterizedType.getRawType();
                Method sam = getSAMMaybe(javaClass);
                if (sam != null) {
                    List<TypeVariable<? extends Class<?>>> typeVariables = Arrays.asList(javaClass.getTypeParameters());
                    List<Type> typeArguments = Arrays.asList(parameterizedType.getActualTypeArguments());
                    Iterator<Type> itor = typeArguments.iterator();
                    Map<String, Type> genericTypeMap = typeVariables.stream().collect(toMap(TypeVariable::getName, v -> itor.next()));

                    String genericReturnType = sam.getGenericReturnType().getTypeName();
                    TsType returnType = context.processType(genericTypeMap.getOrDefault(genericReturnType, sam.getReturnType())).getTsType();

                    List<TsParameter> parameters = new ArrayList<>();
                    for (Type type : sam.getGenericParameterTypes()) {
                        parameters.add(new TsParameter("arg" + parameters.size(), context.processType(genericTypeMap.getOrDefault(type.getTypeName(), type)).getTsType()));
                    }

                    return new Result(new TsType.FunctionType(parameters, returnType));
                }
            }
        }

        //Allow non-paramaterized SAM classes to be emitted if annotated properly
        if (emitSAMs.equals(EmitSAMStrictness.byClassDefinitionAndAnnotation)) {
            if (javaType instanceof Class<?> && isValid((Class<?>) javaType)) {
                Method sam = getSAMMaybe((Class<?>) javaType);
                if (sam != null) {
                    List<TsParameter> parameters = new ArrayList<>();
                    for (Type type : sam.getParameterTypes()) {
                        parameters
                                .add(new TsParameter("arg" + parameters.size(), context.processType(type).getTsType()));
                    }
                    return new Result(new TsType.FunctionType(parameters,
                                                              context.processType(sam.getReturnType()).getTsType()));
                }
            }
        }

        return null;
    }

    private boolean isValid(Class javaClass) {
        return !emitSAMs.equals(EmitSAMStrictness.byClassDefinitionAndAnnotation) ||
                Arrays.stream(javaClass.getAnnotations())
                        .anyMatch(a -> Objects.equals(a.annotationType(), FunctionalInterface.class));
    }

    private static Method getSAMMaybe(Class<?> clazz) {
        return Arrays.stream(clazz.getMethods())
                .filter(m -> Modifier.isAbstract(m.getModifiers()))
                .findFirst()
                .orElse(null);
    }

}
