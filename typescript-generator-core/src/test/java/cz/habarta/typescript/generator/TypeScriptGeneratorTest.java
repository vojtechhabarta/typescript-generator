
package cz.habarta.typescript.generator;

import java.io.File;
import java.util.*;
import org.junit.Test;


public class TypeScriptGeneratorTest {

    @Test
    public void testNamespacesAndModules() {
        TypeScriptGenerator.generateTypeScript(Arrays.asList(DummyBean.class), settings("NS1", "mod1"), new File("target/test-nm1.d.ts"));
        TypeScriptGenerator.generateTypeScript(Arrays.asList(DummyBean.class), settings("NS2", null), new File("target/test-nm2.d.ts"));
        TypeScriptGenerator.generateTypeScript(Arrays.asList(DummyBean.class), settings(null, "mod3"), new File("target/test-nm3.d.ts"));
        TypeScriptGenerator.generateTypeScript(Arrays.asList(DummyBean.class), settings(null, null), new File("target/test-nm4.d.ts"));
    }

    private static Settings settings(String namespace, String module) {
        final Settings settings = new Settings();
        settings.jsonLibrary = JsonLibrary.jackson2;
        settings.namespace = namespace;
        settings.module = module;
        return settings;
    }

}
