
package cz.habarta.typescript.generator;

import java.util.Collections;
import java.util.List;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class CustomPackageMappingTest {

    @Test
    public void testSimple() {
        final Settings settings = createCustomPackageMappingSettings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ListWrapper1.class));

        assertTrue(output.matches("(?s).*declare namespace typescript-generator.CustomPackageMappingTest\\s+\\{" +
                                          "\\s+export interface ListWrapper1<.*"));

        settings.customPackageMappings.clear();
        settings.customPackageMappings.put("cz.habarta.typescript", "typescript");
        final String output2 = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ListWrapper1.class));

        assertTrue(output2.contains("declare namespace typescript.generator.CustomPackageMappingTest"));
    }

    @Test
    public void testRemapPackageToNameWithKeywords() {
        final Settings settings = createCustomPackageMappingSettings();
        settings.customPackageMappings.put("cz.habarta.typescript.generator", "typescript-generator.yield.for.me");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ListWrapper1.class));

        assertTrue(output.contains("declare namespace typescript-generator._yield._for.me.CustomPackageMappingTest"));
    }

    private Settings createCustomPackageMappingSettings() {
        final Settings settings = TestUtils.settings();
        settings.mapPackagesToNamespaces = true;
        settings.customPackageMappings.put("cz.habarta.type", "selectmenot"); // complete package match is required
        settings.customPackageMappings.put("cz.habarta.typescript.generator", "typescript-generator");
        return settings;
    }

    @Test
    public void testCustomPackageMappingAndCustomTypeMapping() {
        final Settings settings = createCustomPackageMappingSettings();
        settings.customTypeNaming = Collections.singletonMap(ListWrapper1.class.getName(), "ListWrapper");
        settings.customTypeMappings = Collections.singletonMap(ListWrapper2.class.getName(), "ListWrapper");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Class1.class));

        assertTrue(output.contains("declare namespace typescript-generator.CustomPackageMappingTest"));
        assertTrue(output.contains("list1: ListWrapper<string>"));
        assertTrue(output.contains("list2: ListWrapper<number>"));
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
}
