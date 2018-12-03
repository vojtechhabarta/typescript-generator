
package cz.habarta.typescript.generator;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;


public class ModuleDependenciesTest {

    @Test
    public void test() {
        generateModuleA();
        generateModuleB();
    }

    private void generateModuleA() {
        final Settings settings = TestUtils.settings();
        settings.outputKind = TypeScriptOutputKind.module;
        settings.customTypeNaming = Collections.singletonMap("cz.habarta.typescript.generator.ModuleDependenciesTest$A2", "NS.A2");
        settings.generateNpmPackageJson = true;
        settings.npmName = "a";
        settings.npmVersion = "1.0.0";
        settings.generateInfoJson = true;
        new TypeScriptGenerator(settings).generateTypeScript(
                Input.from(A1.class, A2.class, Enum1.class),
                Output.to(new File("target/test-module-dependencies/a/a.d.ts")));
        final String output = TestUtils.readFile("target/test-module-dependencies/a/a.d.ts");
        Assert.assertTrue(output.contains("interface A1"));
        Assert.assertTrue(output.contains("namespace NS"));
        Assert.assertTrue(output.contains("interface A2"));
        Assert.assertTrue(output.contains("type Enum1"));
    }

    private void generateModuleB() {
        final Settings settings = TestUtils.settings();
        settings.outputKind = TypeScriptOutputKind.module;
        settings.generateNpmPackageJson = true;
        settings.npmName = "b";
        settings.npmVersion = "1.0.0";
        settings.moduleDependencies = Arrays.asList(
                new ModuleDependency("../a", "a", new File("target/test-module-dependencies/a/typescript-generator-info.json"), "a", "1.0.0")
        );
        new TypeScriptGenerator(settings).generateTypeScript(
                Input.from(B1.class, B2.class, C.class, D1.class, D2.class),
                Output.to(new File("target/test-module-dependencies/b/b.d.ts")));
        final String output = TestUtils.readFile("target/test-module-dependencies/b/b.d.ts");
        Assert.assertTrue(output.contains("import * as a from \"../a\""));
        Assert.assertTrue(output.contains("interface B1 extends a.A1"));
        Assert.assertTrue(output.contains("objectA: a.A1"));
        Assert.assertTrue(output.contains("enum1: a.Enum1"));
        Assert.assertTrue(output.contains("interface B2 extends a.NS.A2"));
        Assert.assertTrue(output.contains("objectA: a.NS.A2"));
        Assert.assertTrue(output.contains("interface D1 extends C<a.A1>"));
        Assert.assertTrue(output.contains("interface D2 extends C<a.NS.A2>"));
    }

    /// module "a"

    private static class A1 {
        public String a;
    }

    private static class A2 {
        public String a;
    }

    private static enum Enum1 {
        c1, c2
    }

    /// module "b"

    private static class B1 extends A1 {
        public String b;
        public A1 objectA;
        public Enum1 enum1;
    }

    private static class B2 extends A2 {
        public String b;
        public A2 objectA;
    }

    private static class C<T> {
        public T c;
    }

    private static class D1 extends C<A1> {
        public String d;
    }

    private static class D2 extends C<A2> {
        public String d;
    }

}
