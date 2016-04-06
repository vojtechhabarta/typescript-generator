
package cz.habarta.typescript.generator.ext;

import java.util.Collections;
import java.util.List;

import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.emitter.EmitterExtension;
import cz.habarta.typescript.generator.emitter.TsEnumModel;
import cz.habarta.typescript.generator.emitter.TsModel;


public class EnumConstantsExtension extends EmitterExtension {

    @Override
    public boolean generatesRuntimeCode() {
        return true;
    }

    @Override
    public void emitElements(Writer writer, Settings settings, boolean exportKeyword, TsModel model) {
        String exportString = exportKeyword ? "export " : "";
        List<TsEnumModel> enums = model.getEnums();
        Collections.sort(enums);
        for (TsEnumModel tsEnum : enums) {
            writer.writeIndentedLine("");
            writer.writeIndentedLine(exportString + "const " + tsEnum.getName() + " = {");
            for (String value : tsEnum.getValues()) {
                writer.writeIndentedLine(settings.indentString + value + ": " + "<" + tsEnum.getName() + ">\"" + value + "\",");
            }
            writer.writeIndentedLine("}");
        }
    }
}
