package cz.habarta.typescript.generator.ext;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
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

public class OnePossiblePropertyValueAssigningExtensionTest {
    private static final String BASE_PATH = "/ext/OnePossiblePropertyValueAssigningExtensionTest-";

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "discriminator")
    static class BaseClass {

        @JsonProperty
        private Long field1;

        @JsonProperty
        private OneValueEnum field2;
    }

    static class SubClass extends BaseClass {

        @JsonProperty
        private String testField1;
    }

    static class OtherSubClass extends BaseClass {

        @JsonProperty
        private String testField2;

        @JsonProperty
        private OneValueEnum enumField1;

        @JsonProperty
        private TwoValueEnum enumField2;
    }

    enum OneValueEnum {
        MY_VALUE
    }

    enum TwoValueEnum {
        ONE,
        TWO
    }

    @Test
    public void testGeneration() {
        Settings settings = createBaseSettings(new OnePossiblePropertyValueAssigningExtension());
        String result = generateTypeScript(settings, SubClass.class, OtherSubClass.class);

        String expected = readResource("all.ts");

        Assert.assertEquals(expected, result);
    }

    private static Settings createBaseSettings(OnePossiblePropertyValueAssigningExtension extension) {
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

    private static String generateTypeScript(Settings settings, Type... types) {
        TypeScriptGenerator typeScriptGenerator = new TypeScriptGenerator(settings);
        String result = typeScriptGenerator.generateTypeScript(Input.from(types));
        return Utils.normalizeLineEndings(result, "\n");
    }

    private String readResource(String suffix) {
        return Utils.readString(getClass().getResourceAsStream(BASE_PATH + suffix), "\n");
    }

}