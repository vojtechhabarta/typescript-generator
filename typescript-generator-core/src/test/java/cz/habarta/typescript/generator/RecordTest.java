
package cz.habarta.typescript.generator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class RecordTest {

    public record Account(Long id, String name) {}

    @Test
    public void test() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Account.class));
        Assertions.assertTrue(!output.contains("interface Record"));
        Assertions.assertTrue(output.contains("id: number"));
        Assertions.assertTrue(output.contains("name: string"));
    }

    @Test
    public void testConstructor() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.mapClasses = ClassMapping.asClasses;
        settings.generateConstructors = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Account.class));
        Assertions.assertTrue(!output.contains("interface Record"));
        Assertions.assertTrue(!output.contains("super"));
        Assertions.assertTrue(output.contains("constructor(data: Account)"));
    }

}
