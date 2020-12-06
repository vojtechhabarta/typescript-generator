
package cz.habarta.typescript.generator.compiler;

import java.util.stream.Collectors;
import org.junit.Assert;
import org.junit.Test;


public class ModelCompilerUtilsTest {

    @Test
    public void testSplitIdentifierIntoWords() {
        Assert.assertEquals("Red", splitIdentifierIntoWords("Red"));
        Assert.assertEquals("ATYPE", splitIdentifierIntoWords("ATYPE"));
        Assert.assertEquals("camel Case Type", splitIdentifierIntoWords("camelCaseType"));
        Assert.assertEquals("Pascal Case Type", splitIdentifierIntoWords("PascalCaseType"));
        Assert.assertEquals("UPPER CASE TYPE", splitIdentifierIntoWords("UPPER_CASE_TYPE"));
        Assert.assertEquals("XML Http Request", splitIdentifierIntoWords("XMLHttpRequest"));
        Assert.assertEquals("HÁČKY A ČÁRKY", splitIdentifierIntoWords("HÁČKY_A_ČÁRKY"));
        Assert.assertEquals("Háčky A Čárky", splitIdentifierIntoWords("HáčkyAČárky"));
        Assert.assertEquals("String 2 Json", splitIdentifierIntoWords("String2Json"));
        Assert.assertEquals("string 2 json", splitIdentifierIntoWords("string2json"));
        Assert.assertEquals("version 42 final", splitIdentifierIntoWords("version42final"));
    }

    private static String splitIdentifierIntoWords(String identifier) {
        return ModelCompiler.splitIdentifierIntoWords(identifier).stream().collect(Collectors.joining(" "));
    }

}
