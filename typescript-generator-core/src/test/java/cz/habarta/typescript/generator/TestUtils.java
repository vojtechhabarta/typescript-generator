
package cz.habarta.typescript.generator;


public class TestUtils {

    private TestUtils() {
    }

    public static Settings settings() {
        final Settings settings = new Settings();
        settings.outputKind = TypeScriptOutputKind.global;
        settings.jsonLibrary = JsonLibrary.jackson2;
        settings.noFileComment = true;
        settings.newline = "\n";
        return settings;
    }

}
