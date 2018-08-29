
package cz.habarta.typescript.generator.ext;

import cz.habarta.typescript.generator.DeprecationText;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.compiler.EnumMemberModel;
import cz.habarta.typescript.generator.emitter.EmitterExtension;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.emitter.TsEnumModel;
import cz.habarta.typescript.generator.emitter.TsModel;
import java.util.Collections;
import java.util.List;


@DeprecationText("Consider using configuration parameter 'mapEnum' with value 'asEnum'")
public class EnumConstantsExtension extends EmitterExtension {

    @Override
    public EmitterExtensionFeatures getFeatures() {
        final EmitterExtensionFeatures features = new EmitterExtensionFeatures();
        features.generatesRuntimeCode = true;
        return features;
    }

    @Override
    public void emitElements(Writer writer, Settings settings, boolean exportKeyword, TsModel model) {
        String exportString = exportKeyword ? "export " : "";
        List<TsEnumModel> enums = model.getOriginalStringEnums();
        Collections.sort(enums);
        for (TsEnumModel tsEnum : enums) {
            writer.writeIndentedLine("");
            writer.writeIndentedLine(exportString + "const " + tsEnum.getName().getSimpleName() + " = {");
            for (EnumMemberModel member : tsEnum.getMembers()) {
                writer.writeIndentedLine(settings.indentString + member.getPropertyName() + ": " + "<" + tsEnum.getName().getSimpleName() + ">\"" + member.getEnumValue() + "\",");
            }
            writer.writeIndentedLine("}");
        }
    }

}
