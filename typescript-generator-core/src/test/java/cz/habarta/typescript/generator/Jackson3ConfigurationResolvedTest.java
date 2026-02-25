package cz.habarta.typescript.generator;

import java.util.Arrays;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class Jackson3ConfigurationResolvedTest {

    @Test
    public void test() {
        final Jackson3Configuration configuration = new Jackson3Configuration();
        configuration.serializerTypeMappings = Arrays.asList(Jackson3ParserTest.IdSerializer.class.getName() + ":" + "string");
        final Jackson3ConfigurationResolved resolved = Jackson3ConfigurationResolved.from(configuration, Thread.currentThread().getContextClassLoader());
        Assertions.assertEquals("string", resolved.serializerTypeMappings.get(Jackson3ParserTest.IdSerializer.class));
    }

}