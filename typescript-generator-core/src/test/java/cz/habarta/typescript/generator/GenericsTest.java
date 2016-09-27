package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import java.io.StringWriter;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.assertEquals;


public class GenericsTest {

    @Test
    public void testDefaultGenerics() throws Exception {
        final Settings settings = TestUtils.settings();
        settings.customTypeProcessor = new DefaultTypeProcessor();

        final StringWriter stringWriter = new StringWriter();
        new TypeScriptGenerator(settings).generateTypeScript(Input.from(A.class), Output.to(stringWriter));
        final String actual = stringWriter.toString().trim();
        final String nl = settings.newline;
        final String expected =
                "interface A {" + nl +
                "    x: A;" + nl +
                "    y: A;" + nl +
                "    z: A;" + nl +
                "}";
        assertEquals(expected, actual);
    }

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
        settings.customTypeProcessor = new GenericsTypeProcessor();

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
        settings.customTypeProcessor = new GenericsTypeProcessor();
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
                "export interface E extends D<string> {" + nl +
                "}";
        assertEquals(expected, actual);
    }

    @Test
    public void testImplements() {
        final Settings settings = TestUtils.settings();
        settings.customTypeProcessor = new GenericsTypeProcessor();
        settings.sortDeclarations = true;
        settings.excludedClassNames = Arrays.asList(Comparable.class.getName());

        final StringWriter stringWriter = new StringWriter();
        new TypeScriptGenerator(settings).generateEmbeddableTypeScript(Input.from(IA.class), Output.to(stringWriter), true, 0);
        final String actual = stringWriter.toString().trim();
        final String nl = settings.newline;
        final String expected =
                "export interface IA extends IB<string> {" + nl +
                "    type: \"GenericsTest$IA\";" + nl +
                "}" + nl +
                "" + nl +
                "export interface IB<T> {" + nl +
                "    type: string;" + nl +
                "    x: T;" + nl +
                "}";
        assertEquals(expected, actual);
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

    class E extends D<String> {
    }

    abstract class IA implements IB<String>, Comparable<IA> {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type", include = As.PROPERTY, visible = false)
    interface IB<T> {
        public T getX();
    }
}
