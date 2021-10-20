
package cz.habarta.typescript.generator.compiler;

import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class ModelCompilerUtilsTest {

    @Test
    public void testSplitIdentifierIntoWords() {
        Assertions.assertEquals("Red", splitIdentifierIntoWords("Red"));
        Assertions.assertEquals("ATYPE", splitIdentifierIntoWords("ATYPE"));
        Assertions.assertEquals("camel Case Type", splitIdentifierIntoWords("camelCaseType"));
        Assertions.assertEquals("Pascal Case Type", splitIdentifierIntoWords("PascalCaseType"));
        Assertions.assertEquals("UPPER CASE TYPE", splitIdentifierIntoWords("UPPER_CASE_TYPE"));
        Assertions.assertEquals("XML Http Request", splitIdentifierIntoWords("XMLHttpRequest"));
        Assertions.assertEquals("HÁČKY A ČÁRKY", splitIdentifierIntoWords("HÁČKY_A_ČÁRKY"));
        Assertions.assertEquals("Háčky A Čárky", splitIdentifierIntoWords("HáčkyAČárky"));
        Assertions.assertEquals("String 2 Json", splitIdentifierIntoWords("String2Json"));
        Assertions.assertEquals("string 2 json", splitIdentifierIntoWords("string2json"));
        Assertions.assertEquals("version 42 final", splitIdentifierIntoWords("version42final"));
    }

    private static String splitIdentifierIntoWords(String identifier) {
        return ModelCompiler.splitIdentifierIntoWords(identifier).stream().collect(Collectors.joining(" "));
    }

}
