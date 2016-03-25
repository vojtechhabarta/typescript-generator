
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

    static interface Book {
        Author getAuthor();
        String getTitle();
    }

    static interface Author {
        String getName();
    }

}
