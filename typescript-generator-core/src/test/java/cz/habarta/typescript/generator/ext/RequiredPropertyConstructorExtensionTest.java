package cz.habarta.typescript.generator.ext;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import cz.habarta.typescript.generator.ClassMapping;
import cz.habarta.typescript.generator.Input;
import cz.habarta.typescript.generator.JsonLibrary;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TypeScriptFileType;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import cz.habarta.typescript.generator.TypeScriptOutputKind;
import cz.habarta.typescript.generator.util.Utils;
import java.lang.reflect.Type;
import org.junit.Assert;
import org.junit.Test;

public class RequiredPropertyConstructorExtensionTest {

    private static final String BASE_PATH = "/ext/RequiredPropertyConstructorExtensionTest-";

    static class SimpleClass {
        public String field1;
        public PolymorphicClass field2;
    }

    static class MultipleEnumContainerClass {
        public MultipleEntryEnum multiple;
    }

    enum MultipleEntryEnum {
        ENTRY_1,
        ENTRY_2,
        ENTRY_3
    }

    static class SingleEnumContainerClass {
        public SingleEntryEnum single;
    }

    enum SingleEntryEnum {
        ENTRY_1
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "discriminator")
    interface SuperInterface {

    }

    @JsonTypeName("class-b")
    static class PolymorphicClass implements SuperInterface {
        public int field1;
    }

    @JsonTypeName("class-c")
    static class SecondClass extends PolymorphicClass {
        public int field2;
    }

    @Test
    public void testBasicWithReadOnly() {
        Settings settings = createBaseSettings();
        settings.declarePropertiesAsReadOnly = true;
        String result = generateTypeScript(settings, SimpleClass.class);

        String expected = readResource("basicWithReadOnly.ts");

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testBasicWithoutReadOnly() {
        Settings settings = createBaseSettings();
        settings.declarePropertiesAsReadOnly = false;
        String result = generateTypeScript(settings, SimpleClass.class);

        String expected = readResource("basicWithoutReadOnly.ts");

        Assert.assertEquals(expected, result);
    }

    @Test
    public void testEnums() {
        Settings settings = createBaseSettings();
        settings.declarePropertiesAsReadOnly = true;

        String result = generateTypeScript(settings, MultipleEnumContainerClass.class, SingleEnumContainerClass.class);

        String expected = readResource("enums.ts");
        Assert.assertEquals(expected, result);
    }

    @Test
    public void testInheritance() {
        Settings settings = createBaseSettings();
        settings.declarePropertiesAsReadOnly = true;

        try {
            generateTypeScript(settings, SecondClass.class);
            Assert.fail("Expected exception");
        }
        catch (IllegalStateException expected) {
            Assert.assertEquals("Creating constructors for inherited beans is not currently supported", expected.getMessage());
        }
    }

    private static String generateTypeScript(Settings settings, Type... types) {
        TypeScriptGenerator typeScriptGenerator = new TypeScriptGenerator(settings);
        String result = typeScriptGenerator.generateTypeScript(Input.from(types));
        return Utils.normalizeLineEndings(result, "\n");
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

    private String readResource(String suffix) {
        return Utils.readString(getClass().getResourceAsStream(BASE_PATH + suffix), "\n");
    }
}
