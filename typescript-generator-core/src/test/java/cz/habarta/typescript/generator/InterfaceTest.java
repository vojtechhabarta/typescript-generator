
package cz.habarta.typescript.generator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class InterfaceTest {

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Book.class));
        Assertions.assertTrue(output.contains("interface Book"));
        Assertions.assertTrue(output.contains("title: string;"));
        Assertions.assertTrue(output.contains("interface Author"));
    }

    @Test
    public void testReadOnlyProperties() {
        final Settings settings = TestUtils.settings();
        settings.declarePropertiesAsReadOnly = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Book.class));
        Assertions.assertTrue(output.contains("readonly author: Author;"));
        Assertions.assertTrue(output.contains("readonly title: string;"));
        Assertions.assertTrue(output.contains("readonly name: string;"));
    }

    static interface Book {
        Author getAuthor();
        String getTitle();
    }

    static interface Author {
        String getName();
    }

}
