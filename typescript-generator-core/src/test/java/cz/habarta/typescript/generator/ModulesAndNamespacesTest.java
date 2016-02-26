
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.emitter.EmitterExtension;
import cz.habarta.typescript.generator.emitter.TsModel;
import java.io.File;
import org.junit.Test;

// run `tsc` compiler from `src/test/ts` directory after this test
public class ModulesAndNamespacesTest {

    @Test
    public void testNamespacesAndModules() {
        final File withoutModuleDir = new File("target/test-ts-withoutmodule");
        final File withModuleDir = new File("target/test-ts-withmodule");
        withoutModuleDir.mkdirs();
        withModuleDir.mkdirs();

        file("Test1", null, null, TypeScriptFormat.declarationFile, new File(withoutModuleDir, "test-mn1.d.ts"));
        file("Test2", null, "NS2", TypeScriptFormat.declarationFile, new File(withoutModuleDir, "test-mn2.d.ts"));
        file("Test3", "mod3", null, TypeScriptFormat.declarationFile, new File(withModuleDir, "test-mn3.d.ts"));
        file("Test4", "mod4", "NS4", TypeScriptFormat.declarationFile, new File(withModuleDir, "test-mn4.d.ts"));

        file("Test5", null, null, TypeScriptFormat.implementationFile, new File(withoutModuleDir, "test-mn5.ts"));
        file("Test6", null, "NS6", TypeScriptFormat.implementationFile, new File(withoutModuleDir, "test-mn6.ts"));
        file("Test7", "mod7", null, TypeScriptFormat.implementationFile, new File(withModuleDir, "test-mn7.ts"));
        file("Test8", "mod8", "NS8", TypeScriptFormat.implementationFile, new File(withModuleDir, "test-mn8.ts"));
    }

    private static void file(String prefix, String module, String namespace, TypeScriptFormat outputFileType, File output) {
        final Settings settings = new Settings();
        settings.jsonLibrary = JsonLibrary.jackson2;
        settings.addTypeNamePrefix = prefix;
        settings.module = module;
        settings.namespace = namespace;
        settings.outputFileType = outputFileType;
        if (outputFileType == TypeScriptFormat.implementationFile) {
            settings.extensions.add(new TestFunctionExtention());
        }
        new TypeScriptGenerator(settings).generateTypeScript(Input.from(Data.class), Output.to(output));
    }

    private static class Data {
    }

    private static class TestFunctionExtention extends EmitterExtension {

        @Override
        public boolean generatesRuntimeCode() {
            return true;
        }

        @Override
        public void emitElements(Writer writer, Settings settings, boolean exportKeyword, TsModel model) {
            writer.writeIndentedLine("");
            writer.writeIndentedLine((exportKeyword ? "export " : "") + "function test() {}");
        }

    }

}
