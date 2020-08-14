package cz.habarta.typescript.generator;


import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import static org.junit.Assert.assertEquals;
import org.junit.Test;

public class SkipTest {
    @Test
    public void skippingShouldNotDoAnything() throws IOException {
        final Settings settings = TestUtils.settings();
        settings.skip = true;
        final File actualFile = new File("target/skipTests-actual.ts");
        new TypeScriptGenerator(settings).generateTypeScript(Input.from(String.class), Output.to(actualFile));

        assertEquals(0, Files.readAllBytes(actualFile.toPath()).length);
    }
}
