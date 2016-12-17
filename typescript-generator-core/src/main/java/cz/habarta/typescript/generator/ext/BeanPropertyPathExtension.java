package cz.habarta.typescript.generator.ext;

import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.emitter.EmitterExtension;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.emitter.TsBeanModel;
import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.emitter.TsPropertyModel;
import java.util.*;

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

        Set<TsBeanModel> emittedBeans = new HashSet<>();
        for (TsBeanModel bean : model.getBeans()) {
            emittedBeans.addAll(
                writeBeanAndParentsFieldSpecs(writer, settings, model, emittedBeans, bean));
        }
        for (TsBeanModel bean : model.getBeans()) {
            createBeanFieldConstant(writer, exportKeyword, bean);
        }
    }

    private static void emitFieldsClass(Writer writer, Settings settings) {
        List<String> fieldsClassLines = Arrays.asList(
            "export class Fields {",
            "    protected $$parent: Fields | undefined;",
            "    protected $$name: string;",
            "    constructor(parent?: Fields, name?: string) {",
            "        this.$$parent = parent;",
            "        this.$$name = name || '';",
            "    };",
            "    get(): string {",
            "        if (this.$$parent && this.$$parent.get().length > 0) {",
            "            return this.$$parent.get() + \".\" + this.$$name;",
            "        } else {",
            "            return this.$$name;",
            "        }",
            "    }",
            "}");
        writer.writeIndentedLine("");
        for (String fieldsClassLine : fieldsClassLines) {
            writer.writeIndentedLine(fieldsClassLine.replace("    ", settings.indentString));
        }
    }

    /**
     * Emits a bean and its parent beans before if needed.
     * Returns the list of beans that were emitted.
     */
    private static Set<TsBeanModel> writeBeanAndParentsFieldSpecs(
        Writer writer, Settings settings, TsModel model, Set<TsBeanModel> emittedSoFar, TsBeanModel bean) {
        if (emittedSoFar.contains(bean)) {
            return new HashSet<>();
        }
        final TsBeanModel parentBean = getBeanModelByType(model, bean.getParent());
        final Set<TsBeanModel> emittedBeans = parentBean != null
            ? writeBeanAndParentsFieldSpecs(writer, settings, model, emittedSoFar, parentBean)
            : new HashSet<TsBeanModel>();
        final String parentClassName = parentBean != null
            ? getBeanModelClassName(parentBean) + "Fields"
            : "Fields";
        writer.writeIndentedLine("");
        writer.writeIndentedLine(
            "class " + getBeanModelClassName(bean) + "Fields extends " + parentClassName + " {");
        writer.writeIndentedLine(
            settings.indentString + "constructor(parent?: Fields, name?: string) { super(parent, name); }");
        for (TsPropertyModel property : bean.getProperties()) {
            writeBeanProperty(writer, settings, model, bean, property);
        }
        writer.writeIndentedLine("}");

        emittedBeans.add(bean);
        return emittedBeans;
    }

    /**
     * is this type an 'original' TS type, or a contextual information?
     * null, undefined and optional info are not original types, everything
     * else is original
     */
    private static boolean isOriginalTsType(TsType type) {
        if (type instanceof TsType.BasicType) {
            TsType.BasicType basicType = (TsType.BasicType)type;
            return !(basicType.name.equals("null") || basicType.name.equals("undefined"));
        }
        return true;
    }

    /**
     * If the type is optional of number|null|undefined, or list of
     * of integer, we want to be able to recognize it as number
     * to link the member to another class.
     * => extract the original type while ignoring the |null|undefined
     * and optional informations.
     */
    private static TsType extractOriginalTsType(TsType type) {
        if (type instanceof TsType.OptionalType) {
            return extractOriginalTsType(((TsType.OptionalType)type).type);
        }
        if (type instanceof TsType.UnionType) {
            TsType.UnionType union = (TsType.UnionType)type;
            List<TsType> originalTypes = new ArrayList<>();
            for (TsType curType : union.types) {
                if (isOriginalTsType(curType)) {
                    originalTypes.add(curType);
                }
            }
            return originalTypes.size() == 1
                ? extractOriginalTsType(originalTypes.get(0))
                : type;
        }
        if (type instanceof TsType.BasicArrayType) {
            return extractOriginalTsType(((TsType.BasicArrayType)type).elementType);
        }
        return type;
    }

    private static TsBeanModel getBeanModelByType(TsModel model, TsType type) {
        TsType originalType = extractOriginalTsType(type);
        if (!(originalType instanceof TsType.ReferenceType)) {
            return null;
        }
        TsType.ReferenceType originalTypeBean = (TsType.ReferenceType)originalType;

        for (TsBeanModel curBean : model.getBeans()) {
            if (curBean.getName().equals(originalTypeBean.symbol)) {
                return curBean;
            }
        }
        return null;
    }

    private static String getBeanModelClassName(TsBeanModel bean) {
        return bean.getName().toString();
    }

    private static void writeBeanProperty(
        Writer writer, Settings settings, TsModel model, TsBeanModel bean,
        TsPropertyModel property) {
        TsBeanModel fieldBeanModel = getBeanModelByType(model, property.getTsType());
        String fieldClassName = fieldBeanModel != null ? getBeanModelClassName(fieldBeanModel) : "";
        // if a class has a field of its own type, we get stackoverflow exception
        if (fieldClassName.equals(bean.getName().toString())) {
            fieldClassName = "";
        }
        writer.writeIndentedLine(
            settings.indentString + property.getName() + " = new " + fieldClassName + "Fields(this, \"" + property.getName() + "\");");
    }

    private static void createBeanFieldConstant(Writer writer, boolean exportKeyword, TsBeanModel bean) {
        writer.writeIndentedLine((exportKeyword ? "export " : "")
            + "const " + getBeanModelClassName(bean) + " = new " + getBeanModelClassName(bean) + "Fields();");
    }
}
