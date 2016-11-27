
package cz.habarta.typescript.generator;

import java.util.*;
import org.junit.Assert;
import org.junit.Test;


public class CovariantPropertiesTest {

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        settings.sortDeclarations = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Dog.class));
        final String expected =
                "interface Animal {\n" +
                "    allFood: Food[];\n" +
                "    todaysFood: Food;\n" +
                "}\n" +
                "\n" +
                "interface Dog extends Animal {\n" +
                "    allFood: DogFood[];\n" +
                "    todaysFood: DogFood;\n" +
                "}\n" +
                "\n" +
                "interface DogFood extends Food {\n" +
                "}\n" +
                "\n" +
                "interface Food {\n" +
                "}";
        Assert.assertEquals(expected.replace('\'', '"'), output.trim());
    }

    private static abstract class Animal {
        public abstract Food getTodaysFood();
        public abstract List<? extends Food> getAllFood();
    }

    private static abstract class Dog extends Animal {
        @Override
        public abstract DogFood getTodaysFood();
        @Override
        public abstract List<? extends DogFood> getAllFood();
    }

    private static abstract class Food {
    }

    private static abstract class DogFood extends Food {
    }

}
