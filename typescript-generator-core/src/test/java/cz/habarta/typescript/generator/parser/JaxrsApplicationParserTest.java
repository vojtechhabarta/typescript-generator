
package cz.habarta.typescript.generator.parser;

import jakarta.ws.rs.ApplicationPath;
import org.junit.Assert;
import org.junit.Test;


public class JaxrsApplicationParserTest {

    @jakarta.ws.rs.ApplicationPath("testJakarta")
    private static class JakartaRsClass {}

    @javax.ws.rs.ApplicationPath("testJavax")
    private static class JavaxRsClass {}

    @Test
    public void testJakartaAnnotation() {
        final ApplicationPath annotation = JaxrsApplicationParser.getRsAnnotation(JakartaRsClass.class, jakarta.ws.rs.ApplicationPath.class);
        Assert.assertNotNull(annotation);
        Assert.assertEquals("testJakarta", annotation.value());
    }

    @Test
    public void testJavaxAnnotation() {
        final ApplicationPath annotation = JaxrsApplicationParser.getRsAnnotation(JavaxRsClass.class, jakarta.ws.rs.ApplicationPath.class);
        Assert.assertNotNull(annotation);
        Assert.assertEquals("testJavax", annotation.value());
    }

}
