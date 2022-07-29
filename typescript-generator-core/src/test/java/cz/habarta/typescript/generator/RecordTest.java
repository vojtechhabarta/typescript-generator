
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
     }

    @Test
    public void testAsClassWithConstructors() {
        final Settings settings = settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Account.class));
        var expected = "export class Account {\n" +
            "    id: number;\n" +
            "    name: string;\n" +
            "\n" +
            "    constructor(data: Account) {\n" +
            "        this.id = data.id;\n" +
            "        this.name = data.name;\n" +
            "    }\n" +
        "}";

        Assertions.assertEquals(expected.replace('\'', '"'), output.trim());
    }

    public static Settings settings() {
        final Settings settings = TestUtils.settings();
        settings.outputKind = TypeScriptOutputKind.module;
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.mapClasses = ClassMapping.asClasses;
        settings.generateConstructors = true;
        return settings;
    }

}
