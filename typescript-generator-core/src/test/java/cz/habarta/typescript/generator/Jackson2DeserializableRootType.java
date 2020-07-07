
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

/**
 * Test that root type name in a hirarchy is included iff root type itself is not abstract
 */
public class Jackson2DeserializableRootType {

    @Test
    public void testHowJacksonDeserializes() throws JsonProcessingException {
        NonAbstractRoot nar = new ObjectMapper()
                        .readValue("{\"type\": \"rootType\"}",
                                        NonAbstractRoot.class);
        NonAbstractRoot nars = new ObjectMapper()
                        .readValue("{\"type\": \"subType\"}",
                                        NonAbstractRoot.class);

        Assert.assertSame(NonAbstractRoot.class, nar.getClass());
        Assert.assertSame(NonAbstractRootSub.class, nars.getClass());
    }

    @Test
    public void testRootTypeIncludedIfNotAbstract() {
        final String output = new TypeScriptGenerator(TestUtils.settings()).generateTypeScript(Input.from(NonAbstractRoot.class));
        Assert.assertTrue(output.contains("\"rootType\""));
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes(@JsonSubTypes.Type(NonAbstractRootSub.class))
    @JsonTypeName("rootType")
    public static class NonAbstractRoot {
    }

    @JsonTypeName("subType")
    public static class NonAbstractRootSub extends NonAbstractRoot {
    }

    @Test
    public void testRootTypeNotIncludedIfAbstract() {
        final String output = new TypeScriptGenerator(TestUtils.settings()).generateTypeScript(Input.from(AbstractRoot.class));
        // Root type is abstract and therefore ignored in the type list
        Assert.assertFalse(output.contains("\"rootType\""));
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
    @JsonSubTypes(@JsonSubTypes.Type(AbstractRootSub.class))
    @JsonTypeName("rootType")
    public static abstract class AbstractRoot {
    }

    @JsonTypeName("subType")
    public static class AbstractRootSub extends AbstractRoot {
    }
}
