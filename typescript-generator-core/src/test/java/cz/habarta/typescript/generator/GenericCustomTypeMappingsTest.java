
package cz.habarta.typescript.generator;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Test;


public class GenericCustomTypeMappingsTest {

    @Test
    public void testListWrapper() {
        final Settings settings = TestUtils.settings();
        settings.customTypeNaming = Collections.singletonMap(ListWrapper1.class.getName(), "ListWrapper");
        settings.customTypeMappings = Collections.singletonMap(ListWrapper2.class.getName(), "ListWrapper");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Class1.class));
        Assert.assertTrue(output.contains("list1: ListWrapper<string>"));
        Assert.assertTrue(output.contains("list2: ListWrapper<number>"));
    }

    private static class Class1 {
        public ListWrapper1<String> list1;
        public ListWrapper2<Number> list2;
    }

    private static class ListWrapper1<T> {
        public List<T> values;
    }

    private static class ListWrapper2<T> {
        public List<T> values;
    }

    @Test
    public void testMap() {
        final Settings settings = TestUtils.settings();
        settings.customTypeMappings = Collections.singletonMap("java.util.Map", "Map");
        settings.mapDate = DateMapping.asString;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Class2.class));
        Assert.assertTrue(output.contains("someMap: Map<string, any>"));
        Assert.assertTrue(output.contains("dateMap: Map<string, DateAsString>"));
    }

    private static class Class2 {
        public Map<String, Object> someMap;
        public Map<String, Date> dateMap;
    }

}
