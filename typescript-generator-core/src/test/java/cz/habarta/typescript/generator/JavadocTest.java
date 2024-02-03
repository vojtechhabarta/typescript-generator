
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonProperty;
import cz.habarta.typescript.generator.parser.BeanModel;
import cz.habarta.typescript.generator.parser.EnumModel;
import cz.habarta.typescript.generator.parser.Jackson2Parser;
import cz.habarta.typescript.generator.parser.Model;
import cz.habarta.typescript.generator.parser.PropertyModel;
import java.io.File;
import java.util.Collections;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;


public class JavadocTest {
    final Settings settings = TestUtils.settings();
    final TypeProcessor typeProcessor = new DefaultTypeProcessor();

    @BeforeEach
    void initSettings() {
        settings.javadocXmlFiles = Collections.singletonList(new File("src/test/javadoc/test-javadoc.xml"));
    }

    @Test
    void javadocXml() {
        {
            final Model model = new Jackson2Parser(settings, typeProcessor).parseModel(ClassWithJavadoc.class);
            final BeanModel bean = model.getBeans().get(0);
            Assertions.assertEquals("Documentation for ClassWithJavadoc. First line.", bean.getComments().get(0));
            Assertions.assertEquals("Second line.", bean.getComments().get(1));
            final PropertyModel property1 = bean.getProperties().get(0);
            Assertions.assertEquals("Documentation for documentedField.", property1.getComments().get(0));
            final PropertyModel property2 = bean.getProperties().get(1);
            Assertions.assertEquals("Documentation for documentedEnumField.", property2.getComments().get(0));
            final EnumModel enumModel = model.getEnums().get(0);
            Assertions.assertEquals("Documentation for DummyEnum.", enumModel.getComments().get(0));
        }
    }

    @Test
    void classWithoutJavadoc() {
        {
            final Model model = new Jackson2Parser(settings, typeProcessor).parseModel(ClassWithoutJavadoc.class);
            final BeanModel bean = model.getBeans().get(0);
            Assertions.assertNull(bean.getComments());
            final PropertyModel property = bean.getProperties().get(0);
            Assertions.assertNull(property.getComments());
        }
    }

    @Test
    void classWithEmbeddedExample() {
        {
            final String generated = new TypeScriptGenerator(settings).generateTypeScript(
                    Input.from(ClassWithJavadoc.class, InterfaceWithJavadoc.class, ClassWithEmbeddedExample.class));

            Assertions.assertTrue(generated.contains("Documentation for ClassWithJavadoc. First line."));
            Assertions.assertTrue(generated.contains("Second line."));
            Assertions.assertTrue(generated.contains("Documentation for documentedField."));
            Assertions.assertTrue(generated.contains("Documentation for documentedEnumField."));
            Assertions.assertTrue(generated.contains("Documentation for DummyEnum."));
            Assertions.assertTrue(generated.contains("Documentation for getter property."));
            Assertions.assertTrue(generated.contains("Documentation for renamed field."));
            Assertions.assertTrue(generated.contains("Documentation for InterfaceWithJavadoc."));
            Assertions.assertTrue(generated.contains("Documentation for interface getter property."));
            Assertions.assertTrue(generated.contains("@return value of getterPropery"));
            Assertions.assertTrue(generated.contains("@deprecated replaced by something else\n"));
            Assertions.assertTrue(!generated.contains("@deprecated\n"));
            Assertions.assertTrue(generated.contains(" *     // indentation and line breaks are kept\n * \n *     {@literal @}"));
            Assertions.assertTrue(generated.contains(" *     public List<String> generics() {\n"));
            Assertions.assertTrue(generated.contains("ff0000"));
            Assertions.assertTrue(generated.contains("00ff00"));
            Assertions.assertTrue(generated.contains("0000ff"));
        }
    }

    @Test
    void deprecatedClassWithoutJavadoc() {
        {
            final String generated = new TypeScriptGenerator(settings).generateTypeScript(Input.from(DeprecatedClassWithoutJavadoc.class));
            final String expected = ""
                    + "/**\n"
                    + " * @deprecated\n"
                    + " */\n"
                    + "interface DeprecatedClassWithoutJavadoc {\n"
                    + "    /**\n"
                    + "     * @deprecated\n"
                    + "     */\n"
                    + "    deprecatedField: string;\n"
                    + "}";
            Assertions.assertEquals(expected.trim(), generated.trim());
        }
    }

