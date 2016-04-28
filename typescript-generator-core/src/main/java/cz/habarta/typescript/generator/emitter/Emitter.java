
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
        emitReferences();
        emitImports();
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

    private void emitReferences() {
        if (settings.referencedFiles != null && !settings.referencedFiles.isEmpty()) {
            writeNewLine();
            for (String reference : settings.referencedFiles) {
                writeIndentedLine("/// <reference path=" + quote(reference) + " />");
            }
        }
    }

    private void emitImports() {
        if (settings.importDeclarations != null && !settings.importDeclarations.isEmpty()) {
            writeNewLine();
            for (String importDeclaration : settings.importDeclarations) {
                writeIndentedLine(importDeclaration + ";");
            }
        }
    }

    private void emitModule(TsModel model) {
        if (settings.outputKind == TypeScriptOutputKind.ambientModule) {
            writeNewLine();
            writeIndentedLine("declare module " + quote(settings.module) + " {");
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
            String prefix = "";
            if (settings.outputFileType == TypeScriptFileType.declarationFile && settings.outputKind == TypeScriptOutputKind.global) {
                prefix = "declare ";
            }
            if (settings.outputKind == TypeScriptOutputKind.module) {
                prefix = "export ";
            }
            writeIndentedLine(prefix +  "namespace " + settings.namespace + " {");
            indent++;
            final boolean exportElements = settings.outputFileType == TypeScriptFileType.implementationFile;
            emitElements(model, exportElements);
            indent--;
            writeNewLine();
            writeIndentedLine("}");
        } else {
            final boolean exportElements = settings.outputKind == TypeScriptOutputKind.module;
            emitElements(model, exportElements);
        }
    }

    private void emitElements(TsModel model, boolean exportKeyword) {
        exportKeyword = exportKeyword || forceExportKeyword;
        emitInterfaces(model, exportKeyword);
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
        final List<TsBeanModel> beans = new ArrayList<>(model.getBeans());
        if (settings.sortDeclarations || settings.sortTypeDeclarations) {
            Collections.sort(beans);
        }
        for (TsBeanModel bean : beans) {
            writeNewLine();
            emitComments(bean.getComments());
            final List<TsType> parents = bean.getParentAndInterfaces();
            final String extendsClause = parents.isEmpty() ? "" : " extends " + Utils.join(parents, ", ");
            writeIndentedLine(exportKeyword, "interface " + bean.getName() + extendsClause + " {");
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
        emitComments(property.getComments());
        final TsType tsType = property.getTsType();
        final String questionMark = settings.declarePropertiesAsOptional || (tsType instanceof TsType.OptionalType) ? "?" : "";
        writeIndentedLine(toPropertyName(property.getName()) + questionMark + ": " + tsType.format(settings) + ";");
    }

    private String toPropertyName(String name) {
        return isValidIdentifierName(name) ? name : quote(name);
    }

    // https://github.com/Microsoft/TypeScript/blob/master/doc/spec.md#2.2.2
    // http://www.ecma-international.org/ecma-262/6.0/index.html#sec-names-and-keywords
    private static boolean isValidIdentifierName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        final char start = name.charAt(0);
        if (!Character.isUnicodeIdentifierStart(start) && start != '$' && start != '_') {
            return false;
        }
        for (char c : name.substring(1).toCharArray()) {
            if (!Character.isUnicodeIdentifierPart(c) && c != '$' && c != '_' && c != '\u200C' && c != '\u200D') {
                return false;
            }
        }
        return true;
    }

    private void emitTypeAliases(TsModel model, boolean exportKeyword) {
        final ArrayList<TsAliasModel> aliases = new ArrayList<>(model.getTypeAliases());
        if (settings.sortDeclarations || settings.sortTypeDeclarations) {
            Collections.sort(aliases);
        }
        for (TsAliasModel alias : aliases) {
            writeNewLine();
            emitComments(alias.getComments());
            writeIndentedLine(exportKeyword, "type " + alias.getName() + " = " + alias.getDefinition().format(settings) + ";");
        }
    }

    private void emitComments(List<String> comments) {
        if (comments != null) {
            writeIndentedLine("/**");
            for (String comment : comments) {
                writeIndentedLine(" * " + comment);
            }
            writeIndentedLine(" */");
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

    private String quote(String value) {
        return settings.quotes + value + settings.quotes;
    }

    private void close() {
        try {
            writer.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
