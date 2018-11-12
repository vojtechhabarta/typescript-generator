
package cz.habarta.typescript.generator.emitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import cz.habarta.typescript.generator.util.Utils;
import java.io.IOException;
import java.io.Writer;
import java.util.stream.Collectors;
import java.util.stream.Stream;


public class InfoJsonEmitter {

    private Writer writer;

    public void emit(TsModel tsModel, Writer output, String outputName, boolean closeOutput) {
        this.writer = output;
        if (outputName != null) {
            TypeScriptGenerator.getLogger().info("Writing module info to: " + outputName);
        }
        emitTypeMappingJson(tsModel);
        if (closeOutput) {
            close();
        }
    }

    private void emitTypeMappingJson(TsModel tsModel) {
        try {
            final ObjectMapper objectMapper = Utils.getObjectMapper();
            final InfoJson infoJson = getInfoJson(tsModel);
            objectMapper.writeValue(writer, infoJson);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private InfoJson getInfoJson(TsModel tsModel) {
        final InfoJson infoJson = new InfoJson();

        infoJson.classes = Stream
                .of(
                        tsModel.getBeans(),
                        tsModel.getEnums(),
                        tsModel.getTypeAliases()
                )
                .flatMap(s -> s.stream())
                .filter(declaration -> declaration.origin != null)
                .map(declaration -> {
                    final InfoJson.ClassInfo typeMapping = new InfoJson.ClassInfo();
                    typeMapping.javaClass = declaration.origin.getName();
                    typeMapping.typeName = declaration.name.getFullName();
                    return typeMapping;
                })
                .collect(Collectors.toList());
        return infoJson;
    }

    private void close() {
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
