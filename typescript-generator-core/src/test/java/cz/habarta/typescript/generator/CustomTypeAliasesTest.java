
package cz.habarta.typescript.generator;

import java.util.Collections;
import org.junit.Assert;
import org.junit.Test;


public class CustomTypeAliasesTest {

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        settings.outputKind = TypeScriptOutputKind.module;
        settings.customTypeAliases = Collections.singletonMap("Id<T>", "string");
        settings.customTypeMappings = Collections.singletonMap(IdRepresentation.class.getName(), "Id");
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

}
