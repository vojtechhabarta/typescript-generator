
package cz.habarta.typescript.generator;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ClassInfoList;
import io.github.classgraph.ScanResult;

import java.util.*;

import org.junit.Assert;
import org.junit.Test;


public class InputTest {

    @Test
    public void testScanner() {
        try (ScanResult scanResult =
                     new ClassGraph()
//                                 .verbose()             // Enable verbose logging
                             .enableAllInfo()       // Scan classes, methods, fields, annotations
                             .scan()) {
            final ClassInfoList testClassNames = Input.filterClassNames(scanResult.getAllClasses(), Arrays.asList("cz.habarta.typescript.generator.**Test"));
            Assert.assertTrue("Typescript-generator must have at least 20 tests :-)", testClassNames.size() > 20);
        }
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

}
