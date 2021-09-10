
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonValue;
import cz.habarta.typescript.generator.compiler.EnumKind;
import cz.habarta.typescript.generator.parser.EnumModel;
import cz.habarta.typescript.generator.parser.Model;
import cz.habarta.typescript.generator.parser.ModelParser;
import java.io.File;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class NumberEnumTest {

    @Test
    public void testParser() {
        final Settings settings = TestUtils.settings();
        final ModelParser parser = new TypeScriptGenerator(settings).getModelParser();
        final Model model = parser.parseModel(SomeCode.class);
        Assertions.assertEquals(1, model.getEnums().size());
        final EnumModel enumModel = model.getEnums().get(0);
        Assertions.assertEquals(EnumKind.NumberBased, enumModel.getKind());
        Assertions.assertEquals(2, enumModel.getMembers().size());
        Assertions.assertEquals(10, ((Number)enumModel.getMembers().get(0).getEnumValue()).intValue());
        Assertions.assertEquals(11, ((Number)enumModel.getMembers().get(1).getEnumValue()).intValue());
    }

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(SomeCode.class));
        Assertions.assertEquals(
                "declare const enum SomeCode {\n" +
                "    VALUE0 = 10,\n" +
                "    VALUE1 = 11,\n" +
                "}",
                output.trim());
    }

    @Test
    public void testNonConstEnum() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.nonConstEnums = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(SomeCode.class));
        Assertions.assertEquals(
                "enum SomeCode {\n" +
                "    VALUE0 = 10,\n" +
                "    VALUE1 = 11,\n" +
                "}",
                output.trim());
    }

    @Test
    public void testNonConstAnnotationEnum() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.nonConstEnumAnnotations.add(SomeNonConstAnnotation.class);
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(SomeCode.class));
        Assertions.assertEquals(
                "enum SomeCode {\n" +
                "    VALUE0 = 10,\n" +
                "    VALUE1 = 11,\n" +
                "}",
                output.trim());
    }

    @Test
    public void testJavadoc() {
        final Settings settings = TestUtils.settings();
        settings.javadocXmlFiles = Arrays.asList(new File("src/test/javadoc/test-javadoc.xml"));
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(SomeCode.class));
        Assertions.assertTrue(output.contains("Documentation for SomeCode enum."));
        Assertions.assertTrue(output.contains("Documentation for VALUE0."));
        Assertions.assertTrue(output.contains("Documentation for VALUE1."));
    }

    /**
     * Documentation for SomeCode enum.
     */
    @JsonFormat(shape = JsonFormat.Shape.NUMBER_INT)
    @SomeNonConstAnnotation
    public static enum SomeCode {

        /**
         * Documentation for VALUE0.
         */
        VALUE0(10),
        /**
         * Documentation for VALUE1.
         */
        VALUE1(11);

        private final Integer jsonValue;

        private SomeCode(final Integer jsonValue) {
            this.jsonValue = jsonValue;
        }

        @JsonValue
        public Integer getJsonValue() {
            return this.jsonValue;
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    @Target({ElementType.TYPE})
    public @interface SomeNonConstAnnotation {
    }

}
