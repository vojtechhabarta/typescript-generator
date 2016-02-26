
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.*;
import java.io.*;
import java.text.*;
import java.util.*;


public class Emitter {

    private final Settings settings;
    private Writer writer;
    private boolean forceExportKeyword;
    private int indent;

    public Emitter(Settings settings) {
        this.settings = settings;
    }

    public void emit(TsModel model, Writer output, String outputName, boolean closeOutput, boolean forceExportKeyword, int initialIndentationLevel) {
        this.writer = output;
        this.forceExportKeyword = forceExportKeyword;
        this.indent = initialIndentationLevel;
        if (outputName != null) {
            System.out.println("Writing declarations to: " + outputName);
        }
        emitFileComment();
        emitModule(model);
        if (closeOutput) {
            close();
        }
    }

    private void emitFileComment() {
        if (!settings.noFileComment) {
            final String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            writeIndentedLine("// Generated using typescript-generator version " + TypeScriptGenerator.Version + " on " + timestamp + ".");
        }
    }

    private void emitModule(TsModel model) {
        if (settings.module != null && settings.outputFileType == TypeScriptFormat.declarationFile) {
            writeNewLine();
            writeIndentedLine("declare module \"" + settings.module + "\" {");
            indent++;
            emitNamespace(model);
            indent--;
            writeNewLine();
            writeIndentedLine("}");
        } else {
            emitNamespace(model);
        }
    }

    private void emitNamespace(TsModel model) {
        if (settings.namespace != null) {
            writeNewLine();
            final String prefix = settings.outputFileType == TypeScriptFormat.declarationFile
                    ? (settings.module != null ? "" : "declare ")
                    : (settings.module != null ? "export " : "");
            writeIndentedLine(prefix +  "namespace " + settings.namespace + " {");
            indent++;
            final boolean exportElements = settings.outputFileType == TypeScriptFormat.implementationFile;
            emitElements(model, exportElements);
            indent--;
            writeNewLine();
            writeIndentedLine("}");
        } else {
            final boolean exportElements = settings.module != null && settings.outputFileType == TypeScriptFormat.implementationFile;
            emitElements(model, exportElements);
        }
    }

    private void emitElements(TsModel model, boolean exportKeyword) {
        exportKeyword = exportKeyword || forceExportKeyword;
        emitInterfaces(model, exportKeyword);
        emitEnums(model, exportKeyword);
        emitTypeAliases(model, exportKeyword);
        for (EmitterExtension emitterExtension : settings.extensions) {
            emitterExtension.emitElements(new EmitterExtension.Writer() {
                @Override
                public void writeIndentedLine(String line) {
                    Emitter.this.writeIndentedLine(line);
                }
            }, settings, exportKeyword, model);
        }
    }

    private void emitInterfaces(TsModel model, boolean exportKeyword) {
        final List<TsBeanModel> beans = model.getBeans();
        if (settings.sortDeclarations || settings.sortTypeDeclarations) {
            Collections.sort(beans);
        }
        for (TsBeanModel bean : beans) {
            writeNewLine();
            final String parent = bean.getParent() != null ? " extends " + bean.getParent() : "";
            writeIndentedLine(exportKeyword, "interface " + bean.getName() + parent + " {");
            indent++;
            final List<TsPropertyModel> properties = bean.getProperties();
            if (settings.sortDeclarations) {
                Collections.sort(properties);
            }
            for (TsPropertyModel property : properties) {
                emitProperty(property);
            }
            indent--;
            writeIndentedLine("}");
        }
    }

    private void emitProperty(TsPropertyModel property) {
        if (property.getComments() != null) {
            writeIndentedLine("/**");
            for (String comment : property.getComments()) {
                writeIndentedLine("  * " + comment);
            }
            writeIndentedLine("  */");
        }
        final TsType tsType = property.getTsType();
        final String questionMark = settings.declarePropertiesAsOptional || (tsType instanceof TsType.OptionalType) ? "?" : "";
        writeIndentedLine(property.getName() + questionMark + ": " + tsType + ";");
    }

    private void emitEnums(TsModel model, boolean exportKeyword) {
        for (TsType.EnumType enumType : model.getEnums()) {
            writeNewLine();
            final ArrayList<String> quotedValues = new ArrayList<>();
            for (String value : enumType.values) {
                quotedValues.add(settings.quotes + value + settings.quotes);
            }
            writeIndentedLine(exportKeyword, "type " + enumType.name + " = " + ModelCompiler.join(quotedValues, " | ") + ";");
        }
    }

    private void emitTypeAliases(TsModel model, boolean exportKeyword) {
        for (TsType.AliasType alias : model.getTypeAliases()) {
            writeNewLine();
            writeIndentedLine(exportKeyword, alias.definition);
        }
    }

    private void writeIndentedLine(boolean exportKeyword, String line) {
        writeIndentedLine((exportKeyword ? "export " : "") + line);
    }

    private void writeIndentedLine(String line) {
        try {
            if (!line.isEmpty()) {
                for (int i = 0; i < indent; i++) {
                    writer.write(settings.indentString);
                }
            }
            writer.write(line);
            writeNewLine();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void writeNewLine() {
        try {
            writer.write(settings.newline);
            writer.flush();
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
