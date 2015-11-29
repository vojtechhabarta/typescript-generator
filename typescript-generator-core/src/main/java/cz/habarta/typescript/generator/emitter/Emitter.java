
package cz.habarta.typescript.generator.emitter;

import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.List;
import java.util.logging.Logger;

import com.google.api.client.repackaged.com.google.common.base.Joiner;

import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TsType;


public class Emitter {

    private final Logger logger;
    private final Settings settings;
    private final PrintWriter writer;
    private int indent;

    private Emitter(Logger logger, Settings settings, PrintWriter writer) {
        this.logger = logger;
        this.settings = settings;
        this.writer = writer;
        this.indent = settings.initialIndentationLevel;
    }

    public static void emit(Logger logger, Settings settings, OutputStream output, TsModel model) {
        try (PrintWriter printWriter = new PrintWriter(output)) {
            final Emitter emitter = new Emitter(logger, settings, printWriter);
            emitter.emitModule(model);
        }
    }

    private void emitModule(TsModel model) {
        model.sort();
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
        for (TsBeanModel bean : model.getBeans()) {
            writeNewLine();
            if (bean instanceof TsEnumBeanModel) {
                TsEnumBeanModel enumBean = (TsEnumBeanModel) bean;
                List<String> values = enumBean.getType().values;
                writeIndentedLine(settings.declarationPrefix + "var " + bean.getName() + " = {");
                indent++;
                int i = 0;
                for (String value : values) {
                    String lineToWrite = value + ": \"" + value + "\"";
                    if (i != values.size() - 1) {
                        lineToWrite += ",";
                    }
                    i++;
                    writeIndentedLine(lineToWrite);
                }
                indent--;
                writeIndentedLine("};");
            } else {
                final String parent = bean.getParent() != null ? " extends " + bean.getParent() : "";
                String genericString = "";
                if (bean.getGenericDeclarations().size() > 0) {
                    genericString = "<" + Joiner.on(", ").join(bean.getGenericDeclarations().iterator()) + ">";
                }

                writeIndentedLine(settings.declarationPrefix + "interface " + bean.getName() + parent + genericString + " {");
                indent++;
                for (TsPropertyModel property : bean.getProperties()) {
                    emitProperty(property);
                }
                indent--;
                writeIndentedLine("}");
            }
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
        final TsType tsType = property.getTsType() instanceof TsType.EnumType ? TsType.String : property.getTsType();
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
        for (int i = 0; !line.isEmpty() && i < indent; i++) {
            writer.write(settings.indentString);
        }
        writer.write(line);
        writeNewLine();
    }

    private void writeNewLine() {
        writer.write(settings.newline);
    }

}
