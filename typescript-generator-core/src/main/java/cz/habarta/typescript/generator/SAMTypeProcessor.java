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

        if (shouldProcessParameterizedType(javaType)) {
            Result result = maybeProcessSAM(javaType, context, this::processParameterizedType);
            if (result != null) {
                return result;
            }
        }

        //Allow non-paramaterized SAM classes to be emitted if annotated properly
        if (shouldProcessNonParameterizedType(javaType)) {
            return maybeProcessSAM(javaType, context, this::processNonParameterizedType);
        }

        return null;
    }

    private boolean shouldProcessParameterizedType(Type javaType) {
        if (javaType instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) javaType;
            return parameterizedType.getRawType() instanceof Class && isValid((Class<?>) parameterizedType.getRawType());
        }
        return false;
    }

    private Result processParameterizedType(Type javaType, Method sam, Context context) {
        ParameterizedType parameterizedType = (ParameterizedType) javaType;
        Class<?> javaClass = (Class<?>) parameterizedType.getRawType();
        List<TypeVariable<? extends Class<?>>> typeVariables = Arrays.asList(javaClass.getTypeParameters());
        List<Type> typeArguments = Arrays.asList(parameterizedType.getActualTypeArguments());
        Iterator<Type> itor = typeArguments.iterator();
        Map<String, Type> genericTypeMap = typeVariables.stream().collect(toMap(TypeVariable::getName, v -> itor.next()));

        String genericReturnType = sam.getGenericReturnType().getTypeName();
        TsType returnType = context.processType(genericTypeMap.getOrDefault(genericReturnType, sam.getReturnType())).getTsType();

        List<TsParameter> parameters = new ArrayList<>();
        for (Type type : sam.getGenericParameterTypes()) {
            parameters.add(new TsParameter("arg" + parameters.size(),
                                           context.processType(genericTypeMap.getOrDefault(type.getTypeName(), type)).getTsType()));
        }

        return new Result(new TsType.FunctionType(parameters, returnType));
    }

    private boolean shouldProcessNonParameterizedType(Type javaType) {
        return emitSAMs.equals(EmitSAMStrictness.byAnnotationOnly) && javaType instanceof Class<?> &&
                isValid((Class<?>) javaType);
    }

    private Result processNonParameterizedType(Type type, Method method, Context context) {
        List<TsParameter> parameters = new ArrayList<>();
        for (Type paramType : method.getParameterTypes()) {
            parameters.add(new TsParameter("arg" + parameters.size(), context.processType(paramType).getTsType()));
        }
        return new Result(new TsType.FunctionType(parameters, context.processType(method.getReturnType()).getTsType()));
    }

    private Result maybeProcessSAM(Type javaType, Context ctx, SAMProcessor samProcessor) {
        Class<?> clazz;
        if (javaType instanceof ParameterizedType) {
            clazz = (Class<?>) ((ParameterizedType) javaType).getRawType();
        } else {
            clazz = (Class<?>) javaType;
        }
        Method sam = getSAMMaybe(clazz);
        Result res = null;
        if (sam != null) {
            res = samProcessor.process(javaType, sam, ctx);
        }
        return res;
    }

    private boolean isValid(Class javaClass) {
        return !emitSAMs.equals(EmitSAMStrictness.byAnnotationOnly) || Arrays.stream(javaClass.getAnnotations())
                .anyMatch(a -> Objects.equals(a.annotationType(), FunctionalInterface.class));
    }

    private static Method getSAMMaybe(Class<?> clazz) {
        return Arrays.stream(clazz.getMethods())
                .filter(m -> Modifier.isAbstract(m.getModifiers()))
                .findFirst()
                .orElse(null);
    }

    interface SAMProcessor {
        Result process(Type type, Method method, Context context);
    }

}
