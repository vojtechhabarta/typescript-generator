package cz.habarta.typescript.generator;

import static org.junit.Assert.assertEquals;

import java.io.StringWriter;

import org.junit.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import cz.habarta.typescript.generator.ext.TypeGuardsForJackson2PolymorphismExtension;


public class BadFieldNameTest {

    @Test
    public void badFieldName() {
        final Settings settings = TestUtils.settings();
        settings.customTypeProcessor = new GenericsTypeProcessor();
        settings.sortDeclarations = true;
        settings.extensions.add(new TypeGuardsForJackson2PolymorphismExtension());

        final StringWriter stringWriter = new StringWriter();
        new TypeScriptGenerator(settings).generateEmbeddableTypeScript(Input.from(BadFieldClass.class), Output.to(stringWriter), true, 0);
        final String actual = stringWriter.toString().trim();
        final String nl = settings.newline;
        final String expected =
                "export interface BadFieldClass {" + nl +
                "    \"@class\"?: string;" + nl +
                "}";
        System.out.println(actual);
        assertEquals(expected, actual);


    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS, include = JsonTypeInfo.As.PROPERTY, property = "@class")
    public static interface BadFieldClass {
    }
}
