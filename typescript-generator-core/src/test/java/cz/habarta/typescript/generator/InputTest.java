
package cz.habarta.typescript.generator;

import io.github.lukehutch.fastclasspathscanner.FastClasspathScanner;
import io.github.lukehutch.fastclasspathscanner.scanner.ScanResult;
import java.util.*;
import org.junit.Assert;
import org.junit.Test;


public class InputTest {

    @Test
    public void testScanner() {
        final ScanResult scanResult = new FastClasspathScanner().scan();
        final List<String> allClassNames = scanResult.getNamesOfAllClasses();
        final List<String> testClassNames = Input.filterClassNames(allClassNames, Arrays.asList("cz.habarta.typescript.generator.**Test"));
        Assert.assertTrue("Typescript-generator must have at least 20 tests :-)", testClassNames.size() > 20);
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
        Assert.assertTrue(result1.contains("com.example.Json"));
        Assert.assertTrue(result1.contains("com.example.AAAJson"));
        Assert.assertTrue(!result1.contains("com.example.AAA"));
        Assert.assertTrue(result1.contains("com.example.aaa$Json"));

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
        Assert.assertTrue(!result2.contains("com.example.Json"));
        Assert.assertTrue(result2.contains("cz.habarta.test.Json"));
        Assert.assertTrue(result2.contains("cz.habarta.test.BBBJson"));
        Assert.assertTrue(!result2.contains("cz.habarta.test.aaa.BBBJson"));
        Assert.assertTrue(!result2.contains("cz.habarta.test.CCC$Json"));

        final List<String> result3 = Input.filterClassNames(
                Arrays.asList(
                        "cz.habarta.test.BBBJson",
                        "cz.habarta.ddd.CCC$Json",
                        "cz.habarta.CCC$Json"
                ),
                Arrays.asList("cz.habarta.*.*$*")
        );
        Assert.assertTrue(!result3.contains("cz.habarta.test.BBBJson"));
        Assert.assertTrue(result3.contains("cz.habarta.ddd.CCC$Json"));
        Assert.assertTrue(!result3.contains("cz.habarta.CCC$Json"));
    }

    @Test
    public void testGlobToRegexp() {
        Assert.assertEquals("\\Q\\E.*\\QJson\\E", Input.globToRegexp("**Json").toString());
        Assert.assertEquals("\\Qcz.habarta.test.\\E[^.\\$]*\\Q\\E", Input.globToRegexp("cz.habarta.test.*").toString());
    }

}
