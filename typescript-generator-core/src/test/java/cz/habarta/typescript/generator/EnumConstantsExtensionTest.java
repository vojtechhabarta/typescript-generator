
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.EnumTest.Direction;
import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.emitter.EmitterExtension;
import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.ext.EnumConstantsExtension;
import cz.habarta.typescript.generator.parser.Jackson2Parser;
import cz.habarta.typescript.generator.parser.Model;
import java.util.ArrayList;
import java.util.List;
import org.junit.Assert;
import org.junit.Test;


public class EnumConstantsExtensionTest {

    @Test
    public void basicTest() {
        final List<String> lines = new ArrayList<>();
        final EmitterExtension.Writer writer = new EmitterExtension.Writer() {
            @Override
            public void writeIndentedLine(String line) {
                lines.add(line);
            }
        };
        final Settings settings = new Settings();
        settings.sortDeclarations = true;
        final TypeProcessor typeProcessor = new DefaultTypeProcessor();
        final Model model = new Jackson2Parser(settings, typeProcessor).parseModel(Direction.class);
        final TsModel tsModel = new ModelCompiler(settings, typeProcessor).javaToTypeScript(model);
        new EnumConstantsExtension().emitElements(writer, settings, false, tsModel);
        String indent = settings.indentString;
        Assert.assertEquals(7, lines.size());
        Assert.assertEquals("", lines.get(0));
        Assert.assertEquals("const Direction = {", lines.get(1));
        Assert.assertEquals(indent + "North: <Direction>\"North\",", lines.get(2));
        Assert.assertEquals(indent + "East: <Direction>\"East\",", lines.get(3));
        Assert.assertEquals(indent + "South: <Direction>\"South\",", lines.get(4));
        Assert.assertEquals(indent + "West: <Direction>\"West\",", lines.get(5));
        Assert.assertEquals("}", lines.get(6));
    }

    @Test
    public void testInTypeScriptGenerator() {
        final Settings settings = new Settings();
        settings.newline = "\n";
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.outputKind = TypeScriptOutputKind.global;
        settings.jsonLibrary = JsonLibrary.jackson2;
        settings.extensions.add(new EnumConstantsExtension());
        final String actual = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Direction.class));
        Assert.assertTrue(actual.contains("const Direction"));
        Assert.assertTrue(actual.contains("North"));
    }

    @Test
    public void testSorting() {
        final Settings settings = TestUtils.settings();
        settings.sortDeclarations = false;
        settings.newline = "\n";
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.outputKind = TypeScriptOutputKind.global;
        settings.jsonLibrary = JsonLibrary.jackson2;
        settings.extensions.add(new EnumConstantsExtension());
        Assert.assertNotEquals(new TypeScriptGenerator(settings).generateTypeScript(Input.from(Emotions.class, Direction.class)),
                               new TypeScriptGenerator(settings).generateTypeScript(Input.from(Direction.class, Emotions.class)));
        settings.sortDeclarations = true;
        Assert.assertEquals(new TypeScriptGenerator(settings).generateTypeScript(Input.from(Emotions.class, Direction.class)),
                            new TypeScriptGenerator(settings).generateTypeScript(Input.from(Direction.class, Emotions.class)));
    }

    public enum Emotions {
        Happy
    }
}
