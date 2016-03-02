
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.parser.BeanModel;
import cz.habarta.typescript.generator.parser.EnumModel;
import cz.habarta.typescript.generator.parser.Jackson2Parser;
import cz.habarta.typescript.generator.parser.Javadoc;
import cz.habarta.typescript.generator.parser.Model;
import cz.habarta.typescript.generator.parser.PropertyModel;
import java.io.File;
import java.util.*;
import org.junit.Assert;
import org.junit.Test;


public class JavadocTest {

    @Test
    public void testJavadocInModelParser() {
        final Settings settings = new Settings();
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
            final String generated = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithJavadoc.class));
            System.out.println(generated);
            Assert.assertTrue(generated.contains("Documentation for ClassWithJavadoc. First line."));
            Assert.assertTrue(generated.contains("Second line."));
            Assert.assertTrue(generated.contains("Documentation for documentedField."));
            Assert.assertTrue(generated.contains("Documentation for documentedEnumField."));
            Assert.assertTrue(generated.contains("Documentation for DummyEnum."));
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

    }

    public static class ClassWithoutJavadoc {

        public String undocumentedField;

    }

}
