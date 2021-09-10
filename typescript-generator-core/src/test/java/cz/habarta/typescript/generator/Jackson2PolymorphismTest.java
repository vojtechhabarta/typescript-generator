
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class Jackson2PolymorphismTest {

    @Test
    public void testPropertyNameQuoting() {
        final String output = new TypeScriptGenerator(TestUtils.settings()).generateTypeScript(Input.from(BadFieldClass.class));
        Assertions.assertTrue(output.contains("\"@class\""));
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
        Assertions.assertTrue(z < x);
        Assertions.assertTrue(z < y);
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
