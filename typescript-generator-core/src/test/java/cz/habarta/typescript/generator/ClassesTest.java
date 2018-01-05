
package cz.habarta.typescript.generator;

import java.util.*;
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


    @Test
    public void testClassPatterns1() {
        testClassPatterns(
                Arrays.asList(
                        "**Bc",
                        "**Bi",
                        "**Derived1",
                        "**Derived2"
                ),
                ""
                + "class Bc {\n"
                + "    x: string;\n"
                + "}\n"
                + "\n"
                + "interface Bi {\n"
                + "    y: string;\n"
                + "}\n"
                + "\n"
                + "class Derived1 extends Bc implements Bi {\n"
                + "    y: string;\n"
                + "}\n"
                + "\n"
                + "class Derived2 extends Derived1 {\n"
                + "}"
        );
    }

    @Test
    public void testClassPatterns2() {
        testClassPatterns(
                Arrays.asList(
                        "**Derived1",
                        "**Derived2"
                ),
                ""
                + "interface Bc {\n"
                + "    x: string;\n"
                + "}\n"
                + "\n"
                + "interface Bi {\n"
                + "    y: string;\n"
                + "}\n"
                + "\n"
                + "class Derived1 implements Bc, Bi {\n"
                + "    x: string;\n"
                + "    y: string;\n"
                + "}\n"
                + "\n"
                + "class Derived2 extends Derived1 {\n"
                + "}"
        );
    }

    @Test
    public void testClassPatterns3() {
        testClassPatterns(
                Arrays.asList(
                        "**Bc",
                        "**Derived2"
                ),
                ""
                + "class Bc {\n"
                + "    x: string;\n"
                + "}\n"
                + "\n"
                + "interface Bi {\n"
                + "    y: string;\n"
                + "}\n"
                + "\n"
                + "interface Derived1 extends Bc, Bi {\n"
                + "}\n"
                + "\n"
                + "class Derived2 implements Derived1 {\n"
                + "    x: string;\n"
                + "    y: string;\n"
                + "}"
        );
    }

    @Test
    public void testClassPatterns4() {
        testClassPatterns(
                Arrays.asList(
                        "**Bc",
                        "**Derived1"
                ),
                ""
                + "class Bc {\n"
                + "    x: string;\n"
                + "}\n"
                + "\n"
                + "interface Bi {\n"
                + "    y: string;\n"
                + "}\n"
                + "\n"
                + "class Derived1 extends Bc implements Bi {\n"
                + "    y: string;\n"
                + "}\n"
                + "\n"
                + "interface Derived2 extends Derived1 {\n"
                + "}"
        );
    }

    private static void testClassPatterns(List<String> mapClassesAsClassesPatterns, String expected) {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.mapClasses = ClassMapping.asClasses;
        settings.mapClassesAsClassesPatterns = mapClassesAsClassesPatterns;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Bc.class, Bi.class, Derived1.class, Derived2.class));
        Assert.assertEquals(expected.replace('\'', '"').trim(), output.trim());
    }

    private static abstract class Bc {
        public abstract String getX();
    }

    private static abstract interface Bi {
        public abstract String getY();
    }

    private static abstract class Derived1 extends Bc implements Bi {
    }

    private static abstract class Derived2 extends Derived1 {
    }

}
