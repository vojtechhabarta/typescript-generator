
package cz.habarta.typescript.generator.emitter;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import cz.habarta.typescript.generator.util.Utils;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;


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
        infoJson.classes = new ArrayList<>();
        for (TsBeanModel tsBeanModel : tsModel.getBeans()) {
            if (tsBeanModel.origin != null) {
                final InfoJson.ClassInfo typeMapping = new InfoJson.ClassInfo();
                typeMapping.javaClass = tsBeanModel.origin.getName();
                typeMapping.typeName = tsBeanModel.name.getFullName();
                infoJson.classes.add(typeMapping);
            }
        }
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
