package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.Input;
import cz.habarta.typescript.generator.JsonLibrary;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TestUtils;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import javax.json.bind.annotation.JsonbProperty;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class JsonbParserTest {

    private Settings settings;

    @Before
    public void before() {
        settings = TestUtils.settings();
        settings.jsonLibrary = JsonLibrary.jsonb;
    }

    private static class OverridenPropertyName {
        @JsonbProperty("$foo")
        int foo;
    }

    public static class DirectName {
        public int foo;
    }

    @Test
    public void tesJsonbProperty() {
        final String output = generate(settings, OverridenPropertyName.class);
        Assert.assertTrue(output, output.contains(" $foo"));
    }
    @Test
    public void tesImplicitName() {
        final String output = generate(settings, DirectName.class);
        Assert.assertTrue(output, output.contains(" foo"));
    }

    private String generate(final Settings settings, Class<?> cls) {
        return new TypeScriptGenerator(settings).generateTypeScript(Input.from(cls));
    }
}
