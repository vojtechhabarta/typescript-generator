
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.junit.Assert;
import org.junit.Test;


public class Jackson2PolymorphismTest {

    @Test
    public void testPropertyNameQuoting() {
        final String output = new TypeScriptGenerator(TestUtils.settings()).generateTypeScript(Input.from(BadFieldClass.class));
        Assert.assertTrue(output.contains("\"@class\""));
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static interface BadFieldClass {
    }

    @Test
    public void testInterfaceOrder() {
        final String output = new TypeScriptGenerator(TestUtils.settings()).generateTypeScript(Input.from(Z.class));
        final int x = output.indexOf("interface X");
        final int y = output.indexOf("interface Y");
        final int z = output.indexOf("interface Z");
        Assert.assertTrue(z < x);
        Assert.assertTrue(z < y);
    }

    @JsonSubTypes({
            @JsonSubTypes.Type(Y.class),
            @JsonSubTypes.Type(X.class)
    })
    public static class Z {
    }

    public static class Y extends Z {
    }

    public static class X extends Z {
    }

}
