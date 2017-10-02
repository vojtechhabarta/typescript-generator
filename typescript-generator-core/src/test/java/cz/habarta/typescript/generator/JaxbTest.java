
package cz.habarta.typescript.generator;

import javax.xml.bind.JAXBElement;
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

    @Test
    public void testJAXBElement() {
        final Settings settings = TestUtils.settings();
        settings.jsonLibrary = JsonLibrary.jaxb;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ClassWithJAXBElements.class));
        Assert.assertTrue(output.contains("ExternalReference: string"));
        Assert.assertTrue(output.contains("UserInformation: UserType"));
        Assert.assertTrue(output.contains("Source: EndPointType"));
        Assert.assertTrue(output.contains("AdditionalContextInfo: AdditionalContextType"));
    }

    @XmlRootElement
    private static class ClassWithJAXBElements {
        @XmlElement(name = "ExternalReference")
        protected String externalReference;
        @XmlElementRef(name = "UserInformation", type = JAXBElement.class, required = false)
        protected JAXBElement<UserType> userInformation;
        @XmlElementRef(name = "Source", type = JAXBElement.class, required = false)
        protected JAXBElement<EndPointType> source;
        @XmlElementRef(name = "AdditionalContextInfo", type = JAXBElement.class, required = false)
        protected JAXBElement<AdditionalContextType> additionalContextInfo;
    }

    private static class UserType {
    }

    private static class EndPointType {
    }

    private static class AdditionalContextType {
    }

}
