
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;


public class MultipleTypeNamesTest {

    public static void main(String[] args) throws JsonProcessingException {
        System.out.println(new ObjectMapper().writeValueAsString(new SpecificType()));
        System.out.println(new ObjectMapper().readValue("{\"kind\":\"TYPE_1\"}", ParentType.class));
        System.out.println(new ObjectMapper().readValue("{\"kind\":\"TYPE_2\"}", ParentType.class));
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", include = JsonTypeInfo.As.PROPERTY)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = SpecificType.class, name = "TYPE_1"),
            @JsonSubTypes.Type(value = SpecificType.class, name = "TYPE_2"),
    })
    public static abstract class ParentType {
    }

    public static class SpecificType extends ParentType {
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", include = JsonTypeInfo.As.PROPERTY)
    @JsonSubTypes({
            @JsonSubTypes.Type(value = SpecificType2a.class, name = "TYPE_1"),
            @JsonSubTypes.Type(value = SpecificType2b.class, name = "TYPE_2"),
    })
    public static abstract class ParentType2 {
    }

    public static class SpecificType2a extends ParentType2 {
    }

    public static class SpecificType2b extends ParentType2 {
    }

    @Test
    public void testSubTypesSameClass() throws JsonProcessingException {
        final Settings settings = TestUtils.settings();
        settings.outputKind = TypeScriptOutputKind.module;
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.mapClasses = ClassMapping.asClasses;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ParentType.class, SpecificType.class));
        Assert.assertTrue(output.contains("TYPE_2"));
    }

    @Test
    public void testSubTypesDifferentClass() throws JsonProcessingException {
        final Settings settings = TestUtils.settings();
        settings.outputKind = TypeScriptOutputKind.module;
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.mapClasses = ClassMapping.asClasses;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ParentType2.class, SpecificType2a.class, SpecificType2b.class));
        Assert.assertTrue(output.contains("TYPE_2"));
    }

}
