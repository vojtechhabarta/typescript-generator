/*
 * Copyright (C) 2010-2019 Evergage, Inc.
 * All rights reserved.
 */

package cz.habarta.typescript.generator;

import java.util.function.Function;

import org.junit.Assert;
import org.junit.Test;



public class SAMTest {
    @Test
    public void testSAM() {
        testOutput(FunctionTest.class, "interface FunctionTest {\n    convertToString: (arg0: number) => string;\n}");
        testOutput(MixedGenericFixedParamTest.class, "interface MixedGenericFixedParamTest {\n    convertToString: (arg0: number, arg1: string) => string;\n}");
        testOutput(NonParameterizedSAMTest.class, "interface NonParameterizedSAMTest {\n    convertToString: (arg0: number) => string;\n}");
        testOutput(FixedReturnSAMTest.class, "interface FixedReturnSAMTest {\n    convertToString: (arg0: number, arg1: string) => string;\n}");
    }


    private abstract class FunctionTest {
        public Function<Integer, String> convertToString;
    }

    public interface MixedGenericFixedParamSAM<T, R> {
        R apply(T t, String fixedType);
    }

    public abstract class MixedGenericFixedParamTest {
        public MixedGenericFixedParamSAM<Integer, String> convertToString;
    }

    public interface FixedReturnSAM<T> {
        String apply(T t, String fixedType);
    }

    public abstract class FixedReturnSAMTest {
        public FixedReturnSAM<Integer> convertToString;
    }

    public interface NonParameterizedSAM {
        String apply(Integer foo);
    }

    public abstract class NonParameterizedSAMTest {
       public NonParameterizedSAM convertToString;
    }

    private static void testOutput(Class<?> inputClass, String expected) {
        final Settings settings = TestUtils.settings();
        settings.emitSAMs = true;
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.mapClasses = ClassMapping.asInterfaces;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(inputClass));
        Assert.assertEquals(expected.replace('\'', '"'), output.trim());
    }

}
