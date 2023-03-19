
package cz.habarta.typescript.generator;

import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class RecordTest {

    // public record Account(Long id, String name) {}

    // @Test
    // public void test() {
    //     final Settings settings = TestUtils.settings();
    //     final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Account.class));
    //     Assertions.assertTrue(!output.contains("interface Record"));
    // }

    // @Test
    // public void testConstructor() {
    //     final Settings settings = TestUtils.settings();
    //     settings.outputFileType = TypeScriptFileType.implementationFile;
    //     settings.mapClasses = ClassMapping.asClasses;
    //     settings.generateConstructors = true;
    //     final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Account.class));
    //     System.out.println(output);
    //     Assertions.assertTrue(!output.contains("interface Record"));
    // }

    private static class Base {
    }

    private static class Derived extends Base {
        public String name;
    }

    @Test
    public void testConstructorWithExclusion() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.mapClasses = ClassMapping.asClasses;
        settings.generateConstructors = true;
        settings.setExcludeFilter(List.of(Base.class.getName()), null);
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Derived.class));
        Assertions.assertTrue(!output.contains("interface Record"));
    }

}
