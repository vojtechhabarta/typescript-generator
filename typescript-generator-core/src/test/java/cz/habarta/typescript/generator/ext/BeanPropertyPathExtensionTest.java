package cz.habarta.typescript.generator.ext;

import java.util.Arrays;
import cz.habarta.typescript.generator.TypeProcessor;
import cz.habarta.typescript.generator.DefaultTypeProcessor;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.emitter.EmitterExtension;
import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.ext.BeanPropertyPathExtension;
import cz.habarta.typescript.generator.parser.Jackson2Parser;
import cz.habarta.typescript.generator.parser.Model;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;

public class BeanPropertyPathExtensionTest {

    static class ClassA {
        public String field1;
        public ClassB field2;
    }

    static class ClassB {
        public int field1;
    }

    @Test
    public void basicTest() {
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
        Assert.assertEquals(29, dataStr.split("\n").length);
        Assert.assertTrue(dataStr.contains("class ClassAFields"));
        Assert.assertTrue(dataStr.contains("class ClassBFields"));
    }
}
