
package cz.habarta.typescript.generator;

import java.lang.reflect.*;
import java.util.*;
import org.junit.*;


public class ModelCompilerTest {

    @Test
    public void testEnum() throws Exception {
        final Settings settings = getTestSettings();
        final Type javaType = A.class.getField("directions").getGenericType();
        Assert.assertEquals("{ [index: string]: Direction }[]", TestUtils.compileType(settings, javaType).toString());
    }

    @Test
    public void testDate() throws Exception {
        final Settings settings = getTestSettings();
        final Type javaType = A.class.getField("timestamp").getGenericType();
        Assert.assertEquals("DateAsString", TestUtils.compileType(settings, javaType).toString());
    }

    @Test
    public void testExclusion() throws Exception {
        final Settings settings = getTestSettings(Direction.class.getName());
        final Type javaType = A.class.getField("directions").getGenericType();
        Assert.assertEquals("{ [index: string]: any }[]", TestUtils.compileType(settings, javaType).toString());
    }

    @Test
    public void testExclusionPattern() throws Exception {
        final Settings settings = TestUtils.settings();
        settings.setExcludeFilter(null, Arrays.asList("**Direction"));
        final Type javaType = A.class.getField("directions").getGenericType();
        Assert.assertEquals("{ [index: string]: any }[]", TestUtils.compileType(settings, javaType).toString());
    }

    private static Settings getTestSettings(String... excludedClassNames) {
        final Settings settings = TestUtils.settings();
        settings.mapDate = DateMapping.asString;
        settings.setExcludeFilter(Arrays.asList(excludedClassNames), null);
        return settings;
    }

    private static enum Direction {
        North, East, South, West
    }

    private static class A {
        public List<Map<String, Direction>> directions;
        public Date timestamp;
    }

}
