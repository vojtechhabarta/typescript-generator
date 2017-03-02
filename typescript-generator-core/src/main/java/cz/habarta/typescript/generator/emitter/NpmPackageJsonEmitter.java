
package cz.habarta.typescript.generator.emitter;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import cz.habarta.typescript.generator.util.StandardJsonPrettyPrinter;
import java.io.*;


public class NpmPackageJsonEmitter {

    private Writer writer;

    public void emit(NpmPackageJson npmPackageJson, Writer output, String outputName, boolean closeOutput) {
        this.writer = output;
        if (outputName != null) {
            System.out.println("Writing NPM package to: " + outputName);
        }
        emitPackageJson(npmPackageJson);
        if (closeOutput) {
            close();
        }
    }

    private void emitPackageJson(NpmPackageJson npmPackageJson) {
        try {
            final ObjectMapper objectMapper = new ObjectMapper();
            objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
            objectMapper.enable(SerializationFeature.INDENT_OUTPUT);
            objectMapper.setDefaultPrettyPrinter(new StandardJsonPrettyPrinter("  ", "\n"));
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
