
package cz.habarta.typescript.generator.parser;

import jakarta.ws.rs.ApplicationPath;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class JaxrsApplicationParserTest {

    @jakarta.ws.rs.ApplicationPath("testJakarta")
    private static class JakartaRsClass {
    }

    @Test
    public void testJakartaAnnotation() {
        final ApplicationPath annotation = JaxrsApplicationParser.getRsAnnotation(JakartaRsClass.class, jakarta.ws.rs.ApplicationPath.class);
        Assertions.assertNotNull(annotation);
        Assertions.assertEquals("testJakarta", annotation.value());
    }

}
