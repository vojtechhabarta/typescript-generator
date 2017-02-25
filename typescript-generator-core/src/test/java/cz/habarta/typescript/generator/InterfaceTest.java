
package cz.habarta.typescript.generator;

import org.junit.Assert;
import org.junit.Test;


public class InterfaceTest {

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Book.class));
        Assert.assertTrue(output.contains("interface Book"));
        Assert.assertTrue(output.contains("title: string;"));
        Assert.assertTrue(output.contains("interface Author"));
    }

    @Test
    public void testReadOnlyProperties() {
        final Settings settings = TestUtils.settings();
        settings.declarePropertiesAsReadOnly = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Book.class));
        Assert.assertTrue(output.contains("readonly author: Author;"));
        Assert.assertTrue(output.contains("readonly title: string;"));
        Assert.assertTrue(output.contains("readonly name: string;"));
    }

    static interface Book {
        Author getAuthor();
        String getTitle();
    }

    static interface Author {
        String getName();
    }

}
