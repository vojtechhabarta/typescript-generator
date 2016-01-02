
package cz.habarta.typescript.generator;

import java.lang.reflect.*;
import java.util.*;
import org.junit.*;


public class ModelCompilerTest {

    @Test
    public void testEnum() throws Exception {
        final ModelCompiler modelCompiler = getTestModelCompiler();
        final Type javaType = A.class.getField("directions").getGenericType();
        Assert.assertEquals("{ [index: string]: Direction }[]", modelCompiler.typeFromJava(javaType).toString());
        Assert.assertEquals("{ [index: string]: string }[]", modelCompiler.typeFromJavaWithReplacement(javaType).toString());
    }

    @Test
    public void testDate() throws Exception {
        final ModelCompiler modelCompiler = getTestModelCompiler();
        final Type javaType = A.class.getField("timestamp").getGenericType();
        Assert.assertEquals("Date", modelCompiler.typeFromJava(javaType).toString());
        Assert.assertEquals("DateAsString", modelCompiler.typeFromJavaWithReplacement(javaType).toString());
    }

    @Test
    public void testExclusion() throws Exception {
        final ModelCompiler modelCompiler = getTestModelCompiler(Direction.class.getName());
        final Type javaType = A.class.getField("directions").getGenericType();
        Assert.assertEquals("{ [index: string]: any }[]", modelCompiler.typeFromJava(javaType).toString());
    }

    private static ModelCompiler getTestModelCompiler(String... excludedClassNames) {
        final Settings settings = new Settings();
        settings.mapDate = DateMapping.asString;
        settings.excludedClassNames = Arrays.asList(excludedClassNames);
        return new TypeScriptGenerator(settings).getModelCompiler();
    }

    private static enum Direction {
        North, East, South, West
    }

    private static class A {
        public List<Map<String, Direction>> directions;
        public Date timestamp;
    }

}
