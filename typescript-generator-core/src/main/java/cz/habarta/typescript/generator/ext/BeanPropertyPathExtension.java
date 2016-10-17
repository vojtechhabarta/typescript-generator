package cz.habarta.typescript.generator.ext;

import java.util.*;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.TsType.GenericReferenceType;
import cz.habarta.typescript.generator.compiler.EnumKind;
import cz.habarta.typescript.generator.compiler.EnumMemberModel;
import cz.habarta.typescript.generator.emitter.EmitterExtension;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.emitter.TsBeanModel;
import cz.habarta.typescript.generator.emitter.TsEnumModel;
import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.emitter.TsPropertyModel;

/**
 * Emitter which generates type-safe property path getters.
 *
 * Many javascript frameworks require that you specify "property paths"
 * to extract data from objects. For instance, if you have a data model
 * which is an array of items, and you want to display them in a grid,
 * you can give column specifications as strings, like "field1.field2".
 * With this emitter you can specify such paths like so:
 * {@code ClassName.field1.field2.get()}
 * Once you call {@code get()}, you get a string
 * (in this case "field1.field2")
 */
public class BeanPropertyPathExtension extends EmitterExtension {

    @Override
    public EmitterExtensionFeatures getFeatures() {
        final EmitterExtensionFeatures features = new EmitterExtensionFeatures();
        features.generatesRuntimeCode = true;
        return features;
    }

    @Override
    public void emitElements(Writer writer, Settings settings, boolean exportKeyword, TsModel model) {
        emitFieldsClass(writer, settings);
        for (TsBeanModel bean : model.getBeans()) {
            writeBeanFieldSpecs(writer, settings, model, bean);
        }
        for (TsBeanModel bean : model.getBeans()) {
            createBeanFieldConstant(writer, bean);
        }
    }

    private static void emitFieldsClass(Writer writer, Settings settings) {
        List<String> fieldsClassLines = Arrays.asList(
            "class Fields {",
            "    protected parent: Fields | undefined;",
            "    protected name: string | undefined;",
            "    constructor(parent?: Fields, name?: string) {",
            "        this.parent = parent;",
            "        this.name = name;",
            "    };",
            "    get(): string | undefined {",
            "        if (this.parent && this.parent.get()) {",
            "            return this.name ? this.parent.get() + \".\" + this.name : this.parent.get();",
            "        } else {",
            "            return this.name;",
            "        }",
            "    }",
            "}");
        writer.writeIndentedLine("");
        for (String fieldsClassLine : fieldsClassLines) {
            writer.writeIndentedLine(fieldsClassLine.replace("    ", settings.indentString));
        }
    }

    private static void writeBeanFieldSpecs(Writer writer, Settings settings, TsModel model, TsBeanModel bean) {
        writer.writeIndentedLine("");
        writer.writeIndentedLine("class " + getBeanModelClassName(bean) + "Fields extends Fields {");
        writer.writeIndentedLine(settings.indentString + "constructor(parent?: Fields, name?: string) { super(parent, name); }");
        TsBeanModel curBean = bean;
        while (curBean != null) {
            for (TsPropertyModel property : curBean.getProperties()) {
                writeBeanProperty(writer, settings, model, curBean, property);
            }
            // also add inherited fields if any
            curBean = getBeanModelByType(model, curBean.getParent());
        }
        writer.writeIndentedLine("}");
    }

    private static TsBeanModel getBeanModelByType(TsModel model, TsType type) {
        for (TsBeanModel curBean : model.getBeans()) {
            if (curBean.getName().equals(type)) {
                return curBean;
            }
        }
        return null;
    }

    /**
     * return a class name formatted for rendering in code
     * as part of another class name (so, for generics, strip
     * the type arguments)
     */
    private static String getBeanModelClassName(TsBeanModel bean) {
        return bean.getName() instanceof GenericReferenceType
            ? ((GenericReferenceType)bean.getName()).symbol.toString()
            : bean.getName().toString();
    }

    private static void writeBeanProperty(
        Writer writer, Settings settings, TsModel model, TsBeanModel bean,
        TsPropertyModel property) {
        TsBeanModel fieldBeanModel = getBeanModelByType(model, property.getTsType());
        String fieldClassName = fieldBeanModel != null ? getBeanModelClassName(fieldBeanModel) : "";
        writer.writeIndentedLine(
            settings.indentString + property.getName() + " = new " + fieldClassName + "Fields(this, \"" + property.getName() + "\");");
    }

    private static void createBeanFieldConstant(Writer writer, TsBeanModel bean) {
        writer.writeIndentedLine("export const " + getBeanModelClassName(bean) + " = new " + getBeanModelClassName(bean) + "Fields();");
    }
}
