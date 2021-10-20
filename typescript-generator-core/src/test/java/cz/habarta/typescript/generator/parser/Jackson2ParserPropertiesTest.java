
package cz.habarta.typescript.generator.parser;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.databind.BeanProperty;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TestUtils;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class Jackson2ParserPropertiesTest {
    
    @JsonPropertyOrder({"password1", "id2"})
    public static class UserOrdered {
        public String name;
        public String id1;
        public String id2;
        public String password1;
        public String password2;
    }

    @JsonPropertyOrder(alphabetic = true)
    public static class UserAlphabetic {
        public String name;
        public String id1;
        public String id2;
        public String password1;
        public String password2;
    }

    public static class UserIndexed {
        @JsonProperty(index = 5) public String name;
        @JsonProperty(index = 4) public String id1;
        @JsonProperty(index = 3) public String id2;
        @JsonProperty(index = 2) public String password1;
        @JsonProperty(index = 1) public String password2;
    }

    public static class User1 {
        @JsonProperty(access = JsonProperty.Access.READ_WRITE) public String name;
        @JsonProperty(access = JsonProperty.Access.READ_WRITE) public String id1;
        @JsonProperty(access = JsonProperty.Access.READ_WRITE) public String id2;
        @JsonProperty(access = JsonProperty.Access.READ_WRITE) public String password1;
        @JsonProperty(access = JsonProperty.Access.READ_WRITE) public String password2;
    }

    public static class User2 {
        @JsonProperty(access = JsonProperty.Access.READ_WRITE) public String name;
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)  public String id1;
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)  public String id2;
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) public String password1;
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) public String password2;
    }

    public static class User3 {
        @JsonProperty(access = JsonProperty.Access.READ_WRITE) public String name;
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) public String password1;
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) public String password2;
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)  public String id1;
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)  public String id2;
    }

    public static class User4 {
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) public String password1;
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)  public String id1;
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY) public String password2;
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)  public String id2;
        @JsonProperty(access = JsonProperty.Access.READ_WRITE) public String name;
    }

    @Test
    public void testPropertyOrder() {
        Assertions.assertEquals(Arrays.asList("password1", "id2", "name", "id1", "password2"), getProperties(UserOrdered.class));
        Assertions.assertEquals(Arrays.asList("id1", "id2", "name", "password1", "password2"), getProperties(UserAlphabetic.class));
        Assertions.assertEquals(Arrays.asList("password2", "password1", "id2", "id1", "name"), getProperties(UserIndexed.class));
        Assertions.assertEquals(Arrays.asList("name", "id1", "id2", "password1", "password2"), getProperties(User1.class));
        Assertions.assertEquals(Arrays.asList("name", "id1", "id2", "password1", "password2"), getProperties(User2.class));
        Assertions.assertEquals(Arrays.asList("name", "password1", "password2", "id1", "id2"), getProperties(User3.class));
        Assertions.assertEquals(Arrays.asList("password1", "id1", "password2", "id2", "name"), getProperties(User4.class));
    }

    private List<String> getProperties(Class<?> beanClass) {
        final Settings settings = TestUtils.settings();
        final TypeScriptGenerator typeScriptGenerator = new TypeScriptGenerator(settings);
        final Jackson2Parser jackson2Parser = (Jackson2Parser) typeScriptGenerator.getModelParser();
        final List<BeanProperty> properties = jackson2Parser.getBeanProperties(beanClass);
        final List<String> names = properties.stream().map(BeanProperty::getName).collect(Collectors.toList());
        return names;
    }

}
