
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonInclude;
import cz.habarta.typescript.generator.util.Utils;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


public class JacksonPrettyPrintTest {

    @Test
    public void testJacksonPrettyPrint() throws Exception {
        final var objectMapper = Utils.getObjectMapper().rebuild()
            .changeDefaultPropertyInclusion(v -> JsonInclude.Value.ALL_ALWAYS)
            .build();
        final var json = objectMapper.writeValueAsString(new JsonPrettyPrintData());
        assertThat(json).isEqualTo(expectedJson);
    }

    @Test
    public void testJackson2PrettyPrint() throws Exception {
        final var objectMapper = Utils.getObjectMapperJ2()
            .setDefaultPropertyInclusion(JsonInclude.Include.ALWAYS);
        final var json = objectMapper.writeValueAsString(new JsonPrettyPrintData());
        assertThat(json).isEqualTo(expectedJson);
    }

    @SuppressWarnings("unused")
    private static class JsonPrettyPrintData {
        public String string1 = "string1";
        public int number2 = 123;
        public boolean boolean3 = true;
        public @Nullable Object null4 = null;
        public List<String> list5 = List.of("one", "two", "three");
        public Map<String, String> map6 = Utils.mapOf("key1", "value1", "key2", "value2");
        public List<String> emptyList7 = List.of();
        public Map<String, String> emptyMap8 = Utils.mapOf();
        public List<Map<String, String>> listOfMaps9 = List.of(
            Utils.mapOf("key1", "value1", "key2", "value2"),
            Utils.mapOf("key3", "value3", "key4", "value4")
        );
        public Map<String, List<String>> mapOfLists10 = Utils.mapOf(
            "key1", List.of("value1", "value2"),
            "key2", List.of("value3", "value4")
        );
    }

    private static String expectedJson = """
        {
          "string1": "string1",
          "number2": 123,
          "boolean3": true,
          "null4": null,
          "list5": [
            "one",
            "two",
            "three"
          ],
          "map6": {
            "key1": "value1",
            "key2": "value2"
          },
          "emptyList7": [],
          "emptyMap8": {},
          "listOfMaps9": [
            {
              "key1": "value1",
              "key2": "value2"
            },
            {
              "key3": "value3",
              "key4": "value4"
            }
          ],
          "mapOfLists10": {
            "key1": [
              "value1",
              "value2"
            ],
            "key2": [
              "value3",
              "value4"
            ]
          }
        }""";

}
