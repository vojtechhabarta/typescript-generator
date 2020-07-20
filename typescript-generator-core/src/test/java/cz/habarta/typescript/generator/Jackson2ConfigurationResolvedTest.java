package cz.habarta.typescript.generator;

import java.util.Arrays;
import org.junit.Assert;
import org.junit.Test;

public class Jackson2ConfigurationResolvedTest {

    @Test
    public void test() {
        final Jackson2Configuration configuration = new Jackson2Configuration();
        configuration.serializerTypeMappings = Arrays.asList(Jackson2ParserTest.IdSerializer.class.getName() + ":" + "string");
        final Jackson2ConfigurationResolved resolved = Jackson2ConfigurationResolved.from(configuration, Thread.currentThread().getContextClassLoader());
        Assert.assertEquals("string", resolved.serializerTypeMappings.get(Jackson2ParserTest.IdSerializer.class));
    }

}