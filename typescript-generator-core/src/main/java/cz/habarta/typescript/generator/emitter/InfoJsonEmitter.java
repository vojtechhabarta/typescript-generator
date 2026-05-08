
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TypeScriptGenerator;
import cz.habarta.typescript.generator.util.Utils;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;
import tools.jackson.databind.ObjectMapper;


public class InfoJsonEmitter {

    private Writer writer;

    public InfoJsonEmitter(Writer output, @Nullable String outputName) {
        this.writer = output;
        if (outputName != null) {
            TypeScriptGenerator.getLogger().info("Writing module info to: " + outputName);
        }
    }

    public void emit(TsModel tsModel, boolean closeOutput) {
        emitTypeMappingJson(tsModel);
        if (closeOutput) {
            close();
        }
    }

    private void emitTypeMappingJson(TsModel tsModel) {
        final ObjectMapper objectMapper = Utils.getObjectMapper();
        final InfoJson infoJson = getInfoJson(tsModel);
        objectMapper.writeValue(writer, infoJson);
    }

    private InfoJson getInfoJson(TsModel tsModel) {
        final LinkedHashMap<String, InfoJson.ClassInfo> map = new LinkedHashMap<>();
        Stream
            .of(
                tsModel.getBeans(),
                tsModel.getEnums(),
                tsModel.getTypeAliases()
            )
            .flatMap(s -> s.stream())
            .filter(declaration -> declaration.origin != null)
            .map(declaration -> {
                final InfoJson.ClassInfo typeMapping = new InfoJson.ClassInfo(
                    declaration.origin.getName(),
                    declaration.name.getFullName()
                );
                return typeMapping;
            })
            .forEach(info -> {
                // remove duplicates, append new items to the end
                map.remove(info.javaClass);
                map.put(info.javaClass, info);
            });

        final InfoJson infoJson = new InfoJson(new ArrayList<>(map.values()));
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
