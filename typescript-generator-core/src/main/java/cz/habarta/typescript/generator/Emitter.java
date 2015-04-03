
package cz.habarta.typescript.generator;

import java.io.*;
import java.util.logging.Logger;


public class Emitter {

    private final Logger logger;
    private final Settings settings;
    private final PrintWriter writer;
    private int indent = 0;

    private Emitter(Logger logger, Settings settings, PrintWriter writer) {
        this.logger = logger;
        this.settings = settings;
        this.writer = writer;
    }

    public static void emit(Logger logger, Settings settings, File outputFile, Model model) {
        try (PrintWriter printWriter = new PrintWriter(outputFile)) {
            final Emitter emitter = new Emitter(logger, settings, printWriter);
            emitter.emitModule(model);
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private void emitModule(Model model) {
        if (settings.moduleName != null) {
            writeNewLine();
            writeIndentedLine("declare module " + settings.moduleName + " {");
            indent++;
            emitInterfaces(model);
            indent--;
            writeNewLine();
            writeIndentedLine("}");
        } else {
            emitInterfaces(model);
        }
    }

    private void emitInterfaces(Model model) {
        for (BeanModel bean : model.getBeans()) {
            writeNewLine();
            final String parent = bean.getParent() != null ? " extends " + bean.getParent() : "";
            writeIndentedLine("export interface " + bean.getName() + parent + " {");
            indent++;
            for (PropertyModel property : bean.getProperties()) {
                emitProperty(property);
            }
            indent--;
            writeIndentedLine("}");
        }
    }

    private void emitProperty(PropertyModel property) {
        if (property.getComments() != null) {
            writeIndentedLine("/**");
            for (String comment : property.getComments()) {
                writeIndentedLine("  * " + comment);
            }
            writeIndentedLine("  */");
        }
        final TsType tsType = property.getTsType() instanceof TsType.EnumType ? TsType.String : property.getTsType();
        final String opt = settings.declarePropertiesAsOptional ? "?" : "";
        writeIndentedLine(property.getName() + opt + ": " + tsType + ";");
    }

    private void writeIndentedLine(String line) {
        for (int i = 0; i < indent; i++) {
            writer.write(settings.indentString);
        }
        writer.write(line);
        writeNewLine();
    }

    private void writeNewLine() {
        writer.write(settings.newline);
    }

}
