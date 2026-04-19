
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


@SuppressWarnings("unused")
public class SealedInterfaceTest {

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "unit")
    public sealed interface Quantity {
        sealed interface DecimalAmount extends Quantity {
            @JsonTypeName("gram")
            record Gram(BigDecimal amount) implements DecimalAmount {}

            @JsonTypeName("kilogram")
            record Kilogram(BigDecimal amount) implements DecimalAmount {}

            @JsonTypeName("liter")
            record Liter(BigDecimal amount) implements DecimalAmount {}

            @JsonTypeName("milliliter")
            record Milliliter(BigDecimal amount) implements DecimalAmount {}

            @JsonTypeName("arbitrary")
            record Arbitrary(BigDecimal amount) implements DecimalAmount {}
        }

        @JsonTypeName("unspecified")
        record Unspecified(String notes) implements Quantity {}
    }

    static class Recipe {
        public List<Quantity> quantities;
    }

    @Test
    public void testSealedInterfaceMarkerExcluded() {
        final Settings settings = TestUtils.settings();
        settings.quotes = "'";
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Recipe.class));

        // Should NOT contain DecimalAmount interface - it's a sealed marker without @JsonSubTypes
        Assertions.assertFalse(output.contains("interface DecimalAmount"),
            "DecimalAmount should not appear as it's a sealed marker without @JsonSubTypes\n" + output);

        // Should NOT contain DecimalAmount in the union
        Assertions.assertFalse(output.contains("| DecimalAmount"),
            "DecimalAmount should not appear in QuantityUnion\n" + output);

        // Should have leaf types
        Assertions.assertTrue(output.contains("interface Gram"), "Should have Gram");
        Assertions.assertTrue(output.contains("interface Kilogram"), "Should have Kilogram");
        Assertions.assertTrue(output.contains("interface Unspecified"), "Should have Unspecified");

        // Union should only have leaf types
        Assertions.assertTrue(output.contains("type QuantityUnion = Gram | Kilogram | Liter | Milliliter | Arbitrary | Unspecified"),
            "QuantityUnion should only contain leaf types\n" + output);
    }
}
