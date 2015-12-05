
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.*;
import java.io.*;
import java.text.*;
import java.util.*;
import java.util.logging.Logger;


public class Emitter {

    private final Logger logger;
    private final Settings settings;
    private final PrintWriter writer;
    private final boolean forceExportKeyword;
    private int indent;

    private Emitter(Logger logger, Settings settings, boolean forceExportKeyword, int initialIndentationLevel, PrintWriter writer) {
        this.logger = logger;
        this.settings = settings;
        this.forceExportKeyword = forceExportKeyword;
        this.writer = writer;
        this.indent = initialIndentationLevel;
    }

    public static void emit(Logger logger, Settings settings, Writer output, TsModel model, boolean forceExportKeyword, int initialIndentationLevel) {
        try (PrintWriter printWriter = new PrintWriter(output)) {
            final Emitter emitter = new Emitter(logger, settings, forceExportKeyword, initialIndentationLevel, printWriter);
            emitter.emitFileComment();
            emitter.emitModule(model);
        }
    }

    private void emitFileComment() {
        if (!settings.noFileComment) {
            final String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            writeIndentedLine("// Generated using typescript-generator version " + TypeScriptGenerator.Version + " on " + timestamp + ".");
        }
    }

    private void emitModule(TsModel model) {
        if (settings.sortDeclarations) {
            model.sort();
        }
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
        for (TsBeanModel bean : model.getBeans()) {
            writeNewLine();
            String parent = bean.getParent() != null ? " extends " + bean.getParent() : "";
            String genericString =  "";
            if (bean.getGenericDeclarations().size() > 0) {
                List<String> generics = bean.getGenericDeclarations();
                genericString = Arrays.toString(generics.toArray());
                genericString = "<" + genericString.substring(1, genericString.length() - 1) + ">";
            }
            writeIndentedLine(exportPrefix + "interface " + bean.getName() + parent + genericString + " {");
            indent++;
            for (TsPropertyModel property : bean.getProperties()) {
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
        final String opt = settings.declarePropertiesAsOptional || tsType.getOptional() ? "?" : "";
        writeIndentedLine(property.getName() + opt + ": " + tsType + ";");
    }

    private void emitTypeAliases(TsModel model) {
        for (TsType.AliasType alias : model.getTypeAliases()) {
            writeNewLine();
            writeIndentedLine(alias.definition);
        }
    }

    private void writeIndentedLine(String line) {
        if (!line.isEmpty()) {
            for (int i = 0; i < indent; i++) {
                writer.write(settings.indentString);
            }
        }
        writer.write(line);
        writeNewLine();
    }

    private void writeNewLine() {
        writer.write(settings.newline);
    }

}
