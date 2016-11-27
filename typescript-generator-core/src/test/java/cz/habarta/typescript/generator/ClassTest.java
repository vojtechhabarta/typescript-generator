
package cz.habarta.typescript.generator;

import org.junit.Assert;
import org.junit.Test;


public class ClassTest {

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        settings.classType = ClassType.asClass;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Book.class));
        Assert.assertTrue(output.contains("class Book"));
        Assert.assertTrue(output.contains("title: string;"));
        Assert.assertTrue(output.contains("class Author"));
    }

    static interface Book {
        Author getAuthor();

        String getTitle();
    }

    static interface Author {
        String getName();
    }

}
