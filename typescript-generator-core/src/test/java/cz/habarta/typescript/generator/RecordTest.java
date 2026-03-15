
package cz.habarta.typescript.generator;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class RecordTest {

    public record Account(Long id, String name) {}

    public record GenericRecord<T>(T value, String label) {}

    public record NestedRecord(Account account, String description) {}

    @Test
    public void testRecordInterface() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Account.class));
        Assertions.assertTrue(output.contains("interface Account"), "Output must contain 'interface Account'");
        Assertions.assertTrue(output.contains("id"), "Output must contain 'id' property");
        Assertions.assertTrue(output.contains("name"), "Output must contain 'name' property");
    }

    @Test
    public void testRecordClass() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.mapClasses = ClassMapping.asClasses;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Account.class));
        Assertions.assertTrue(output.contains("class Account"), "Output must contain 'class Account'");
        Assertions.assertTrue(output.contains("id"), "Output must contain 'id' property");
        Assertions.assertTrue(output.contains("name"), "Output must contain 'name' property");
    }

    @Test
    public void testRecordConstructor() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.mapClasses = ClassMapping.asClasses;
        settings.generateConstructors = true;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Account.class));
        Assertions.assertTrue(output.contains("class Account"), "Output must contain 'class Account'");
        Assertions.assertTrue(output.contains("id"), "Output must contain 'id' property");
        Assertions.assertTrue(output.contains("name"), "Output must contain 'name' property");
    }

    @Test
    public void testRecordDoesNotExtendRecord() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(Account.class));
        Assertions.assertFalse(output.contains("extends Record"), "Record type must not extend 'Record'");
        Assertions.assertTrue(output.contains("interface Account"), "Output must contain 'interface Account'");
    }

    @Test
    public void testNestedRecord() {
        final Settings settings = TestUtils.settings();
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(NestedRecord.class));
        Assertions.assertTrue(output.contains("account"), "Output must contain 'account' property");
        Assertions.assertTrue(output.contains("description"), "Output must contain 'description' property");
        Assertions.assertTrue(output.contains("Account"), "Output must contain 'Account' type reference");
    }

    @Test
    public void testRecordDiscoveredViaClassNamePattern() {
        final Settings settings = TestUtils.settings();
        final Input.Parameters parameters = new Input.Parameters();
        parameters.classNamePatterns = Arrays.asList(Account.class.getName());
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(parameters));
        Assertions.assertTrue(output.contains("interface Account"), "Output must contain 'interface Account' when discovered via classNamePatterns");
        Assertions.assertTrue(output.contains("id"), "Output must contain 'id' property when discovered via classNamePatterns");
        Assertions.assertTrue(output.contains("name"), "Output must contain 'name' property when discovered via classNamePatterns");
    }

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
        Assertions.assertTrue(output.contains("class Derived"), "Output must contain 'class Derived'");
    }

}
