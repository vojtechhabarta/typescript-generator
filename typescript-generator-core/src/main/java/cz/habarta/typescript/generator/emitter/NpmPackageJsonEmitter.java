
package cz.habarta.typescript.generator.emitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import cz.habarta.typescript.generator.util.Utils;
import java.io.IOException;
import java.io.Writer;


public class NpmPackageJsonEmitter {

    private Writer writer;

    public void emit(NpmPackageJson npmPackageJson, Writer output, String outputName, boolean closeOutput) {
        this.writer = output;
        if (outputName != null) {
            TypeScriptGenerator.getLogger().info("Writing NPM package to: " + outputName);
        }
        emitPackageJson(npmPackageJson);
        if (closeOutput) {
            close();
        }
    }

    private void emitPackageJson(NpmPackageJson npmPackageJson) {
        try {
            final ObjectMapper objectMapper = Utils.getObjectMapper();
            objectMapper.writeValue(writer, npmPackageJson);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void close() {
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
