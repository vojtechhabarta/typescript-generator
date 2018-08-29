package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import java.io.StringWriter;
import java.util.*;
import static org.junit.Assert.assertEquals;
import org.junit.Test;


public class GenericsTest {

    @Test
    public void testAdvancedGenerics() throws Exception {
        final Settings settings = TestUtils.settings();
        settings.addTypeNamePrefix = "I";

        final StringWriter stringWriter = new StringWriter();
        new TypeScriptGenerator(settings).generateEmbeddableTypeScript(Input.from(A.class), Output.to(stringWriter), true, 0);
        final String actual = stringWriter.toString().trim();
        final String nl = settings.newline;
        final String expected =
                "export interface IA<U, V> {" + nl +
                "    x: IA<string, string>;" + nl +
                "    y: IA<IA<string, IB>, string[]>;" + nl +
                "    z: IA<{ [index: string]: V }, number[]>;" + nl +
                "}" + nl +
                "" + nl +
                "export interface IB {" + nl +
                "}";
        assertEquals(expected, actual);
        assertEquals("IA<string, string>", TestUtils.compileType(settings, A.class.getField("x").getGenericType()).toString());
        assertEquals("IA<IA<string, IB>, string[]>", TestUtils.compileType(settings, A.class.getField("y").getGenericType()).toString());
        assertEquals("IA<{ [index: string]: V }, number[]>", TestUtils.compileType(settings, A.class.getField("z").getGenericType()).toString());
    }

    @Test
    public void testWildcardGeneric() {
        final Settings settings = TestUtils.settings();
        settings.addTypeNamePrefix = "I";

        final StringWriter stringWriter = new StringWriter();
        new TypeScriptGenerator(settings).generateEmbeddableTypeScript(Input.from(C.class), Output.to(stringWriter), true, 0);
        final String actual = stringWriter.toString().trim();
        final String nl = settings.newline;
        final String expected =
                "export interface IC {" + nl +
                "    x: string[];" + nl +
                "}";
        assertEquals(expected, actual);
    }

    @Test
    public void testNonGenericExtends() {
        final Settings settings = TestUtils.settings();
        settings.sortDeclarations = true;

        final StringWriter stringWriter = new StringWriter();
        new TypeScriptGenerator(settings).generateEmbeddableTypeScript(Input.from(E.class), Output.to(stringWriter), true, 0);
        final String actual = stringWriter.toString().trim();
        final String nl = settings.newline;
        final String expected =
                "export interface D<T> {" + nl +
                "    x: T;" + nl +
                "}" + nl +
                "" + nl +
                "export interface E extends D<F> {" + nl +
                "}" + nl +
                "" + nl +
                "export interface F {" + nl +
                "}";
        assertEquals(expected, actual);
    }

    @Test
    public void testImplements() {
        final Settings settings = TestUtils.settings();
        settings.sortDeclarations = true;
        settings.setExcludeFilter(Arrays.asList(Comparable.class.getName()), null);

        final StringWriter stringWriter = new StringWriter();
        new TypeScriptGenerator(settings).generateEmbeddableTypeScript(Input.from(IA.class), Output.to(stringWriter), true, 0);
        final String actual = stringWriter.toString().trim();
        final String nl = settings.newline;
        final String expected =
                "export interface IA extends IB<string> {" + nl +
                "    type: string;" + nl +
                "}" + nl +
                "" + nl +
                "export interface IB<T> {" + nl +
                "    type: string;" + nl +
                "    x: T;" + nl +
                "}";

        assertEquals(expected, actual);
    }

    @Test
    public void testGenericsWithoutTypeArgument() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Table.class, Page1.class, Page2.class));
        final String expected =
                "interface Table<T> {\n" +
                "    rows: T[];\n" +
                "}\n" +
                "\n" +
                "interface Page1 {\n" +
                "    stringTable: Table<string>;\n" +
                "}\n" +
                "\n" +
                "interface Page2 {\n" +
                "    someTable: Table<any>;\n" +
                "}";
        assertEquals(expected, output.trim());
    }

    @Test
    public void testGenericArray() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(TableGA.class));
        final String expected =
                "interface TableGA<T> {\n" +
                "    rows: T[];\n" +
                "}";
        assertEquals(expected, output.trim());
    }

    @Test
    public void testArbitraryGenericParameter() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ExecutionResult.class));
        final String expected =
                "interface ExecutionResult {\n" +
                "    data: number;\n" +
                "}";
        assertEquals(expected, output.trim());
    }

    class A<U,V> {
        public A<String, String> x;
        public A<A<String, B>, List<String>> y;
        public A<Map<String, V>, Set<Integer>> z;
    }

    class B {
    }

    class C {
        public List<? extends String> x;
    }

    class D<T> {
        public T x;
    }

    class E extends D<F> {
    }

    class F {
    }

    abstract class IA implements IB<String>, Comparable<IA> {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = As.PROPERTY, visible = false)
    interface IB<T> {
        public T getX();
    }

    class Table<T> {
        public List<T> rows;
    }

    class Page1 {
        public Table<String> stringTable;
    }

    class Page2 {
        @SuppressWarnings("rawtypes")
        public Table someTable;
    }

    class TableGA<T> {
        public T[] rows;
    }

    interface ExecutionResult {
        public <T extends Number> T getData();
    }

}
