
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class DuplicatePropertyTest {

    public static class DuplicateKindUsage {
        public DuplicateKind duplicateKind;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind", include = JsonTypeInfo.As.PROPERTY)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = DuplicateKind1.class, name = "kind_1"),
        @JsonSubTypes.Type(value = DuplicateKind2.class, name = "kind_2"),
    })
    public static abstract class DuplicateKind {
//        public String kind;
    }

    public static class DuplicateKind1 extends DuplicateKind {
        public String kind;
    }

    public static class DuplicateKind2 extends DuplicateKind {
    }

    @Test
    public void testJacksonDuplicateProperty() throws JsonProcessingException {
        final DuplicateKind1 object = new DuplicateKind1();
        object.kind = "kind_invalid";
        final String json = new ObjectMapper().writeValueAsString(object);
        // {"kind":"kind_1","kind":"kind_invalid"}
        Assertions.assertTrue(json.contains("\"kind\":\"kind_1\""));
        Assertions.assertTrue(json.contains("\"kind\":\"kind_invalid\""));
    }

    @Test
    public void testDuplicateProperty() throws JsonProcessingException {
        final Settings settings = TestUtils.settings();
        settings.outputKind = TypeScriptOutputKind.module;
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.mapClasses = ClassMapping.asClasses;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(DuplicateKindUsage.class));
        Assertions.assertTrue(!output.contains("DuplicateKindUnion"));
    }

}
