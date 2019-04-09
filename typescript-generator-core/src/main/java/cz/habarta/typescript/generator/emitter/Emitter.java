
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.*;
import cz.habarta.typescript.generator.compiler.EnumMemberModel;
import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.util.Utils;
import java.io.*;
import java.text.*;
import java.util.*;


public class Emitter implements EmitterExtension.Writer {

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
            TypeScriptGenerator.getLogger().info("Writing declarations to: " + outputName);
        }
        emitFileComment();
        emitReferences();
        emitImports();
        emitModule(model);
        emitUmdNamespace();
        if (closeOutput) {
            close();
        }
    }

    private void emitFileComment() {
        if (!settings.noTslintDisable) {
            writeIndentedLine("/* tslint:disable */");
        }
        if (!settings.noEslintDisable) {
            writeIndentedLine("/* eslint-disable */");
        }
        if (!settings.noFileComment) {
            final String timestamp = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date());
            writeIndentedLine("// Generated using typescript-generator version " + TypeScriptGenerator.Version + " on " + timestamp + ".");
        }
    }

    private void emitReferences() {
        if (settings.referencedFiles != null && !settings.referencedFiles.isEmpty()) {
            writeNewLine();
            for (String reference : settings.referencedFiles) {
                writeIndentedLine("/// <reference path=" + quote(reference, settings) + " />");
            }
        }
    }

    private void emitImports() {
        if (settings.moduleDependencies != null && !settings.moduleDependencies.isEmpty()) {
            writeNewLine();
            for (ModuleDependency dependency : settings.moduleDependencies) {
                writeIndentedLine("import * as " + dependency.importAs + " from " + quote(dependency.importFrom, settings) + ";");
            }
        }
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
            writeIndentedLine("declare module " + quote(settings.module, settings) + " {");
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
            emitElements(model, exportElements, false);
            indent--;
            writeNewLine();
            writeIndentedLine("}");
        } else {
            final boolean exportElements = settings.outputKind == TypeScriptOutputKind.module;
            final boolean declareElements = settings.outputFileType == TypeScriptFileType.declarationFile && settings.outputKind == TypeScriptOutputKind.global;
            emitElements(model, exportElements, declareElements);
        }
    }

    private void emitElements(TsModel model, boolean exportKeyword, boolean declareKeyword) {
        exportKeyword = exportKeyword || forceExportKeyword;
        emitBeans(model, exportKeyword, declareKeyword);
        emitTypeAliases(model, exportKeyword, declareKeyword);
        emitLiteralEnums(model, exportKeyword, declareKeyword);
        emitHelpers(model);
        for (EmitterExtension emitterExtension : settings.extensions) {
            final List<String> extensionLines = new ArrayList<>();
            final EmitterExtension.Writer extensionWriter = new EmitterExtension.Writer() {
                @Override
                public void writeIndentedLine(String line) {
                    extensionLines.add(line);
                }
            };
            emitterExtension.emitElements(extensionWriter, settings, exportKeyword, model);
            if (!extensionLines.isEmpty()) {
                writeNewLine();
                writeNewLine();
                writeIndentedLine(String.format("// Added by '%s' extension", emitterExtension.getClass().getSimpleName()));
                for (String line : extensionLines) {
                    this.writeIndentedLine(line);
                }
            }
        }
    }

    private void emitBeans(TsModel model, boolean exportKeyword, boolean declareKeyword) {
        for (TsBeanModel bean : model.getBeans()) {
            emitFullyQualifiedDeclaration(bean, exportKeyword, declareKeyword);
        }
    }

    private void emitTypeAliases(TsModel model, boolean exportKeyword, boolean declareKeyword) {
        for (TsAliasModel alias : model.getTypeAliases()) {
            emitFullyQualifiedDeclaration(alias, exportKeyword, declareKeyword);
        }
    }

    private void emitLiteralEnums(TsModel model, boolean exportKeyword, boolean declareKeyword) {
        for (TsEnumModel enumModel : model.getEnums()) {
            emitFullyQualifiedDeclaration(enumModel, exportKeyword, declareKeyword);
        }
    }

    private void emitFullyQualifiedDeclaration(TsDeclarationModel declaration, boolean exportKeyword, boolean declareKeyword) {
        if (declaration.getName().getNamespace() != null) {
            writeNewLine();
            final String prefix = declareKeyword ? "declare " : "";
            writeIndentedLine(exportKeyword, prefix + "namespace " + declaration.getName().getNamespace() + " {");
            indent++;
            emitDeclaration(declaration, true, false);
            indent--;
            writeNewLine();
            writeIndentedLine("}");
        } else {
            emitDeclaration(declaration, exportKeyword, declareKeyword);
        }
    }

    private void emitDeclaration(TsDeclarationModel declaration, boolean exportKeyword, boolean declareKeyword) {
        if (declaration instanceof TsBeanModel) {
            emitBean((TsBeanModel) declaration, exportKeyword);
        } else if (declaration instanceof TsAliasModel) {
            emitTypeAlias((TsAliasModel) declaration, exportKeyword);
        } else if (declaration instanceof TsEnumModel) {
            emitLiteralEnum((TsEnumModel) declaration, exportKeyword, declareKeyword);
        } else {
            throw new RuntimeException("Unknown declaration type: " + declaration.getClass().getName());
        }
    }

    private void emitBean(TsBeanModel bean, boolean exportKeyword) {
        writeNewLine();
        emitComments(bean.getComments());
        final String declarationType = bean.isClass() ? "class" : "interface";
        final String typeParameters = bean.getTypeParameters().isEmpty() ? "" : "<" + Utils.join(bean.getTypeParameters(), ", ")+ ">";
        final List<TsType> extendsList = bean.getExtendsList();
        final List<TsType> implementsList = bean.getImplementsList();
        final String extendsClause = extendsList.isEmpty() ? "" : " extends " + Utils.join(extendsList, ", ");
        final String implementsClause = implementsList.isEmpty() ? "" : " implements " + Utils.join(implementsList, ", ");
        writeIndentedLine(exportKeyword, declarationType + " " + bean.getName().getSimpleName() + typeParameters + extendsClause + implementsClause + " {");
        indent++;
        for (TsPropertyModel property : bean.getProperties()) {
            emitProperty(property);
        }
        if (bean.getConstructor() != null) {
            emitCallable(bean.getConstructor());
        }
        for (TsMethodModel method : bean.getMethods()) {
            emitCallable(method);
        }
        indent--;
        writeIndentedLine("}");
    }

    private void emitProperty(TsPropertyModel property) {
        emitComments(property.getComments());
        final TsType tsType = property.getTsType();
        final String staticString = property.modifiers.isStatic ? "static " : "";
        final String readonlyString = property.modifiers.isReadonly ? "readonly " : "";
        final String questionMark = tsType instanceof TsType.OptionalType ? "?" : "";
        writeIndentedLine(staticString + readonlyString + quoteIfNeeded(property.getName(), settings) + questionMark + ": " + tsType.format(settings) + ";");
    }

    public static String quoteIfNeeded(String name, Settings settings) {
        return ModelCompiler.isValidIdentifierName(name) ? name : quote(name, settings);
    }

    public static String quote(String value, Settings settings) {
        return settings.quotes + value + settings.quotes;
    }

    public static String formatList(Settings settings, List<? extends Emittable> list) {
        return formatList(settings, list, ", ");
    }

    public static String formatList(Settings settings, List<? extends Emittable> list, String delimiter) {
        final List<String> result = new ArrayList<>();
        for (Emittable item : list) {
            result.add(item.format(settings));
        }
        return Utils.join(result, delimiter);
    }

    private void emitCallable(TsCallableModel method) {
        writeNewLine();
        emitComments(method.getComments());
        final String staticString = method.getModifiers().isStatic ? "static " : "";
        final String typeParametersString = method.getTypeParameters().isEmpty() ? "" : "<" + formatList(settings, method.getTypeParameters()) + ">";
        final String parametersString = formatParameterList(method.getParameters(), true);
        final String type = method.getReturnType() != null ? ": " + method.getReturnType() : "";
        final String signature = staticString + method.getName() + typeParametersString + parametersString + type;
        if (method.getBody() != null) {
            writeIndentedLine(signature + " {");
            indent++;
            emitStatements(method.getBody());
            indent--;
            writeIndentedLine("}");
        } else {
            writeIndentedLine(signature + ";");
        }
    }

    public static String formatParameterList(List<? extends TsParameter> parameters, boolean alwaysEncloseInParentheses) {
        final List<String> params = new ArrayList<>();
        for (TsParameter parameter : parameters) {
            final TsAccessibilityModifier accessibilityModifier = parameter instanceof TsParameterModel
                    ? ((TsParameterModel) parameter).getAccessibilityModifier()
                    : null;
            final String access = accessibilityModifier != null ? accessibilityModifier.format() + " " : "";
            final String questionMark = (parameter.getTsType() instanceof TsType.OptionalType) ? "?" : "";
            final String type = parameter.getTsType() != null ? ": " + parameter.getTsType() : "";
            params.add(access + parameter.getName() + questionMark + type);
        }
        boolean parentheses = alwaysEncloseInParentheses || (parameters.size() != 1 || parameters.get(0).tsType != null);
        return parentheses
                ? "(" + Utils.join(params, ", ") + ")"
                : Utils.join(params, ", ");
    }

    private void emitStatements(List<TsStatement> statements) {
        for (TsStatement statement : statements) {
            if (statement instanceof TsReturnStatement) {
                emitReturnStatement((TsReturnStatement) statement);
            } else if (statement instanceof TsIfStatement) {
                emitIfStatement((TsIfStatement) statement);
            } else if (statement instanceof TsExpressionStatement) {
                emitExpressionStatement((TsExpressionStatement) statement);
            } else if (statement instanceof TsVariableDeclarationStatement) {
                emitVariableDeclarationStatement((TsVariableDeclarationStatement) statement);
            } else if (statement instanceof TsSwitchStatement) {
                emitTsSwitchStatement((TsSwitchStatement) statement);
            }
        }
    }

    private void emitReturnStatement(TsReturnStatement returnStatement) {
        if (returnStatement.getExpression() != null) {
            writeIndentedLine("return " + returnStatement.getExpression().format(settings) + ";");
        } else {
            writeIndentedLine("return;");
        }
    }

    private void emitIfStatement(TsIfStatement ifStatement) {
        writeIndentedLine("if (" + ifStatement.getExpression().format(settings) + ") {");
        indent++;
        emitStatements(ifStatement.getThenStatements());
        indent--;
        if (ifStatement.getElseStatements() != null) {
            writeIndentedLine("} else {");
            indent++;
            emitStatements(ifStatement.getElseStatements());
            indent--;
        }
        writeIndentedLine("}");
    }

    private void emitExpressionStatement(TsExpressionStatement expressionStatement) {
        writeIndentedLine(expressionStatement.getExpression().format(settings) + ";");
    }

    private void emitVariableDeclarationStatement(TsVariableDeclarationStatement variableDeclarationStatement) {
        writeIndentedLine(
                (variableDeclarationStatement.isConst() ? "const " : "let ")
                + variableDeclarationStatement.getName()
                + (variableDeclarationStatement.getType() != null ? ": " + variableDeclarationStatement.getType().format(settings) : "")
                + (variableDeclarationStatement.getInitializer() != null ? " = " + variableDeclarationStatement.getInitializer().format(settings) : "")
                + ";"
        );
    }

    private void emitTsSwitchStatement(TsSwitchStatement switchStatement) {
        writeIndentedLine("switch (" + switchStatement.getExpression().format(settings) + ") {");
        indent++;
        for (TsSwitchCaseClause caseClause : switchStatement.getCaseClauses()) {
            writeIndentedLine("case " + caseClause.getExpression().format(settings) + ":");
            indent++;
            emitStatements(caseClause.getStatements());
            indent--;
        }
        if (switchStatement.getDefaultClause() != null) {
            writeIndentedLine("default:");
            indent++;
            emitStatements(switchStatement.getDefaultClause());
            indent--;
        }
        indent--;
        writeIndentedLine("}");
    }

    private void emitTypeAlias(TsAliasModel alias, boolean exportKeyword) {
        writeNewLine();
        emitComments(alias.getComments());
        final String genericParameters = alias.getTypeParameters().isEmpty()
                ? ""
                : "<" + Utils.join(alias.getTypeParameters(), ", ") + ">";
        writeIndentedLine(exportKeyword, "type " + alias.getName().getSimpleName() + genericParameters + " = " + alias.getDefinition().format(settings) + ";");
    }

    private void emitLiteralEnum(TsEnumModel enumModel, boolean exportKeyword, boolean declareKeyword) {
        writeNewLine();
        emitComments(enumModel.getComments());
        final String declareText = declareKeyword ? "declare " : "";
        final String constText = enumModel.isNonConstEnum() ? "" : "const ";
        writeIndentedLine(exportKeyword, declareText + constText + "enum " + enumModel.getName().getSimpleName() + " {");
        indent++;
        for (EnumMemberModel member : enumModel.getMembers()) {
            emitComments(member.getComments());
            final Object value = member.getEnumValue();
            final String initializer = value != null
                    ? " = " + (value instanceof String ? quote((String) value, settings) : String.valueOf(value))
                    : "";
            writeIndentedLine(member.getPropertyName() + initializer + ",");
        }
        indent--;
        writeIndentedLine("}");
    }

    private void emitHelpers(TsModel model) {
        for (TsHelper helper : model.getHelpers()) {
            writeNewLine();
            writeTemplate(this, settings, helper.getLines(), null);
        }
    }

    private void emitUmdNamespace() {
        if (settings.umdNamespace != null) {
            writeNewLine();
            writeIndentedLine("export as namespace " + settings.umdNamespace + ";");
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

    public static void writeTemplate(EmitterExtension.Writer writer, Settings settings, List<String> template, Map<String, String> replacements) {
        for (String line : template) {
            if (replacements != null) {
                for (Map.Entry<String, String> entry : replacements.entrySet()) {
                    line = line.replace(entry.getKey(), entry.getValue());
                }
            }
            writer.writeIndentedLine(line
                    .replace("\t", settings.indentString)
                    .replace("\"", settings.quotes)
            );
        }
    }

    private void writeIndentedLine(boolean exportKeyword, String line) {
        writeIndentedLine((exportKeyword ? "export " : "") + line);
    }

    @Override
    public void writeIndentedLine(String line) {
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
