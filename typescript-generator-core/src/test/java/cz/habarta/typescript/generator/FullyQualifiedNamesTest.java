
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.p2.D;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


@SuppressWarnings("unused")
public class FullyQualifiedNamesTest {

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.mapClasses = ClassMapping.asClasses;
        settings.mapPackagesToNamespaces = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(D.class));
        final String expected = ""
                + "namespace cz.habarta.typescript.generator.p2 {\n"
                + "\n"
                + "    export class D {\n"
                + "        a: cz.habarta.typescript.generator.p1.A;\n"
                + "        b: cz.habarta.typescript.generator.p2.B;\n"
                + "        c: cz.habarta.typescript.generator.p1.C;\n"
                + "        e: cz.habarta.typescript.generator.p1.E;\n"
                + "    }\n"
                + "\n"
                + "}\n"
                + "\n"
                + "namespace cz.habarta.typescript.generator.p1 {\n"
                + "\n"
                + "    export class A {\n"
                + "        sa: string;\n"
                + "    }\n"
                + "\n"
                + "}\n"
                + "\n"
                + "namespace cz.habarta.typescript.generator.p2 {\n"
                + "\n"
                + "    export class B extends cz.habarta.typescript.generator.p1.A {\n"
                + "        sb: string;\n"
                + "    }\n"
                + "\n"
                + "}\n"
                + "\n"
                + "namespace cz.habarta.typescript.generator.p1 {\n"
                + "\n"
                + "    export class C extends cz.habarta.typescript.generator.p2.B {\n"
                + "        sc: string;\n"
                + "    }\n"
                + "\n"
                + "}\n"
                + "\n"
                + "namespace cz.habarta.typescript.generator.p1 {\n"
                + "\n"
                + "    export type E = \"Left\" | \"Right\";\n"
                + "\n"
                + "}";
        Assertions.assertEquals(expected.trim(), output.trim());
    }

    @Test
    public void testNested() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.mapClasses = ClassMapping.asClasses;
        settings.mapPackagesToNamespaces = true;
        settings.sortTypeDeclarations = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Outer.Inner.class, Outer.class));
        final String expected = ""
                + "namespace cz.habarta.typescript.generator.FullyQualifiedNamesTest {\n"
                + "\n"
                + "    export class Outer {\n"
                + "        outer: string;\n"
                + "    }\n"
                + "\n"
                + "}\n"
                + "\n"
                + "namespace cz.habarta.typescript.generator.FullyQualifiedNamesTest.Outer {\n"
                + "\n"
                + "    export class Inner {\n"
                + "        inner: string;\n"
                + "    }\n"
                + "\n"
                + "}\n";
        Assertions.assertEquals(expected.trim(), output.trim());
    }

    private static class Outer {
        public String outer;
        private static class Inner {
            public String inner;
        }
    }

}
