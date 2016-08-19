
package cz.habarta.typescript.generator.ext;

import java.util.Collections;
import java.util.List;

import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.compiler.EnumKind;
import cz.habarta.typescript.generator.compiler.EnumMemberModel;
import cz.habarta.typescript.generator.emitter.EmitterExtension;
import cz.habarta.typescript.generator.emitter.TsEnumModel;
import cz.habarta.typescript.generator.emitter.TsModel;

/**
 * For creating real TypeScript enumerations
 * @author rgevrek
 * @since 19.08.2016
 */
public class EnumExtension extends EmitterExtension {

  @Override
  public boolean generatesRuntimeCode() {
    return false;
  }

  @Override
  public void emitElements(Writer writer, Settings settings, boolean exportKeyword, TsModel model) {
    String exportString = exportKeyword ? "export " : "";
    List<TsEnumModel<String>> enums = model.getEnums(EnumKind.StringBased);
    Collections.sort(enums);
    for (TsEnumModel<String> tsEnum : enums) {
      writer.writeIndentedLine("");
      writer.writeIndentedLine(exportString + "const enum " + tsEnum.getName() + " {");
      int length = tsEnum.getMembers().size();
      int i=0;
      for (EnumMemberModel<String> member : tsEnum.getMembers()) {
        if (++i<length) {
          writer.writeIndentedLine(settings.indentString + member.getPropertyName()+",");
        }
        else
          writer.writeIndentedLine(settings.indentString + member.getPropertyName());
      }
      writer.writeIndentedLine("}");
    }
  }
}