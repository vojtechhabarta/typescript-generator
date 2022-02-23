
package cz.habarta.typescript.generator;

import java.util.Map;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class MapTest {

    public static class ClassWithMap {
        public Map<String, Person> people;
    }

    @Test
    public void testDefault() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithMap.class));
        Assertions.assertTrue(output.contains("people: { [index: string]: Person }"));
    }

    @Test
    public void testIndexedArray() {
        final Settings settings = TestUtils.settings();
        settings.mapMap = MapMapping.asIndexedArray;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithMap.class));
        Assertions.assertTrue(output.contains("people: { [index: string]: Person }"));
    }

    @Test
    public void testRecord() {
        final Settings settings = TestUtils.settings();
        settings.mapMap = MapMapping.asRecord;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithMap.class));
        Assertions.assertTrue(output.contains("people: Record<string, Person>"));
    }

}
