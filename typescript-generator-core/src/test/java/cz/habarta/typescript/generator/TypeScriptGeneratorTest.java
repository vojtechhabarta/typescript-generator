
package cz.habarta.typescript.generator;

import java.io.File;
import java.util.*;
import org.junit.Test;


public class TypeScriptGeneratorTest {

    @Test
    public void test() {
        TypeScriptGenerator.generateTypeScript(Arrays.asList(DummyBean.class), new Settings(), new File("target/testNoModule.d.ts"));

        final Settings settings = new Settings();
        settings.moduleName = "Test";
        TypeScriptGenerator.generateTypeScript(Arrays.asList(DummyBean.class), settings, new File("target/testWithModule.d.ts"));
    }

}
