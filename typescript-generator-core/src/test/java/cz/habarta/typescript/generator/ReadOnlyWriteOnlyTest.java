
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class ReadOnlyWriteOnlyTest {
    
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ReadOnlyWriteOnlyUser {

        public String name;

        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        public String id1;

        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        public String password1;

        private String _id2;  // Jackson would use `id2` field as writable property

        public String getId2() {
            return _id2;
        }

        private String password2;

        public void setPassword2(String password2) {
            this.password2 = password2;
        }

        @Override
        public String toString() {
            return new Gson().toJson(this);
        }

    }

    @Test
    public void testJacksonSerialization() throws JsonProcessingException {
        final ReadOnlyWriteOnlyUser user = new ReadOnlyWriteOnlyUser();
        user.name = "name";
        user.id1 = "id1";
        user._id2 = "id2";
        user.password1 = "password1";
        user.password2 = "password2";
        final String json = new ObjectMapper().writeValueAsString(user);
        Assertions.assertTrue(json.contains("id1"));
        Assertions.assertTrue(json.contains("id2"));
        Assertions.assertTrue(!json.contains("password1"));
        Assertions.assertTrue(!json.contains("password2"));
    }

    @Test
    public void testJacksonDeserialization() throws JsonProcessingException {
        final String json = "{'name':'name','id1':'id1','id2':'id2','password1':'password1','password2':'password2'}"
                .replace("'", "\"");
        final ReadOnlyWriteOnlyUser user = new ObjectMapper().readValue(json, ReadOnlyWriteOnlyUser.class);
        Assertions.assertNull(user.id1);
        Assertions.assertNull(user._id2);
        Assertions.assertNotNull(user.password1);
        Assertions.assertNotNull(user.password2);
    }

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        settings.generateReadonlyAndWriteonlyJSDocTags = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ReadOnlyWriteOnlyUser.class));
        String expected1 = "    name: string;\n";
        String expected2 = "    /**\n" + "     * @readonly\n" + "     */\n" + "    id1: string;\n";
        String expected3 = "    /**\n" + "     * @writeonly\n" + "     */\n" + "    password1: string;\n";
        String expected4 = "    /**\n" + "     * @readonly\n" + "     */\n" + "    id2: string;\n";
        String expected5 = "    /**\n" + "     * @writeonly\n" + "     */\n" + "    password2: string;\n";
        Assertions.assertTrue(
            output.length() == 269 && 
            output.substring(0,35).equals("\n" + "interface ReadOnlyWriteOnlyUser {\n") && 
            output.substring(267,269).equals("}\n") && 
            output.contains(expected1) && 
            output.contains(expected2) && 
            output.contains(expected3) && 
            output.contains(expected4) && 
            output.contains(expected5));
    }

}
