
package cz.habarta.typescript.generator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
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
			String moduleName = null;

			if (settings.ambientModuleName) {
				moduleName = "\"" + settings.moduleName + "\"";
			} else {
				moduleName = settings.moduleName;
			}

			writeIndentedLine("declare module " + moduleName + " {");
			indent++;
			emitDefinitions(model);
			indent--;
			writeNewLine();
			writeIndentedLine("}");
		} else {
			emitDefinitions(model);
		}
	}

	private void emitDefinitions(Model model) {
		for (BaseModel baseModel : model.getBeans()) {
			if (baseModel instanceof BeanModel) {
				emitInterface((BeanModel) baseModel);
			} else if (baseModel instanceof EnumModel) {
				emitEnum((EnumModel) baseModel);
			}
		}
	}

	private void emitInterface(BeanModel bean) {
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

	private void emitEnum(EnumModel enumModel) {
		writeNewLine();
		writeIndentedLine("export enum " + enumModel.getName() + " {");
		indent++;
		StringBuilder valuesBuilder = new StringBuilder();
		for (String value : enumModel.getValues()) {
			if (!(valuesBuilder.length() == 0)) {
				valuesBuilder.append(", ");
			}
			valuesBuilder.append(value);
		}
		writeIndentedLine(valuesBuilder.toString());
		indent--;
		writeIndentedLine("}");
	}

	private void emitProperty(PropertyModel property) {
		if (property.getComments() != null) {
			writeIndentedLine("/**");
			for (String comment : property.getComments()) {
				writeIndentedLine("  * " + comment);
			}
			writeIndentedLine("  */");
		}

		TsType tsType = null;
		if (settings.declareEnums) {
			tsType = property.getTsType();
		} else {
			tsType = property.getTsType() instanceof TsType.EnumType ? TsType.String : property.getTsType();
		}

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
