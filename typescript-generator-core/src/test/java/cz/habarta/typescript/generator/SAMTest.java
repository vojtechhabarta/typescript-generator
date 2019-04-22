/*
 * Copyright (C) 2010-2019 Evergage, Inc.
 * All rights reserved.
 */

package cz.habarta.typescript.generator;

import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Assert;
import org.junit.Test;



public class SAMTest {
    @Test
    public void testSAM() {
        testOutput(FunctionTest.class, "interface FunctionTest {\n    convertToString: (arg0: number) => string;\n}");
        testOutput(MixedGenericFixedParamTest.class, "interface MixedGenericFixedParamTest {\n    convertToString: (arg0: number, arg1: string) => string;\n}");
        testOutput(FixedReturnSAMTest.class, "interface FixedReturnSAMTest {\n    convertToString: (arg0: number, arg1: string) => string;\n}");
        testOutput(VoidReturnSAMTest.class, "interface VoidReturnSAMTest {\n    consumeInt: (arg0: number) => void;\n}");
        testOutput(SupplierSAMTest.class, "interface SupplierSAMTest {\n    supplyInt: () => number;\n}");
        testOutput(SAMInterface.class, "interface SAMInterface {\n\n    foo(arg0: (arg0: string) => number): () => number;\n}");
        testOutput(SAMInterfaceGeneric.class, "interface SAMInterfaceGeneric<T> {\n\n    foo(arg0: (arg0: T) => number): () => T;\n}");
        testOutput(SetterTest.class, "interface SetterTest {\n\n    setThing(arg0: string, arg1: any, arg2: (arg0: string) => string): () => number;\n}");
//        Decision made to only emit parameterized classes causes the test below to fail.
//        testOutput(NonParameterizedSAMTest.class, "interface NonParameterizedSAMTest {\n    convertToString: (arg0: number) => string;\n}");
    }

//    public interface NonParameterizedSAM {
//        String apply(Integer foo);
//    }
//
//    public class NonParameterizedSAMTest {
//        public NonParameterizedSAM convertToString;
//    }

    interface SAMInterface {
        Supplier<Integer> foo(Function<String, Double> fun);
    }

    interface SAMInterfaceGeneric<T> {
        Supplier<T> foo(Function<T, Double> fun);
    }

    private class FunctionTest {
        public Function<Integer, String> convertToString;
    }

    public interface MixedGenericFixedParamSAM<T, R> {
        R apply(T t, String fixedType);
    }

    public class MixedGenericFixedParamTest {
        public MixedGenericFixedParamSAM<Integer, String> convertToString;
    }

    public interface FixedReturnSAM<T> {
        String apply(T t, String fixedType);
    }

    public class FixedReturnSAMTest {
        public FixedReturnSAM<Integer> convertToString;
    }


    public interface VoidReturnSAM<T> {
        void apply(T arg);
    }

    public class VoidReturnSAMTest {
        public VoidReturnSAM<Integer> consumeInt;
    }

    public interface SupplierSAM<T> {
        T get();
    }

    public class SupplierSAMTest {
        public SupplierSAM<Integer> supplyInt;
    }

    public abstract class SetterTest {
        abstract SupplierSAM<Integer> setThing(String thingKey, Object val, Function<String, String> function);
    }

    private static void testOutput(Class<?> inputClass, String expected) {
        final Settings settings = TestUtils.settings();
        settings.emitSAMs = true;
        settings.emitAbstractMethodsInBeans = true;
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.mapClasses = ClassMapping.asInterfaces;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(inputClass));
        Assert.assertEquals(expected.replace('\'', '"'), output.trim());
    }

}
