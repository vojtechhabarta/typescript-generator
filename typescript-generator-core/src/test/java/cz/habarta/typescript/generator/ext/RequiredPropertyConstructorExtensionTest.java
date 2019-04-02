package cz.habarta.typescript.generator.ext;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import cz.habarta.typescript.generator.*;
import cz.habarta.typescript.generator.util.Utils;
import org.junit.Test;

import java.lang.reflect.Type;

import static org.junit.Assert.assertEquals;

public class RequiredPropertyConstructorExtensionTest {

    static class SimpleClass {
        public String field1;
        public PolymorphicClass field2;
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "class")
    interface SuperInterface {

    }

    @JsonTypeName("class-b")
    static class PolymorphicClass implements SuperInterface {
        public int field1;
    }

    @Test
    public void testBasicWithReadOnly() {
        Settings settings = createBaseSettings();
        settings.declarePropertiesAsReadOnly = true;
        String result = generateTypeScript(settings, SimpleClass.class);

        String expected = readResource("/ext/RequiredPropertyConstructorExtensionTest-basicWithReadOnly.ts");
        assertEquals(expected, result);
    }

    @Test
    public void testBasicWithoutReadOnly() {
        Settings settings = createBaseSettings();
        settings.declarePropertiesAsReadOnly = false;
        String result = generateTypeScript(settings, SimpleClass.class);

        String expected = readResource("/ext/RequiredPropertyConstructorExtensionTest-basicWithoutReadOnly.ts");
        assertEquals(expected, result);
    }

    private static String generateTypeScript(Settings settings, Type... types) {
        TypeScriptGenerator typeScriptGenerator = new TypeScriptGenerator(settings);
        return typeScriptGenerator.generateTypeScript(Input.from(types));
    }

    private static Settings createBaseSettings() {
        Settings settings = new Settings();
        settings.sortDeclarations = true;
        settings.extensions.add(new RequiredPropertyConstructorExtension());
        settings.jsonLibrary = JsonLibrary.jackson2;
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.outputKind = TypeScriptOutputKind.module;
        settings.mapClasses = ClassMapping.asClasses;
        settings.noFileComment = true;
        return settings;
    }

    private String readResource(String name) {
        return Utils.readString(getClass().getResourceAsStream(name), "\n");
    }
}
