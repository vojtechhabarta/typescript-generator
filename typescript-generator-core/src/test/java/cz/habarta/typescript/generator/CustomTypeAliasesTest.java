
package cz.habarta.typescript.generator;

import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;


@SuppressWarnings("unused")
public class CustomTypeAliasesTest {

    @Test
    public void testGeneric() {
        final Settings settings = TestUtils.settings();
        settings.outputKind = TypeScriptOutputKind.module;
        settings.customTypeAliases = Collections.singletonMap("Id<T>", "string");
        settings.customTypeMappings = Collections.singletonMap("cz.habarta.typescript.generator.CustomTypeAliasesTest$IdRepresentation<T>", "Id<T>");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(MyEntityRepresentation.class));
        Assert.assertTrue(output.contains("id: Id<MyEntityRepresentation>"));
        Assert.assertTrue(output.contains("export type Id<T> = string"));
    }

    private static class MyEntityRepresentation {
        public IdRepresentation<MyEntityRepresentation> id;
    }

    private static class IdRepresentation<T> {
        public String id;
    }

    @Test
    public void testNonGeneric() {
        final Settings settings = TestUtils.settings();
        settings.customTypeAliases = Collections.singletonMap("Id", "string");
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from());
        Assert.assertTrue(output.contains("type Id = string"));
    }

}
