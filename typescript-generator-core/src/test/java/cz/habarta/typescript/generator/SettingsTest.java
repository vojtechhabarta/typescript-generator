
package cz.habarta.typescript.generator;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Modifier;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SettingsTest {
    /**
     * Checks if generic type arguments are parsed correctly, even when there are nested generic types.
     */
    @Test
    void testParseGenericName() {
        final var className = "Class";
        final String[] nonNestedGenericArgumentTypes = {"T1", "T2"};

        assertEquals(newGenericName(className, nonNestedGenericArgumentTypes), Settings.parseGenericName("Class<T1, T2>"));
        assertEquals(newGenericName(className, nonNestedGenericArgumentTypes), Settings.parseGenericName("Class[T1, T2]"));
        assertEquals(newGenericName(className, "T1[T2]", "T3"), Settings.parseGenericName("Class[T1[T2], T3]"));
        assertEquals(newGenericName(className, "T1<T2>", "T3"), Settings.parseGenericName("Class<T1<T2>, T3>"));
    }

    /**
     * Creates a new {@link Settings.GenericName} instance.
     * @param className name of a class that have generic type arguments.
     * @param genericArguments generic type arguments
     * @return a new {@link Settings.GenericName} instance.
     */
    private static Settings.GenericName newGenericName(final String className, final String ...genericArguments) {
        return new Settings.GenericName(className, Arrays.asList(genericArguments));
    }

    @Test
    public void testParseModifiers() {
        assertEquals(0, Settings.parseModifiers("", Modifier.fieldModifiers()));
        assertEquals(Modifier.STATIC, Settings.parseModifiers("static", Modifier.fieldModifiers()));
        assertEquals(Modifier.STATIC | Modifier.TRANSIENT, Settings.parseModifiers("static | transient", Modifier.fieldModifiers()));
    }

    @Test
    public void testNpmDependenciesValidation() {
        String exceptionMessage = "'npmDependencies', 'npmDevDependencies' and 'npmPeerDependencies' parameters are only applicable when generating NPM 'package.json'.";

        {
            Settings settings = new Settings();
            settings.outputKind = TypeScriptOutputKind.module;
            settings.jsonLibrary = JsonLibrary.jackson2;
            settings.generateNpmPackageJson = false;
            settings.npmPackageDependencies.put("dependencies", "version");

            RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> settings.validate());
            assertEquals(exceptionMessage, exception.getMessage());
        }

        {
            Settings settings = new Settings();
            settings.outputKind = TypeScriptOutputKind.module;
            settings.jsonLibrary = JsonLibrary.jackson2;
            settings.generateNpmPackageJson = false;
            settings.npmDevDependencies.put("dependencies", "version");

            RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> settings.validate());
            assertEquals(exceptionMessage, exception.getMessage());
        }

        {
            Settings settings = new Settings();
            settings.outputKind = TypeScriptOutputKind.module;
            settings.jsonLibrary = JsonLibrary.jackson2;
            settings.generateNpmPackageJson = false;
            settings.npmPeerDependencies.put("dependencies", "version");

            RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> settings.validate());
            assertEquals(exceptionMessage, exception.getMessage());
        }
    }
}
