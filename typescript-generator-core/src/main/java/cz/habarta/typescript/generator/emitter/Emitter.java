
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
        if (settings.module != null) {
            writeNewLine();
            writeIndentedLine("declare module '" + settings.module + "' {");
            indent++;
            emitNamespace(model, true);
            indent--;
            writeNewLine();
            writeIndentedLine("}");
        } else {
            emitNamespace(model, false);
        }
    }

    private void emitNamespace(TsModel model, boolean ambientContext) {
        if (settings.namespace != null) {
            writeNewLine();
            final String declarePrefix = ambientContext ? "" : "declare ";
            writeIndentedLine(declarePrefix +  "namespace " + settings.namespace + " {");
            indent++;
            emitObjects(model);
            indent--;
            writeNewLine();
            writeIndentedLine("}");
        } else {
            emitObjects(model);
        }
    }

    private void emitObjects(TsModel model) {
        emitInterfaces(model);
        emitTypeAliases(model);
    }

    private void emitInterfaces(TsModel model) {
        String exportPrefix = forceExportKeyword ? "export " : "";
        final List<TsBeanModel> beans = model.getBeans();
        if (settings.sortDeclarations || settings.sortTypeDeclarations) {
            Collections.sort(beans);
        }
        for (TsBeanModel bean : beans) {
            writeNewLine();
            final String parent = bean.getParent() != null ? " extends " + bean.getParent() : "";
            writeIndentedLine(exportPrefix + "interface " + bean.getName() + parent + " {");
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

    private void emitTypeAliases(TsModel model) {
        for (TsType.AliasType alias : model.getTypeAliases()) {
            writeNewLine();
            writeIndentedLine(alias.definition);
        }
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
