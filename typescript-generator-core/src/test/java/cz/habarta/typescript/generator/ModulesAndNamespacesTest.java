
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonFormat;
import cz.habarta.typescript.generator.emitter.EmitterExtension;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.emitter.TsModel;
import java.io.File;
import org.junit.jupiter.api.Test;

// run `tsc` compiler from `src/test/ts` directory after this test
public class ModulesAndNamespacesTest {

    @Test
    public void testNamespacesAndModules() {
        files(new File("target/test-ts-modules"), false);
        files(new File("target/test-ts-modules-pkg"), true);
    }

    public void files(File outputDir, boolean mapPackages) {
        outputDir.mkdirs();

        file("Test1", null, null, mapPackages, TypeScriptOutputKind.global, TypeScriptFileType.declarationFile, new File(outputDir, "test-mn1.d.ts"));
        file("Test2", null, "NS2", mapPackages, TypeScriptOutputKind.global, TypeScriptFileType.declarationFile, new File(outputDir, "test-mn2.d.ts"));
        file("Test3a", "mod3a", null, mapPackages, TypeScriptOutputKind.ambientModule, TypeScriptFileType.declarationFile, new File(outputDir, "test-mn3a.d.ts"));
        file("Test3b", null, null, mapPackages, TypeScriptOutputKind.module, TypeScriptFileType.declarationFile, new File(outputDir, "test-mn3b.d.ts"));
        file("Test4a", "mod4a", "NS4a", mapPackages, TypeScriptOutputKind.ambientModule, TypeScriptFileType.declarationFile, new File(outputDir, "test-mn4a.d.ts"));
        file("Test4b", null, "NS4b", mapPackages, TypeScriptOutputKind.module, TypeScriptFileType.declarationFile, new File(outputDir, "test-mn4b.d.ts"));

        file("Test5", null, null, mapPackages, TypeScriptOutputKind.global, TypeScriptFileType.implementationFile, new File(outputDir, "test-mn5.ts"));
        file("Test6", null, "NS6", mapPackages, TypeScriptOutputKind.global, TypeScriptFileType.implementationFile, new File(outputDir, "test-mn6.ts"));
        file("Test7", null, null, mapPackages, TypeScriptOutputKind.module, TypeScriptFileType.implementationFile, new File(outputDir, "test-mn7.ts"));
        file("Test8", null, "NS8", mapPackages, TypeScriptOutputKind.module, TypeScriptFileType.implementationFile, new File(outputDir, "test-mn8.ts"));
    }

    private static void file(String prefix, String module, String namespace, boolean mapPackagesToNamespaces, TypeScriptOutputKind outputKind, TypeScriptFileType outputFileType, File output) {
        final Settings settings = new Settings();
        settings.jsonLibrary = JsonLibrary.jackson2;
        settings.mapEnum = EnumMapping.asEnum;
        settings.addTypeNamePrefix = prefix;
        settings.module = module;
        settings.namespace = namespace;
        settings.mapPackagesToNamespaces = mapPackagesToNamespaces;
        settings.outputKind = outputKind;
        settings.outputFileType = outputFileType;
        if (outputFileType == TypeScriptFileType.implementationFile) {
            settings.nonConstEnums = true;
            settings.mapClasses = ClassMapping.asClasses;
        }
        if (outputFileType == TypeScriptFileType.implementationFile && !mapPackagesToNamespaces) {
            settings.extensions.add(new TestFunctionExtension());
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

    private static class TestFunctionExtension extends EmitterExtension {

        @Override
        public EmitterExtensionFeatures getFeatures() {
            final EmitterExtensionFeatures features = new EmitterExtensionFeatures();
            features.generatesRuntimeCode = true;
            features.worksWithPackagesMappedToNamespaces = true;
            return features;
        }

        @Override
        public void emitElements(Writer writer, Settings settings, boolean exportKeyword, TsModel model) {
            writer.writeIndentedLine("");
            writer.writeIndentedLine((exportKeyword ? "export " : "") + "function test() {}");
        }

    }

}
