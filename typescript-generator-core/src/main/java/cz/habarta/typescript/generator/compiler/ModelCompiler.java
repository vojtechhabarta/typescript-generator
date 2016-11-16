
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

        // dates
        tsModel = transformDates(symbolTable, tsModel);

        // enums
        if (!settings.areDefaultStringEnumsOverriddenByExtension()) {
            if (settings.mapEnum == null || settings.mapEnum == EnumMapping.asUnion || settings.mapEnum == EnumMapping.asInlineUnion) {
                tsModel = transformEnumsToUnions(tsModel);
            }
            if (settings.mapEnum == EnumMapping.asInlineUnion) {
                tsModel = inlineEnums(tsModel, symbolTable);
            }
        }

        // tagged unions
        tsModel = createAndUseTaggedUnions(symbolTable, tsModel);

        symbolTable.resolveSymbolNames();
        return tsModel;
    }

    public TsType javaToTypeScript(Type type) {
        final BeanModel beanModel = new BeanModel(Object.class, Object.class, null, null, null, Collections.<Type>emptyList(), Collections.singletonList(new PropertyModel("property", type, false, null, null)), null);
        final Model model = new Model(Collections.singletonList(beanModel), Collections.<EnumModel<?>>emptyList());
        final TsModel tsModel = javaToTypeScript(model);
        return tsModel.getBeans().get(0).getProperties().get(0).getTsType();
    }

    private TsModel processModel(SymbolTable symbolTable, Model model) {
        final Map<Type, List<BeanModel>> children = createChildrenMap(model);
        final List<TsBeanModel> beans = new ArrayList<>();
        for (BeanModel bean : model.getBeans()) {
            beans.add(processBean(symbolTable, children, bean));
        }
        final List<TsEnumModel<?>> enums = new ArrayList<>();
        for (EnumModel<?> enumModel : model.getEnums()) {
            enums.add(processEnum(symbolTable, enumModel));
        }
        final List<TsAliasModel> typeAliases = new ArrayList<>();
        return new TsModel(beans, enums, typeAliases);
    }

    private Map<Type, List<BeanModel>> createChildrenMap(Model model) {
        final Map<Type, List<BeanModel>> children = new LinkedHashMap<>();
        for (BeanModel bean : model.getBeans()) {
            for (Type ancestor : bean.getDirectAncestors()) {
                if (!children.containsKey(ancestor)) {
                    children.put(ancestor, new ArrayList<BeanModel>());
                }
                children.get(ancestor).add(bean);
            }
        }
        return children;
    }

    private TsBeanModel processBean(SymbolTable symbolTable, Map<Type, List<BeanModel>> children, BeanModel bean) {
        final TsType beanType = typeFromJava(symbolTable, bean.getOrigin());
        TsType parentType = typeFromJava(symbolTable, bean.getParent());
        if (parentType != null && parentType.equals(TsType.Any)) {
            parentType = null;
        }
        final List<TsType> interfaces = new ArrayList<>();
        for (Type aInterface : bean.getInterfaces()) {
            final TsType interfaceType = typeFromJava(symbolTable, aInterface);
            if (!interfaceType.equals(TsType.Any)) {
                interfaces.add(interfaceType);
            }
        }
        final List<TsPropertyModel> properties = new ArrayList<>();
        for (PropertyModel property : bean.getProperties()) {
            properties.add(processProperty(symbolTable, bean, property));
        }

        if (bean.getDiscriminantProperty() != null && !containsProperty(properties, bean.getDiscriminantProperty())) {
            final List<BeanModel> selfAndDescendants = getSelfAndDescendants(bean, children);
            final List<TsType.StringLiteralType> literals = new ArrayList<>();
            for (BeanModel descendant : selfAndDescendants) {
                if (descendant.getDiscriminantLiteral() != null) {
                    literals.add(new TsType.StringLiteralType(descendant.getDiscriminantLiteral()));
                }
            }
            final TsType discriminantType = literals.isEmpty()
                    ? TsType.String
                    : new TsType.UnionType(literals);
            properties.add(0, new TsPropertyModel(bean.getDiscriminantProperty(), discriminantType, null));
        }

        return new TsBeanModel(bean.getOrigin(), beanType, parentType, bean.getTaggedUnionClasses(), interfaces, properties, bean.getComments());
    }

    private static List<BeanModel> getSelfAndDescendants(BeanModel bean, Map<Type, List<BeanModel>> children) {
        final List<BeanModel> descendants = new ArrayList<>();
        descendants.add(bean);
        final List<BeanModel> directDescendants = children.get(bean.getOrigin());
        if (directDescendants != null) {
            for (BeanModel descendant : directDescendants) {
                descendants.addAll(getSelfAndDescendants(descendant, children));
            }
        }
        return descendants;
    }

    private static boolean containsProperty(List<TsPropertyModel> properties, String propertyName) {
        for (TsPropertyModel property : properties) {
            if (property.getName().equals(propertyName)) {
                return true;
            }
        }
        return false;
    }

    private TsPropertyModel processProperty(SymbolTable symbolTable, BeanModel bean, PropertyModel property) {
        final TsType type = typeFromJava(symbolTable, property.getType(), property.getName(), bean.getOrigin());
        final TsType tsType = property.isOptional() ? type.optional() : type;
        return new TsPropertyModel(property.getName(), tsType, property.getComments());
    }

    private TsEnumModel<?> processEnum(SymbolTable symbolTable, EnumModel<?> enumModel) {
        final TsType enumType = typeFromJava(symbolTable, enumModel.getOrigin());
        return TsEnumModel.fromEnumModel(enumType, enumModel);
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

    private TsModel transformEnumsToUnions(TsModel tsModel) {
        final LinkedHashSet<TsAliasModel> typeAliases = new LinkedHashSet<>(tsModel.getTypeAliases());
        for (TsEnumModel<String> enumModel : tsModel.getEnums(EnumKind.StringBased)) {
            final List<TsType> values = new ArrayList<>();
            for (EnumMemberModel<String> member : enumModel.getMembers()) {
                values.add(new TsType.StringLiteralType(member.getEnumValue()));
            }
            final TsType union = new TsType.UnionType(values);
            typeAliases.add(new TsAliasModel(enumModel.getOrigin(), enumModel.getName(), union, enumModel.getComments()));
        }
        return tsModel.setTypeAliases(new ArrayList<>(typeAliases));
    }

    private TsModel inlineEnums(final TsModel tsModel, final SymbolTable symbolTable) {
        final Set<TsAliasModel> inlinedAliases = new LinkedHashSet<>();
        final TsModel newTsModel = transformBeanPropertyTypes(tsModel, new TsType.Transformer() {
            @Override
            public TsType transform(TsType tsType) {
                if (tsType instanceof TsType.EnumReferenceType) {
                    final TsType.ReferenceType reference = (TsType.ReferenceType) tsType;
                    final Class<?> cls = symbolTable.getSymbolClass(reference.symbol);
                    if (cls != null) {
                        for (TsAliasModel alias : tsModel.getTypeAliases()) {
                            if (alias.getOrigin() == cls) {
                                inlinedAliases.add(alias);
                                return alias.getDefinition();
                            }
                        }
                    }
                }
                return tsType;
            }
        });
        final ArrayList<TsAliasModel> aliases = new ArrayList<>(tsModel.getTypeAliases());
        aliases.removeAll(inlinedAliases);
        return newTsModel.setTypeAliases(aliases);
    }

    private TsModel createAndUseTaggedUnions(final SymbolTable symbolTable, TsModel tsModel) {
        if (settings.disableTaggedUnions) {
            return tsModel;
        }
        // create tagged unions
        final LinkedHashSet<TsAliasModel> typeAliases = new LinkedHashSet<>(tsModel.getTypeAliases());
        for (TsBeanModel bean : tsModel.getBeans()) {
            if (bean.getTaggedUnionClasses() != null) {
                if (bean.getName() instanceof TsType.ReferenceType) {
                    final TsType.ReferenceType unionName = new TsType.ReferenceType(symbolTable.getSymbol(bean.getOrigin(), "Union"));
                    final List<TsType> unionTypes = new ArrayList<>();
                    for (Class<?> cls : bean.getTaggedUnionClasses()) {
                        final TsType type = new TsType.ReferenceType(symbolTable.getSymbol(cls));
                        unionTypes.add(type);
                    }
                    final TsType.UnionType union = new TsType.UnionType(unionTypes);
                    typeAliases.add(new TsAliasModel(bean.getOrigin(), unionName, union, null));
                }
            }
        }
        // use tagged unions
        final TsModel model = transformBeanPropertyTypes(tsModel, new TsType.Transformer() {
            @Override
            public TsType transform(TsType tsType) {
                if (tsType instanceof TsType.ReferenceType) {
                    final TsType.ReferenceType referenceType = (TsType.ReferenceType) tsType;
                    if (!(referenceType instanceof TsType.GenericReferenceType)) {
                        final Class<?> cls = symbolTable.getSymbolClass(referenceType.symbol);
                        final Symbol unionSymbol = symbolTable.hasSymbol(cls, "Union");
                        if (unionSymbol != null) {
                            return new TsType.ReferenceType(unionSymbol);
                        }
                    }
                }
                return tsType;
            }
        });
        return model.setTypeAliases(new ArrayList<>(typeAliases));
    }

    private static TsModel transformBeanPropertyTypes(TsModel tsModel, TsType.Transformer transformer) {
        final List<TsBeanModel> newBeans = new ArrayList<>();
        for (TsBeanModel bean : tsModel.getBeans()) {
            final List<TsPropertyModel> newProperties = new ArrayList<>();
            for (TsPropertyModel property : bean.getProperties()) {
                final TsType newType = TsType.transformTsType(property.getTsType(), transformer);
                newProperties.add(property.setTsType(newType));
            }
            newBeans.add(bean.withProperties(newProperties));
        }
        return tsModel.setBeans(newBeans);
    }

}
