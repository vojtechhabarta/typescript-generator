
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonFormat;
import cz.habarta.typescript.generator.emitter.EmitterExtension;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.emitter.TsModel;
import java.io.File;
import org.junit.Test;

// run `tsc` compiler from `src/test/ts` directory after this test
public class ModulesAndNamespacesTest {

    @Test
    public void testNamespacesAndModules() {
        final File outputDir = new File("target/test-ts-modules");
        outputDir.mkdirs();

        file("Test1", null, null, TypeScriptOutputKind.global, TypeScriptFileType.declarationFile, new File(outputDir, "test-mn1.d.ts"));
        file("Test2", null, "NS2", TypeScriptOutputKind.global, TypeScriptFileType.declarationFile, new File(outputDir, "test-mn2.d.ts"));
        file("Test3a", "mod3a", null, TypeScriptOutputKind.ambientModule, TypeScriptFileType.declarationFile, new File(outputDir, "test-mn3a.d.ts"));
        file("Test3b", null, null, TypeScriptOutputKind.module, TypeScriptFileType.declarationFile, new File(outputDir, "test-mn3b.d.ts"));
        file("Test4a", "mod4a", "NS4a", TypeScriptOutputKind.ambientModule, TypeScriptFileType.declarationFile, new File(outputDir, "test-mn4a.d.ts"));
        file("Test4b", null, "NS4b", TypeScriptOutputKind.module, TypeScriptFileType.declarationFile, new File(outputDir, "test-mn4b.d.ts"));

        file("Test5", null, null, TypeScriptOutputKind.global, TypeScriptFileType.implementationFile, new File(outputDir, "test-mn5.ts"));
        file("Test6", null, "NS6", TypeScriptOutputKind.global, TypeScriptFileType.implementationFile, new File(outputDir, "test-mn6.ts"));
        file("Test7", null, null, TypeScriptOutputKind.module, TypeScriptFileType.implementationFile, new File(outputDir, "test-mn7.ts"));
        file("Test8", null, "NS8", TypeScriptOutputKind.module, TypeScriptFileType.implementationFile, new File(outputDir, "test-mn8.ts"));
    }

    private static void file(String prefix, String module, String namespace, TypeScriptOutputKind outputKind, TypeScriptFileType outputFileType, File output) {
        final Settings settings = new Settings();
        settings.jsonLibrary = JsonLibrary.jackson2;
        settings.addTypeNamePrefix = prefix;
        settings.module = module;
        settings.namespace = namespace;
        settings.outputKind = outputKind;
        settings.outputFileType = outputFileType;
        if (outputFileType == TypeScriptFileType.implementationFile) {
            settings.extensions.add(new TestFunctionExtention());
        }
        new TypeScriptGenerator(settings).generateTypeScript(Input.from(Data.class, Direction.class, Align.class), Output.to(output));
    }

    private static class Data {
    }

    enum Direction {
        North,
        East, 
        South,
        West
    }

    @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    enum Align {
        Left,
        Right
    }

    private static class TestFunctionExtention extends EmitterExtension {

        @Override
        public EmitterExtensionFeatures getFeatures() {
            final EmitterExtensionFeatures features = new EmitterExtensionFeatures();
            features.generatesRuntimeCode = true;
            return features;
        }

        @Override
        public void emitElements(Writer writer, Settings settings, boolean exportKeyword, TsModel model) {
            writer.writeIndentedLine("");
            writer.writeIndentedLine((exportKeyword ? "export " : "") + "function test() {}");
        }

    }

}
