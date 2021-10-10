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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import javax.annotation.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class RequiredPropertyConstructorExtensionTest {

    private static final String BASE_PATH = "/ext/RequiredPropertyConstructorExtensionTest-";

    static class SimpleClass {
        public String field1;
        public PolymorphicClass field2;
    }

    static class OtherClass {
        public String field2;
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

    static class SecondClass extends SimpleClass {
        public int field3;
    }

    static class SimpleOptionalClass {
        public String field1;
        @Nullable
        public Integer field2;
    }

    static class SecondOptionalClass extends SimpleOptionalClass {
        public String field3;
    }

    @Test
    public void testBasicWithReadOnly() {
        Settings settings = createBaseSettings(new RequiredPropertyConstructorExtension());
        settings.declarePropertiesAsReadOnly = true;
        String result = generateTypeScript(settings, SimpleClass.class);

        String expected = readResource("basicWithReadOnly.ts");

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testBasicWithConfiguration() {
        RequiredPropertyConstructorExtension extension = new RequiredPropertyConstructorExtension();
        Map<String, String> configuration = new HashMap<>();
        String classes = SimpleClass.class.getCanonicalName() + " " + OtherClass.class.getCanonicalName();
        configuration.put(RequiredPropertyConstructorExtension.CFG_CLASSES, classes);
        extension.setConfiguration(configuration);

        Settings settings = createBaseSettings(extension);
        settings.declarePropertiesAsReadOnly = true;
        String result = generateTypeScript(settings, SimpleClass.class, OtherClass.class);

        String expected = readResource("basicWithConfiguration.ts");

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testBasicWithoutReadOnly() {
        Settings settings = createBaseSettings();
        settings.declarePropertiesAsReadOnly = false;
        String result = generateTypeScript(settings, SimpleClass.class);

        String expected = readResource("basicWithoutReadOnly.ts");

        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testEnums() {
        Settings settings = createBaseSettings();
        settings.declarePropertiesAsReadOnly = true;

        String result = generateTypeScript(settings, MultipleEnumContainerClass.class, SingleEnumContainerClass.class);

        String expected = readResource("enums.ts");
        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testInheritance() {
        Settings settings = createBaseSettings();
        settings.declarePropertiesAsReadOnly = true;

        String result = generateTypeScript(settings, SecondClass.class);

        String expected = readResource("inheritance.ts");
        Assertions.assertEquals(expected, result);
    }

    @Test
    public void testOptionalParameters() {
        Settings settings = createBaseSettings();
        settings.declarePropertiesAsReadOnly = true;
        settings.optionalAnnotations = new ArrayList<>();
        settings.optionalAnnotations.add(Nullable.class);

        String result = generateTypeScript(settings, SecondOptionalClass.class);

        String expected = readResource("optionalParameters.ts");
        Assertions.assertEquals(expected, result);
    }

    private static String generateTypeScript(Settings settings, Type... types) {
        TypeScriptGenerator typeScriptGenerator = new TypeScriptGenerator(settings);
        String result = typeScriptGenerator.generateTypeScript(Input.from(types));
        return Utils.normalizeLineEndings(result, "\n");
    }

    private static Settings createBaseSettings() {
        return createBaseSettings(new RequiredPropertyConstructorExtension());
    }

    private static Settings createBaseSettings(RequiredPropertyConstructorExtension extension) {
        Settings settings = new Settings();
        settings.sortDeclarations = true;
        settings.extensions.add(extension);
        settings.jsonLibrary = JsonLibrary.jackson2;
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.outputKind = TypeScriptOutputKind.module;
        settings.mapClasses = ClassMapping.asClasses;
        settings.noFileComment = true;
        settings.noEslintDisable = true;
        return settings;
    }

    private String readResource(String suffix) {
        return Utils.readString(getClass().getResourceAsStream(BASE_PATH + suffix), "\n");
    }
}
