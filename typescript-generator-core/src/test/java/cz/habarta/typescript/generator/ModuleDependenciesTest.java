
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


@SuppressWarnings("unused")
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
                Input.from(A1.class, A2.class, Enum1.class, ABase.class),
                Output.to(new File("target/test-module-dependencies/a/a.d.ts")));
        final String output = TestUtils.readFile("target/test-module-dependencies/a/a.d.ts");
        Assertions.assertTrue(output.contains("interface A1 {"));
        Assertions.assertTrue(output.contains("namespace NS {"));
        Assertions.assertTrue(output.contains("interface A2 {"));
        Assertions.assertTrue(output.contains("type Enum1 ="));
    }

    private void generateModuleB() {
        final Settings settings = TestUtils.settings();
        settings.outputKind = TypeScriptOutputKind.module;
        settings.generateNpmPackageJson = true;
        settings.npmName = "b";
        settings.npmVersion = "1.0.0";
        settings.moduleDependencies = Arrays.asList(
                ModuleDependency.module("../a", "a", new File("target/test-module-dependencies/a/typescript-generator-info.json"), "a", "1.0.0")
        );
        new TypeScriptGenerator(settings).generateTypeScript(
                Input.from(B1.class, B2.class, C.class, D1.class, D2.class),
                Output.to(new File("target/test-module-dependencies/b/b.d.ts")));
        final String output = TestUtils.readFile("target/test-module-dependencies/b/b.d.ts");
        Assertions.assertTrue(output.contains("import * as a from \"../a\""));
        Assertions.assertTrue(output.contains("interface B1 extends a.A1 {"));
        Assertions.assertTrue(output.contains("objectA: a.A1;"));
        Assertions.assertTrue(output.contains("enum1: a.Enum1;"));
        Assertions.assertTrue(output.contains("aBase: a.ABaseUnion<string>;"));
        Assertions.assertTrue(output.contains("aBases: a.ABaseUnion<string>[];"));
        Assertions.assertTrue(output.contains("interface B2 extends a.NS.A2 {"));
        Assertions.assertTrue(output.contains("objectA: a.NS.A2;"));
        Assertions.assertTrue(output.contains("interface D1 extends C<a.A1> {"));
        Assertions.assertTrue(output.contains("interface D2 extends C<a.NS.A2> {"));
        Assertions.assertTrue(!output.contains("interface A1 {"));
        Assertions.assertTrue(!output.contains("namespace NS {"));
        Assertions.assertTrue(!output.contains("interface A2 {"));
        Assertions.assertTrue(!output.contains("type Enum1 ="));
    }

    @Test
    public void testGlobal() {
        generateGlobalA("global-a");
        generateGlobalB("global-b", "global-a");
    }

    @Test
    public void testGlobalWithConflict() {
        generateGlobalA("global-a1");
        generateGlobalA("global-a2");

        RuntimeException e = Assertions.assertThrows(RuntimeException.class, () -> generateGlobalB("global-b-conflict", "global-a1", "global-a2"));
        System.out.println("Exception (expected): " + e.getMessage());
    }

    private void generateGlobalA(String directory) {
        final Settings settings = TestUtils.settings();
        settings.outputKind = TypeScriptOutputKind.global;
        settings.customTypeNaming = Collections.singletonMap("cz.habarta.typescript.generator.ModuleDependenciesTest$A2", "NS.A2");
        settings.generateInfoJson = true;
        new TypeScriptGenerator(settings).generateTypeScript(
                Input.from(A1.class, A2.class, Enum1.class, ABase.class),
                Output.to(new File("target/test-module-dependencies/" + directory + "/global.d.ts")));
        final String output = TestUtils.readFile("target/test-module-dependencies/" + directory + "/global.d.ts");
        Assertions.assertTrue(output.contains("interface A1 {"));
        Assertions.assertTrue(output.contains("namespace NS {"));
        Assertions.assertTrue(output.contains("interface A2 {"));
        Assertions.assertTrue(output.contains("type Enum1 ="));
    }

    private void generateGlobalB(String directory, String... dependencyDirectories) {
        final Settings settings = TestUtils.settings();
        settings.outputKind = TypeScriptOutputKind.global;
        settings.referencedFiles = Stream.of(dependencyDirectories)
                .map(depDir -> "../" + depDir + "/global.d.ts")
                .collect(Collectors.toList());
        settings.moduleDependencies = Stream.of(dependencyDirectories)
                .map(depDir -> ModuleDependency.global(new File("target/test-module-dependencies/" + depDir + "/typescript-generator-info.json")))
                .collect(Collectors.toList());
        new TypeScriptGenerator(settings).generateTypeScript(
                Input.from(B1.class, B2.class, C.class, D1.class, D2.class),
                Output.to(new File("target/test-module-dependencies/" + directory + "/global.d.ts")));
        final String output = TestUtils.readFile("target/test-module-dependencies/" + directory + "/global.d.ts");
        Assertions.assertTrue(!output.contains("import"));
        Assertions.assertTrue(output.contains("interface B1 extends A1 {"));
        Assertions.assertTrue(output.contains("objectA: A1;"));
        Assertions.assertTrue(output.contains("enum1: Enum1;"));
        Assertions.assertTrue(output.contains("aBase: ABaseUnion<string>;"));
        Assertions.assertTrue(output.contains("aBases: ABaseUnion<string>[];"));
        Assertions.assertTrue(output.contains("interface B2 extends NS.A2 {"));
        Assertions.assertTrue(output.contains("objectA: NS.A2;"));
        Assertions.assertTrue(output.contains("interface D1 extends C<A1> {"));
        Assertions.assertTrue(output.contains("interface D2 extends C<NS.A2> {"));
        Assertions.assertTrue(!output.contains("interface A1 {"));
        Assertions.assertTrue(!output.contains("namespace NS {"));
        Assertions.assertTrue(!output.contains("interface A2 {"));
        Assertions.assertTrue(!output.contains("type Enum1 ="));
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

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME)
    @JsonSubTypes({
        @JsonSubTypes.Type(name = "ADerived1", value = ADerived1.class),
        @JsonSubTypes.Type(name = "ADerived2", value = ADerived2.class),
    })
    private static abstract class ABase<T> {
    }

    private static class ADerived1<T> extends ABase<T> {
    }

    private static class ADerived2<T> extends ABase<T> {
    }

    /// module "b"

    private static class B1 extends A1 {
        public String b;
        public A1 objectA;
        public Enum1 enum1;
        public ABase<String> aBase;
        public List<ABase<String>> aBases;
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
