
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.emitter.EmitterExtension;
import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.ext.TypeGuardsForJackson2PolymorphismExtension;
import cz.habarta.typescript.generator.parser.Jackson2Parser;
import cz.habarta.typescript.generator.parser.Model;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


@SuppressWarnings("unused")
public class TypeGuardsForJackson2PolymorphismExtensionTest {

    @Test
    public void basicTest() {
        final List<String> lines = new ArrayList<>();
        final EmitterExtension.Writer writer = new EmitterExtension.Writer() {
            @Override
            public void writeIndentedLine(String line) {
                lines.add(line);
            }
        };
        final Settings settings = TestUtils.settings();
        final TypeProcessor typeProcessor = new DefaultTypeProcessor();
        final Model model = new Jackson2Parser(settings, typeProcessor).parseModel(Point.class);
        final TsModel tsModel = new ModelCompiler(settings, typeProcessor).javaToTypeScript(model);
        new TypeGuardsForJackson2PolymorphismExtension().emitElements(writer, settings, false, tsModel);
        Assertions.assertEquals(8, lines.size());
        Assertions.assertEquals("", lines.get(0));
        Assertions.assertEquals("function isCartesianPoint(point: Point): point is CartesianPoint {", lines.get(1));
        Assertions.assertEquals("    return point.type === \"cartesian\";", lines.get(2));
        Assertions.assertEquals("}", lines.get(3));
    }

    @Test
    public void testInTypeScriptGenerator() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.addTypeNamePrefix = "Json";
        settings.extensions.add(new TypeGuardsForJackson2PolymorphismExtension());
        final String actual = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Point.class));
        Assertions.assertTrue(actual.contains("type: \"cartesian\" | \"polar\";"));
        Assertions.assertTrue(actual.contains("function isJsonCartesianPoint(jsonPoint: JsonPoint): jsonPoint is JsonCartesianPoint {"));
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(CartesianPoint.class),
        @JsonSubTypes.Type(PolarPoint.class)
    })
    private static interface Point {
    }

    @JsonTypeName("cartesian")
    private static class CartesianPoint implements Point {
        public double x, y;
    }

    @JsonTypeName("polar")
    private static class PolarPoint implements Point {
        public double r, theta;
    }

}
