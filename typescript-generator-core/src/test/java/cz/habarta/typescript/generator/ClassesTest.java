
package cz.habarta.typescript.generator;

import org.junit.Assert;
import org.junit.Test;


public class ClassesTest {

    @Test(expected = Exception.class)
    public void testInvalidSettings() {
        final Settings settings = TestUtils.settings();
        settings.mapClasses = ClassMapping.asClasses;
        new TypeScriptGenerator(settings).generateTypeScript(Input.from());
    }

    @Test
    public void testClass() {
        testOutput(A.class,
                "class A {\n" +
                "    a: string;\n" +
                "}"
        );
    }

    @Test
    public void testInheritedClass() {
        // A and B order is important
        testOutput(B.class,
                "class A {\n" +
                "    a: string;\n" +
                "}\n" +
                "\n" +
                "class B extends A {\n" +
                "    b: string;\n" +
                "}"
        );
    }

    @Test
    public void testClassImplementsInterface() {
        testOutput(E.class,
                "class E implements D {\n" +
                "    c: string;\n" +
                "    d: string;\n" +
                "    e: string;\n" +
                "}\n" +
                "\n" +
                "interface D extends C {\n" +
                "    d: string;\n" +
                "}\n" +
                "\n" +
                "interface C {\n" +
                "    c: string;\n" +
                "}"
        );
    }

    @Test
    public void testComplexHierarchy() {
        // Q3 and Q5 order is important
        testOutput(Q5.class,
                "class Q3 implements Q2 {\n" +
                "    q1: string;\n" +
                "    q2: string;\n" +
                "    q3: string;\n" +
                "}\n" +
                "\n" +
                "class Q5 extends Q3 implements Q2, Q4 {\n" +
                "    q4: string;\n" +
                "    q5: string;\n" +
                "}\n" +
                "\n" +
                "interface Q2 extends Q1 {\n" +
                "    q2: string;\n" +
                "}\n" +
                "\n" +
                "interface Q4 {\n" +
                "    q4: string;\n" +
                "}\n" +
                "\n" +
                "interface Q1 {\n" +
                "    q1: string;\n" +
                "}"
        );
    }

    private static void testOutput(Class<?> inputClass, String expected) {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.mapClasses = ClassMapping.asClasses;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(inputClass));
        Assert.assertEquals(expected.replace('\'', '"'), output.trim());
    }

    private static abstract class A {
        public abstract String getA();
    }

    private static abstract class B extends A {
        public abstract String getB();
    }

    private static abstract interface C {
        public abstract String getC();
    }

    private static interface D extends C {
        public abstract String getD();
    }

    private static abstract class E implements D {
        public abstract String getE();
    }


    private static interface Q1 {
        public abstract String getQ1();
    }

    private static interface Q2 extends Q1 {
        public abstract String getQ2();
    }

    private static abstract class Q3 implements Q2 {
        public abstract String getQ3();
    }

    private static interface Q4 {
        public abstract String getQ4();
    }

    private static abstract class Q5 extends Q3 implements Q2, Q4 {
        public abstract String getQ5();
    }

}
