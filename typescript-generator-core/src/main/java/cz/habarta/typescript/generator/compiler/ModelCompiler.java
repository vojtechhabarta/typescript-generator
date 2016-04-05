
package cz.habarta.typescript.generator.compiler;

import cz.habarta.typescript.generator.*;
import cz.habarta.typescript.generator.emitter.*;
import cz.habarta.typescript.generator.parser.*;
import java.lang.reflect.*;
import java.util.*;


/**
 * Compiles Java model to TypeScript model.
 * <ol>
 * <li>
 *   Transforms Model to TsModel.
 *   TypeProcessor (chain) is used to transform Java types to TypeScript types.
 *   Symbols are used instead of final type names.
 * </li>
 * <li>
 *   Applies needed transformations:
 *   <ul>
 *     <li>Dates to strings or numbers.</li>
 *     <li>Enums to string literal union types.</li>
 *   </ul>
 * </li>
 * <li>
 *   Resolves Symbols type names. This maps Java class names to TypeScript identifiers using any relevant options from Settings.
 * </li>
 * </ol>
 */
public class ModelCompiler {

    private final Settings settings;
    private final TypeProcessor typeProcessor;

    public ModelCompiler(Settings settings, TypeProcessor typeProcessor) {
        this.settings = settings;
        this.typeProcessor = typeProcessor;
    }

    public TsModel javaToTypeScript(Model model) {
        final SymbolTable symbolTable = new SymbolTable(settings);
        TsModel tsModel = processModel(symbolTable, model);
        tsModel = transformDates(symbolTable, tsModel);
        tsModel = transformEnums(tsModel);
        symbolTable.resolveSymbolNames(settings);
        return tsModel;
    }

    private TsModel processModel(SymbolTable symbolTable, Model model) {
        final List<TsBeanModel> beans = new ArrayList<>();
        for (BeanModel bean : model.getBeans()) {
            beans.add(processBean(symbolTable, bean));
        }
        final List<TsEnumModel> enums = new ArrayList<>();
        for (EnumModel enumModel : model.getEnums()) {
            enums.add(processEnum(symbolTable, enumModel));
        }
        final List<TsAliasModel> typeAliases = new ArrayList<>();
        return new TsModel(beans, enums, typeAliases);
    }

    private TsBeanModel processBean(SymbolTable symbolTable, BeanModel bean) {
        final TsType beanType = typeFromJava(symbolTable, bean.getBeanClass());
        final TsType parentType = typeFromJava(symbolTable, bean.getParent());
        final List<TsPropertyModel> properties = new ArrayList<>();
        for (PropertyModel property : bean.getProperties()) {
            properties.add(processProperty(symbolTable, bean, property));
        }
        return new TsBeanModel(bean.getBeanClass(), beanType, parentType, properties, bean.getComments());
    }

    private TsPropertyModel processProperty(SymbolTable symbolTable, BeanModel bean, PropertyModel property) {
        final TsType type = typeFromJava(symbolTable, property.getType(), property.getName(), bean.getBeanClass());
        final TsType tsType = property.isOptional() ? type.optional() : type;
        return new TsPropertyModel(property.getName(), tsType, property.getComments());
    }

    private TsEnumModel processEnum(SymbolTable symbolTable, EnumModel enumModel) {
        final TsType enumType = typeFromJava(symbolTable, enumModel.getEnumClass());
        return new TsEnumModel(enumModel.getEnumClass(), enumType, enumModel.getComments(), new ArrayList<>(enumModel.getValues()));
    }

    private TsType typeFromJava(SymbolTable symbolTable, Type javaType) {
        return typeFromJava(symbolTable, javaType, null, null);
    }

    private TsType typeFromJava(SymbolTable symbolTable, Type javaType, String usedInProperty, Class<?> usedInClass) {
        if (javaType == null) {
            return null;
        }
        final TypeProcessor.Context context = new TypeProcessor.Context(symbolTable, typeProcessor);
        final TypeProcessor.Result result = context.processType(javaType);
        if (result != null) {
            return result.getTsType();
        } else {
            if (usedInClass != null && usedInProperty != null) {
                System.out.println(String.format("Warning: Unsupported type '%s' used in '%s.%s'", javaType, usedInClass.getSimpleName(), usedInProperty));
            } else {
                System.out.println(String.format("Warning: Unsupported type '%s'", javaType));
            }
            return TsType.Any;
        }
    }

    private TsModel transformDates(SymbolTable symbolTable, TsModel tsModel) {
        final TsAliasModel dateAsNumber = new TsAliasModel(new TsType.ReferenceType(symbolTable.getSyntheticSymbol("DateAsNumber")), TsType.Number, null);
        final TsAliasModel dateAsString = new TsAliasModel(new TsType.ReferenceType(symbolTable.getSyntheticSymbol("DateAsString")), TsType.String, null);
        final LinkedHashSet<TsAliasModel> typeAliases = new LinkedHashSet<>(tsModel.getTypeAliases());
        final TsModel model = transformBeanPropertyTypes(tsModel, new TsType.Transformer() {
            @Override
            public TsType transform(TsType type) {
                if (type == TsType.Date) {
                    if (settings.mapDate == DateMapping.asNumber) {
                        typeAliases.add(dateAsNumber);
                        return dateAsNumber.getName();
                    }
                    if (settings.mapDate == DateMapping.asString) {
                        typeAliases.add(dateAsString);
                        return dateAsString.getName();
                    }
                }
                return type;
                
            }
        });
        return model.setTypeAliases(new ArrayList<>(typeAliases));
    }

    private TsModel transformEnums(TsModel tsModel) {
        final LinkedHashSet<TsAliasModel> typeAliases = new LinkedHashSet<>(tsModel.getTypeAliases());
        for (TsEnumModel enumModel : tsModel.getEnums()) {
            final List<TsType> values = new ArrayList<>();
            for (String value : enumModel.getValues()) {
                values.add(new TsType.StringLiteralType(value));
            }
            final TsType union = new TsType.UnionType(values);
            typeAliases.add(new TsAliasModel(enumModel.getOrigin(), enumModel.getName(), union, enumModel.getComments()));
        }
        return tsModel.setTypeAliases(new ArrayList<>(typeAliases));
    }

    private static TsModel transformBeanPropertyTypes(TsModel tsModel, TsType.Transformer transformer) {
        final List<TsBeanModel> newBeans = new ArrayList<>();
        for (TsBeanModel bean : tsModel.getBeans()) {
            final List<TsPropertyModel> newProperties = new ArrayList<>();
            for (TsPropertyModel property : bean.getProperties()) {
                final TsType newType = TsType.transformTsType(property.getTsType(), transformer);
                newProperties.add(property.setTsType(newType));
            }
            newBeans.add(bean.setProperties(newProperties));
        }
        return tsModel.setBeans(newBeans);
    }

}
