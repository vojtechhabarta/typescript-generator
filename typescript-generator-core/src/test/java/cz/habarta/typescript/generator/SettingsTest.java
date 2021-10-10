
package cz.habarta.typescript.generator;

import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class SettingsTest {

    @Test
    public void testParseModifiers() {
        Assertions.assertEquals(0, Settings.parseModifiers("", Modifier.fieldModifiers()));
        Assertions.assertEquals(Modifier.STATIC, Settings.parseModifiers("static", Modifier.fieldModifiers()));
        Assertions.assertEquals(Modifier.STATIC | Modifier.TRANSIENT, Settings.parseModifiers("static | transient", Modifier.fieldModifiers()));
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
            Assertions.assertEquals(exceptionMessage, exception.getMessage());
        }

        {
            Settings settings = new Settings();
            settings.outputKind = TypeScriptOutputKind.module;
            settings.jsonLibrary = JsonLibrary.jackson2;
            settings.generateNpmPackageJson = false;
            settings.npmDevDependencies.put("dependencies", "version");
            
            RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> settings.validate());
            Assertions.assertEquals(exceptionMessage, exception.getMessage());
        }

        {
            Settings settings = new Settings();
            settings.outputKind = TypeScriptOutputKind.module;
            settings.jsonLibrary = JsonLibrary.jackson2;
            settings.generateNpmPackageJson = false;
            settings.npmPeerDependencies.put("dependencies", "version");
            
            RuntimeException exception = Assertions.assertThrows(RuntimeException.class, () -> settings.validate());
            Assertions.assertEquals(exceptionMessage, exception.getMessage());
        }
    }
}
