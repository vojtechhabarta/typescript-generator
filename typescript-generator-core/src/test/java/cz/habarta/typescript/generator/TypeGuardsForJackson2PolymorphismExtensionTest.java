
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.*;
import cz.habarta.typescript.generator.compiler.*;
import cz.habarta.typescript.generator.emitter.*;
import cz.habarta.typescript.generator.ext.TypeGuardsForJackson2PolymorphismExtension;
import cz.habarta.typescript.generator.parser.*;
import java.util.*;
import org.junit.*;


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
        Assert.assertEquals(8, lines.size());
        Assert.assertEquals("", lines.get(0));
        Assert.assertEquals("function isCartesianPoint(point: Point): point is CartesianPoint {", lines.get(1));
        Assert.assertEquals("    return point.type === \"cartesian\";", lines.get(2));
        Assert.assertEquals("}", lines.get(3));
    }

    @Test
    public void testInTypeScriptGenerator() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.addTypeNamePrefix = "Json";
        settings.extensions.add(new TypeGuardsForJackson2PolymorphismExtension());
        final String actual = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Point.class));
        Assert.assertTrue(actual.contains("type: \"cartesian\" | \"polar\";"));
        Assert.assertTrue(actual.contains("function isJsonCartesianPoint(jsonPoint: JsonPoint): jsonPoint is JsonCartesianPoint {"));
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
