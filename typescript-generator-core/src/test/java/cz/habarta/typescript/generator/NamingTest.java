
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.compiler.SymbolTable;
import cz.habarta.typescript.generator.yield.KeywordInPackage;
import java.util.Collections;
import java.util.LinkedHashMap;
import org.junit.Assert;
import org.junit.Test;


public class NamingTest {

    @Test(expected = SymbolTable.NameConflictException.class)
    public void testConflictReport() {
        final Settings settings = TestUtils.settings();
        new TypeScriptGenerator(settings).generateTypeScript(Input.from(A.ConflictingClass.class, B.ConflictingClass.class));
    }

    @Test
    public void testConflictResolved() {
        final Settings settings = TestUtils.settings();
        settings.customTypeNaming = new LinkedHashMap<>();
        settings.customTypeNaming.put("cz.habarta.typescript.generator.NamingTest$A$ConflictingClass", "A$ConflictingClass");
        settings.customTypeNaming.put("cz.habarta.typescript.generator.NamingTest$B$ConflictingClass", "B$ConflictingClass");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(A.ConflictingClass.class, B.ConflictingClass.class));
        Assert.assertTrue(output.contains("A$ConflictingClass"));
        Assert.assertTrue(output.contains("B$ConflictingClass"));
    }

    @Test
    public void testConflictPrevented() {
        final Settings settings = TestUtils.settings();
        settings.mapPackagesToNamespaces = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(A.ConflictingClass.class, B.ConflictingClass.class));
        Assert.assertTrue(output.contains("namespace cz.habarta.typescript.generator.NamingTest.A {"));
        Assert.assertTrue(output.contains("namespace cz.habarta.typescript.generator.NamingTest.B {"));
    }

    private static class A {
        private static class ConflictingClass {
            public String conflictingProperty;
        }
    }

    private static class B {
        private static class ConflictingClass {
            public String conflictingProperty;
        }
    }

    @Test
    public void testTypeNamingFunction() {
        final Settings settings = TestUtils.settings();
        settings.customTypeNamingFunction = "function(name, simpleName) { if (name.indexOf('cz.') === 0) return 'Test' + simpleName; }";
        final SymbolTable symbolTable = new SymbolTable(settings);
        final String name = symbolTable.getMappedNamespacedName(A.class);
        Assert.assertEquals("TestA", name);
    }

    @Test
    public void testTypeNamingFunctionReturnsUndefined() {
        final Settings settings = TestUtils.settings();
        settings.customTypeNamingFunction = "function() {}";
        final SymbolTable symbolTable = new SymbolTable(settings);
        final String name = symbolTable.getMappedNamespacedName(A.class);
        Assert.assertEquals("A", name);
    }

    @Test
    public void testCombinations() {
        final Settings settings = TestUtils.settings();
        settings.customTypeNamingFunction = "function(name, simpleName) { if (name.indexOf('cz.') === 0) return 'Func' + simpleName; }";
        settings.addTypeNamePrefix = "Conf";
        settings.mapPackagesToNamespaces = true;
        final SymbolTable symbolTable = new SymbolTable(settings);
        Assert.assertEquals("FuncA", symbolTable.getMappedNamespacedName(A.class));
        Assert.assertEquals("java.lang.ConfObject", symbolTable.getMappedNamespacedName(Object.class));
    }

    @Test
    public void testTypeScriptKeywords() {
        final Settings settings = TestUtils.settings();
        settings.mapPackagesToNamespaces = true;
        final SymbolTable symbolTable = new SymbolTable(settings);
        final String name = symbolTable.getMappedNamespacedName(KeywordInPackage.class);
        Assert.assertEquals("cz.habarta.typescript.generator._yield.KeywordInPackage", name);
    }

    @Test
    public void testNamespaced() {
        final Settings settings = TestUtils.settings();
        settings.customTypeNaming = Collections.singletonMap("cz.habarta.typescript.generator.NamingTest$C", "NS.C");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(C.class, D.class));
        Assert.assertTrue(output.contains("namespace NS"));
        Assert.assertTrue(output.contains("interface C"));
        Assert.assertTrue(output.contains("interface D extends NS.C"));
        Assert.assertTrue(output.contains("objectC: NS.C"));
    }

    private static class C {
        public String c;
    }

    private static class D extends C {
        public String d;
        public C objectC;
    }

}
