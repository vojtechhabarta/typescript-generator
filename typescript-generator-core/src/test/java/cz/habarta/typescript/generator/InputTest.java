
package cz.habarta.typescript.generator;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


@SuppressWarnings("unused")
public class InputTest {

    @Test
    public void testScanner() {
        final ScanResult scanResult = new ClassGraph().enableAllInfo().acceptPackages("cz.habarta").scan();
        final List<String> allClassNames = scanResult.getAllClasses().getNames();
        final List<String> testClassNames = Input.filterClassNames(allClassNames, Arrays.asList("cz.habarta.typescript.generator.**Test"));
        Assertions.assertTrue(testClassNames.size() > 20, "Typescript-generator must have at least 20 tests :-)");
    }

    @Test
    public void testMatches() {
        final List<String> result1 = Input.filterClassNames(
                Arrays.asList(
                        "com.example.Json",
                        "com.example.AAAJson",
                        "com.example.AAA",
                        "com.example.aaa$Json"
                ),
                Arrays.asList("**Json")
        );
        Assertions.assertTrue(result1.contains("com.example.Json"));
        Assertions.assertTrue(result1.contains("com.example.AAAJson"));
        Assertions.assertTrue(!result1.contains("com.example.AAA"));
        Assertions.assertTrue(result1.contains("com.example.aaa$Json"));

        final List<String> result2 = Input.filterClassNames(
                Arrays.asList(
                        "com.example.Json",
                        "cz.habarta.test.Json",
                        "cz.habarta.test.BBBJson",
                        "cz.habarta.test.aaa.BBBJson",
                        "cz.habarta.test.CCC$Json"
                ),
                Arrays.asList("cz.habarta.test.*")
        );
        Assertions.assertTrue(!result2.contains("com.example.Json"));
        Assertions.assertTrue(result2.contains("cz.habarta.test.Json"));
        Assertions.assertTrue(result2.contains("cz.habarta.test.BBBJson"));
        Assertions.assertTrue(!result2.contains("cz.habarta.test.aaa.BBBJson"));
        Assertions.assertTrue(!result2.contains("cz.habarta.test.CCC$Json"));

        final List<String> result3 = Input.filterClassNames(
                Arrays.asList(
                        "cz.habarta.test.BBBJson",
                        "cz.habarta.ddd.CCC$Json",
                        "cz.habarta.CCC$Json"
                ),
                Arrays.asList("cz.habarta.*.*$*")
        );
        Assertions.assertTrue(!result3.contains("cz.habarta.test.BBBJson"));
        Assertions.assertTrue(result3.contains("cz.habarta.ddd.CCC$Json"));
        Assertions.assertTrue(!result3.contains("cz.habarta.CCC$Json"));
    }

    @Test
    public void testClassesWithAnnotations() {
        final Input.Parameters parameters = new Input.Parameters();
        parameters.classesWithAnnotations = Arrays.asList(MyJsonClass.class.getName());
        parameters.scanningAcceptedPackages = Arrays.asList("cz.habarta");
        final String output = new TypeScriptGenerator(TestUtils.settings()).generateTypeScript(Input.from(parameters));
        Assertions.assertTrue(output.contains("name: string;"));
    }

    @Test
    public void testClassesImplementingInterfaces() {
        final Input.Parameters parameters = new Input.Parameters();
        parameters.classesImplementingInterfaces = Arrays.asList(MyJsonInterface.class.getName());
        final String output = new TypeScriptGenerator(TestUtils.settings()).generateTypeScript(Input.from(parameters));
        Assertions.assertTrue(output.contains("firstName: string;"));
        Assertions.assertTrue(output.contains("lastName: string;"));
    }

    @Test
    public void testClassesExtendingClasses() {
        final Input.Parameters parameters = new Input.Parameters();
        parameters.classesExtendingClasses = Arrays.asList(MyJsonInterfaceImpl.class.getName());
        final String output = new TypeScriptGenerator(TestUtils.settings()).generateTypeScript(Input.from(parameters));
        Assertions.assertTrue(output.contains("lastName: string;"));
    }

    @Retention(RetentionPolicy.RUNTIME)
    private static @interface MyJsonClass {
    }

    private interface MyJsonInterface {
    }

    private static class MyJsonInterfaceImpl implements MyJsonInterface {
        public String firstName;
    }

    private static class MyJsonInterfaceSubclass extends MyJsonInterfaceImpl {
        public String lastName;
    }

    @MyJsonClass
    private static class MyData {
        public String name;
    }

}
