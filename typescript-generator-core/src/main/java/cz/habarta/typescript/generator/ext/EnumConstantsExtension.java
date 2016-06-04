
package cz.habarta.typescript.generator.ext;

import java.util.Collections;
import java.util.List;

import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.compiler.EnumKind;
import cz.habarta.typescript.generator.compiler.EnumMemberModel;
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
        List<TsEnumModel<String>> enums = model.getEnums(EnumKind.StringBased);
        Collections.sort(enums);
        for (TsEnumModel<String> tsEnum : enums) {
            writer.writeIndentedLine("");
            writer.writeIndentedLine(exportString + "const " + tsEnum.getName() + " = {");
            for (EnumMemberModel<String> member : tsEnum.getMembers()) {
                writer.writeIndentedLine(settings.indentString + member.getPropertyName() + ": " + "<" + tsEnum.getName() + ">\"" + member.getEnumValue() + "\",");
            }
            writer.writeIndentedLine("}");
        }
    }
}
