
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.junit.Assert;
import org.junit.Test;


public class Jackson2PolymorphismTest {

    @Test
    public void test() {
        final String output = new TypeScriptGenerator(TestUtils.settings()).generateTypeScript(Input.from(BadFieldClass.class));
        Assert.assertTrue(output.contains("\"@class\""));
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static interface BadFieldClass {
    }

}
