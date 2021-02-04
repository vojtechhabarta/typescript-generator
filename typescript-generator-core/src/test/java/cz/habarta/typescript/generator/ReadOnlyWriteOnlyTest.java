
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.google.gson.Gson;
import org.junit.Assert;
import org.junit.Test;


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
        Assert.assertTrue(json.contains("id1"));
        Assert.assertTrue(json.contains("id2"));
        Assert.assertTrue(!json.contains("password1"));
        Assert.assertTrue(!json.contains("password2"));
    }

    @Test
    public void testJacksonDeserialization() throws JsonProcessingException {
        final String json = "{'name':'name','id1':'id1','id2':'id2','password1':'password1','password2':'password2'}"
                .replace("'", "\"");
        final ReadOnlyWriteOnlyUser user = new ObjectMapper().readValue(json, ReadOnlyWriteOnlyUser.class);
        Assert.assertNull(user.id1);
        Assert.assertNull(user._id2);
        Assert.assertNotNull(user.password1);
        Assert.assertNotNull(user.password2);
    }

    @Test
    public void test() throws JsonProcessingException {
        final Settings settings = TestUtils.settings();
        settings.generateReadonlyAndWriteonlyJSDocTags = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(ReadOnlyWriteOnlyUser.class));
        final String expected = "\n"
                + "interface ReadOnlyWriteOnlyUser {\n"
                + "    name: string;\n"
                + "    /**\n"
                + "     * @readonly\n"
                + "     */\n"
                + "    id1: string;\n"
                + "    /**\n"
                + "     * @writeonly\n"
                + "     */\n"
                + "    password1: string;\n"
                + "    /**\n"
                + "     * @readonly\n"
                + "     */\n"
                + "    id2: string;\n"
                + "    /**\n"
                + "     * @writeonly\n"
                + "     */\n"
                + "    password2: string;\n"
                + "}\n";
        ObjectMapper mapper = JsonMapper.builder()
                .nodeFactory(new SortingNodeFactory())
                .build();
        Assert.assertEquals(mapper.writeValueAsString(mapper.readTree(expected)), mapper.writeValueAsString(mapper.readTree(output)));
    }

}
