
package cz.habarta.typescript.generator;

import javax.xml.bind.annotation.*;
import org.junit.Assert;
import org.junit.Test;


public class JaxbTest {

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        settings.jsonLibrary = JsonLibrary.jaxb;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(MyJaxbBean.class));
        Assert.assertTrue(output.contains("king"));
        Assert.assertFalse(output.contains("age"));
    }

    @XmlRootElement
    private static class MyJaxbBean {

        @XmlElement(name = "king")
        public String name;

        @XmlTransient
        public int age;

    }

}