    @Test
    void deprecatedEnumWithoutJavadoc() {
        {
            final String generated = new TypeScriptGenerator(settings).generateTypeScript(Input.from(DeprecatedEnumWithoutJavadoc.class));
            final String expected = ""
                    + "/**\n"
                    + " * @deprecated\n"
                    + " * \n"
                    + " * Values:\n"
                    + " * - `North`\n"
                    + " * - `East` - deprecated\n"
                    + " * - `South`\n"
                    + " * - `West`\n"
                    + " */\n"
                    + "type DeprecatedEnumWithoutJavadoc = \"North\" | \"East\" | \"South\" | \"West\";\n"
                    + "";
            Assertions.assertEquals(expected.trim(), generated.trim());
        }
    }

    @Test
    void deprecatedEnumWItem() {
        final String generated = new TypeScriptGenerator(settings).generateTypeScript(Input.from(DeprecatedEnumItem.class));
        final String expected = ""
                + "/**\n"
                + " * Values:\n"
                + " * - `First`\n"
                + " * - `Second` - deprecated\n"
                + " * - `Third`\n"
                + " */\n"
                + "type DeprecatedEnumItem = \"First\" | \"Second\" | \"Third\";\n"
                + "";
        Assertions.assertEquals(expected.trim(), generated.trim());
    }

    @Test
    void classWithBrInJavadoc() {
        {
            final String generated = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithBrElements.class));
            Assertions.assertTrue(!generated.contains("<br>"));
            Assertions.assertTrue(!generated.contains("<br/>"));
            Assertions.assertTrue(!generated.contains("<br />"));
            Assertions.assertTrue(generated.contains("Class documentation\n * \n"));
            Assertions.assertTrue(generated.contains("Some documentation\n * \n * for this class."));
        }
    }

    @Test
    void classWithPInJavadoc() {
        {
            final String generated = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithPElements.class));
            Assertions.assertTrue(!generated.contains("<p>"));
            Assertions.assertTrue(!generated.contains("</p>"));
            Assertions.assertTrue(generated.contains("Long\n * paragraph\n * \n * Second\n * paragraph"));
        }
    }

    /**
     * Documentation for ClassWithJavadoc. First line.
     * Second line.
     */
    public static class ClassWithJavadoc {

        /**
         * Documentation for documentedField.
         */
        public String documentedField;

        /**
         * Documentation for documentedEnumField.
         */
        public DummyEnum documentedEnumField;

        /**
         * Documentation for getter property.
         */
        public String getGetterPropery() {
            return null;
        }

        /**
         * Documentation for renamed field.
         */
        @JsonProperty(value = "newName")
        public String originalName;

    }

    /**
     * Documentation for InterfaceWithJavadoc.
     */
    public static interface InterfaceWithJavadoc {

        /**
         * Documentation for interface getter property.
         * @return value of getterPropery
         * @deprecated replaced by something else
         */
        @Deprecated
        public String getGetterPropery();

    }

    public static class ClassWithoutJavadoc {

        public String undocumentedField;

    }

    @Deprecated
    public static class DeprecatedClassWithoutJavadoc {

        @Deprecated
        public String deprecatedField;

    }

    @Deprecated
    public static enum DeprecatedEnumWithoutJavadoc {

        North,
        @Deprecated East,
        South,
        West;

    }

    public enum DeprecatedEnumItem {
        First,
        @Deprecated Second,
        Third;
    }

    /**
     * This class comes with an embedded example!
     *
     * <pre>{@code
     * public class Example {
     *     // indentation and line breaks are kept
     *
     *     {@literal @}SuppressWarnings
     *     public List<String> generics() {
     *         return null;
     *     }
     * }
     * }</pre>
     */
    public static class ClassWithEmbeddedExample {

        public String field;

    }

    /**
     * Class documentation <br>
     * ------------------- <br/>
     * Some documentation <br /> for this class.<br>
     */
    public static class ClassWithBrElements {
    }

    /**
     * First sentence.
     * 
     * <p> Long
     * paragraph </p>
     * 
     * <p>Second
     * paragraph</p>
     */
    public static class ClassWithPElements {
    }

}
