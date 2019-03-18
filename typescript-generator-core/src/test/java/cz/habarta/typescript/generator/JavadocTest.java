
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonProperty;
import cz.habarta.typescript.generator.parser.BeanModel;
import cz.habarta.typescript.generator.parser.EnumModel;
import cz.habarta.typescript.generator.parser.Jackson2Parser;
import cz.habarta.typescript.generator.parser.Model;
import cz.habarta.typescript.generator.parser.PropertyModel;
import java.io.File;
import java.util.*;
import org.junit.Assert;
import org.junit.Test;


public class JavadocTest {

    @Test
    public void testJavadoc() {
        final Settings settings = TestUtils.settings();
        settings.javadocXmlFiles = Arrays.asList(new File("target/test-javadoc.xml"));
        final TypeProcessor typeProcessor = new DefaultTypeProcessor();
        {
            final Model model = new Jackson2Parser(settings, typeProcessor).parseModel(ClassWithJavadoc.class);
            final BeanModel bean = model.getBeans().get(0);
            Assert.assertEquals("Documentation for ClassWithJavadoc. First line.", bean.getComments().get(0));
            Assert.assertEquals("Second line.", bean.getComments().get(1));
            final PropertyModel property1 = bean.getProperties().get(0);
            Assert.assertEquals("Documentation for documentedField.", property1.getComments().get(0));
            final PropertyModel property2 = bean.getProperties().get(1);
            Assert.assertEquals("Documentation for documentedEnumField.", property2.getComments().get(0));
            final EnumModel enumModel = model.getEnums().get(0);
            Assert.assertEquals("Documentation for DummyEnum.", enumModel.getComments().get(0));
        }
        {
            final Model model = new Jackson2Parser(settings, typeProcessor).parseModel(ClassWithoutJavadoc.class);
            final BeanModel bean = model.getBeans().get(0);
            Assert.assertNull(bean.getComments());
            final PropertyModel property = bean.getProperties().get(0);
            Assert.assertNull(property.getComments());
        }
        {
            final String generated = new TypeScriptGenerator(settings).generateTypeScript(
                    Input.from(ClassWithJavadoc.class, InterfaceWithJavadoc.class, ClassWithEmbeddedExample.class));

            Assert.assertTrue(generated.contains("Documentation for ClassWithJavadoc. First line."));
            Assert.assertTrue(generated.contains("Second line."));
            Assert.assertTrue(generated.contains("Documentation for documentedField."));
            Assert.assertTrue(generated.contains("Documentation for documentedEnumField."));
            Assert.assertTrue(generated.contains("Documentation for DummyEnum."));
            Assert.assertTrue(generated.contains("Documentation for getter property."));
            Assert.assertTrue(generated.contains("Documentation for renamed field."));
            Assert.assertTrue(generated.contains("Documentation for InterfaceWithJavadoc."));
            Assert.assertTrue(generated.contains("Documentation for interface getter property."));
            Assert.assertTrue(generated.contains("@return value of getterPropery"));
            Assert.assertTrue(generated.contains("@deprecated replaced by something else"));

            Assert.assertTrue(generated.contains(" *     // indentation and line breaks are kept\n * \n *     {@literal @}"));
            Assert.assertTrue(generated.contains(" *     public List<String> generics() {\n"));
        }
        {
            final String generated = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithBrElements.class));
            Assert.assertTrue(!generated.contains("<br>"));
            Assert.assertTrue(!generated.contains("<br/>"));
            Assert.assertTrue(!generated.contains("<br />"));
            Assert.assertTrue(generated.contains("Class documentation\n * \n"));
            Assert.assertTrue(generated.contains("Some documentation\n * \n * for this class."));
        }
        {
            final String generated = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithPElements.class));
            Assert.assertTrue(!generated.contains("<p>"));
            Assert.assertTrue(!generated.contains("</p>"));
            Assert.assertTrue(generated.contains("Long\n * paragraph\n * \n * Second\n * paragraph"));
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
