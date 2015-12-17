
package cz.habarta.typescript.generator;

import java.io.File;
import org.junit.Test;


public class TypeScriptGeneratorTest {

    @Test
    public void testNamespacesAndModules() {
        new TypeScriptGenerator(settings("NS1", "mod1")).generateTypeScript(Input.from(DummyBean.class), new File("target/test-nm1.d.ts"));
        new TypeScriptGenerator(settings("NS2", null)).generateTypeScript(Input.from(DummyBean.class), new File("target/test-nm2.d.ts"));
        new TypeScriptGenerator(settings(null, "mod3")).generateTypeScript(Input.from(DummyBean.class), new File("target/test-nm3.d.ts"));
        new TypeScriptGenerator(settings(null, null)).generateTypeScript(Input.from(DummyBean.class), new File("target/test-nm4.d.ts"));
    }

    private static Settings settings(String namespace, String module) {
        final Settings settings = new Settings();
        settings.jsonLibrary = JsonLibrary.jackson2;
        settings.namespace = namespace;
        settings.module = module;
        return settings;
    }

}
