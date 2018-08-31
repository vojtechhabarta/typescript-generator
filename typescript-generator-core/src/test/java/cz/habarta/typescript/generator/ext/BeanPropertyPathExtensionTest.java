package cz.habarta.typescript.generator.ext;

import cz.habarta.typescript.generator.DefaultTypeProcessor;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TypeProcessor;
import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.emitter.EmitterExtension;
import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.parser.Jackson2Parser;
import cz.habarta.typescript.generator.parser.Model;
import cz.habarta.typescript.generator.util.Utils;
import org.junit.Assert;
import org.junit.Test;

public class BeanPropertyPathExtensionTest {

    static class ClassA {
        public String field1;
        public ClassB field2;
        public ClassC field3;
    }

    static class ClassB {
        public int field1;
    }

    static class ClassC extends ClassB {
        public int field4;
    }

    @Test
    public void basicTest() throws Exception {
        final StringBuilder data = new StringBuilder();
        final EmitterExtension.Writer writer = new EmitterExtension.Writer() {
            @Override
            public void writeIndentedLine(String line) {
                data.append(line + "\n");
            }
        };
        final Settings settings = new Settings();
        settings.sortDeclarations = true;
        final TypeProcessor typeProcessor = new DefaultTypeProcessor();
        final Model model = new Jackson2Parser(settings, typeProcessor).parseModel(ClassA.class);
        final TsModel tsModel = new ModelCompiler(settings, typeProcessor).javaToTypeScript(model);
        new BeanPropertyPathExtension().emitElements(writer, settings, false, tsModel);
        String dataStr = data.toString();
        final String expected = Utils.readString(getClass().getResourceAsStream("/ext/expected.ts"), "\n");
        Assert.assertEquals(expected.trim(), dataStr.trim());
    }
}
