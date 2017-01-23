
package cz.habarta.typescript.generator.compiler;

import cz.habarta.typescript.generator.*;
import cz.habarta.typescript.generator.emitter.*;
import cz.habarta.typescript.generator.parser.*;
import cz.habarta.typescript.generator.util.Utils;
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
        tsModel = removeInheritedProperties(symbolTable, tsModel);
        tsModel = addImplementedProperties(symbolTable, tsModel);

        // JAX-RS
        if (settings.generateJaxrsApplicationInterface) {
            tsModel = createJaxrsInterface(symbolTable, tsModel, model);
        }

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
        tsModel = sortDeclarations(symbolTable, tsModel);
        return tsModel;
    }

    public TsType javaToTypeScript(Type type) {
        final BeanModel beanModel = new BeanModel(Object.class, Object.class, null, null, null, Collections.<Type>emptyList(), Collections.singletonList(new PropertyModel("property", type, false, null, null)), null);
        final Model model = new Model(Collections.singletonList(beanModel), Collections.<EnumModel<?>>emptyList(), null);
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
            for (Type ancestor : bean.getParentAndInterfaces()) {
                if (!children.containsKey(ancestor)) {
                    children.put(ancestor, new ArrayList<BeanModel>());
                }
                children.get(ancestor).add(bean);
            }
        }
        return children;
    }

    private <T> TsBeanModel processBean(SymbolTable symbolTable, Map<Type, List<BeanModel>> children, BeanModel bean) {
        final boolean isClass = !bean.getOrigin().isInterface() && settings.mapClasses == ClassMapping.asClasses;
        final Symbol beanIdentifier = symbolTable.getSymbol(bean.getOrigin());
        final List<TsType.GenericVariableType> typeParameters = new ArrayList<>();
        for (TypeVariable<?> typeParameter : bean.getOrigin().getTypeParameters()) {
            typeParameters.add(new TsType.GenericVariableType(typeParameter.getName()));
        }
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

        return new TsBeanModel(bean.getOrigin(), isClass, beanIdentifier, typeParameters, parentType, bean.getTaggedUnionClasses(), interfaces, properties, null, bean.getComments());
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
        final Symbol beanIdentifier = symbolTable.getSymbol(enumModel.getOrigin());
        return TsEnumModel.fromEnumModel(beanIdentifier, enumModel);
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

    private TsModel removeInheritedProperties(SymbolTable symbolTable, TsModel tsModel) {
        final List<TsBeanModel> beans = new ArrayList<>();
        for (TsBeanModel bean : tsModel.getBeans()) {
            final Map<String, TsType> inheritedPropertyTypes = getInheritedProperties(symbolTable, tsModel, bean.getParentAndInterfaces());
            final List<TsPropertyModel> properties = new ArrayList<>();
            for (TsPropertyModel property : bean.getProperties()) {
                if (!Objects.equals(property.getTsType(), inheritedPropertyTypes.get(property.getName()))) {
                    properties.add(property);
                }
            }
            beans.add(bean.withProperties(properties));
        }
        return tsModel.setBeans(beans);
    }

    private TsModel addImplementedProperties(SymbolTable symbolTable, TsModel tsModel) {
        final List<TsBeanModel> beans = new ArrayList<>();
        for (TsBeanModel bean : tsModel.getBeans()) {
            if (bean.isClass()) {
                final List<TsPropertyModel> resultProperties = new ArrayList<>(bean.getProperties());
                
                final Set<String> classPropertyNames = new LinkedHashSet<>();
                for (TsPropertyModel property : bean.getProperties()) {
                    classPropertyNames.add(property.getName());
                }
                classPropertyNames.addAll(getInheritedProperties(symbolTable, tsModel, Utils.listFromNullable(bean.getParent())).keySet());
                
                final List<TsPropertyModel> implementedProperties = getImplementedProperties(symbolTable, tsModel, bean.getInterfaces());
                Collections.reverse(implementedProperties);
                for (TsPropertyModel implementedProperty : implementedProperties) {
                    if (!classPropertyNames.contains(implementedProperty.getName())) {
                        resultProperties.add(0, implementedProperty);
                        classPropertyNames.add(implementedProperty.getName());
                    }
                }
                
                beans.add(bean.withProperties(resultProperties));
            } else {
                beans.add(bean);
            }
        }
        return tsModel.setBeans(beans);
    }

    private static Map<String, TsType> getInheritedProperties(SymbolTable symbolTable, TsModel tsModel, List<TsType> parents) {
        final Map<String, TsType> properties = new LinkedHashMap<>();
        for (TsType parentType : parents) {
            final TsBeanModel parent = tsModel.getBean(getOriginClass(symbolTable, parentType));
            if (parent != null) {
                properties.putAll(getInheritedProperties(symbolTable, tsModel, parent.getParentAndInterfaces()));
                for (TsPropertyModel property : parent.getProperties()) {
                    properties.put(property.getName(), property.getTsType());
                }
            }
        }
        return properties;
    }

    private static List<TsPropertyModel> getImplementedProperties(SymbolTable symbolTable, TsModel tsModel, List<TsType> interfaces) {
        final List<TsPropertyModel> properties = new ArrayList<>();
        for (TsType aInterface : interfaces) {
            final TsBeanModel bean = tsModel.getBean(getOriginClass(symbolTable, aInterface));
            if (bean != null) {
                properties.addAll(getImplementedProperties(symbolTable, tsModel, bean.getInterfaces()));
                properties.addAll(bean.getProperties());
            }
        }
        return properties;
    }

    private TsModel createJaxrsInterface(SymbolTable symbolTable, TsModel tsModel, Model model) {
        final Symbol responseSymbol = symbolTable.getSyntheticSymbol("RestResponse");
        final TsType.GenericVariableType varR = new TsType.GenericVariableType("R");
        final TsType.GenericReferenceType responseTypeDefinition = new TsType.GenericReferenceType(symbolTable.getSyntheticSymbol("Promise"), Arrays.<TsType>asList(varR));
        final TsAliasModel responseTypeAlias = new TsAliasModel(null, responseSymbol, Arrays.asList(varR), responseTypeDefinition, null);
        tsModel.getTypeAliases().add(responseTypeAlias);
        final JaxrsApplicationModel jaxrsApplication = model.getJaxrsApplication();
        if (jaxrsApplication == null || jaxrsApplication.getMethods().isEmpty()) {
            return tsModel;
        }
        final String applicationPath = jaxrsApplication.getApplicationPath();
        final String pathPrefix = applicationPath != null && !applicationPath.isEmpty() ? applicationPath + "/" : "";
        final List<TsMethodModel> methods = new ArrayList<>();
        final Map<String, Long> methodNamesCount = groupingByMethodName(jaxrsApplication.getMethods());
        for (JaxrsMethodModel method : jaxrsApplication.getMethods()) {
            final boolean createLongName = methodNamesCount.get(method.getName()) > 1;
            methods.add(processJaxrsMethod(symbolTable, pathPrefix, responseSymbol, method, createLongName));
        }
        final String applicationName = jaxrsApplication.getApplicationName() != null ? jaxrsApplication.getApplicationName() : "RestApplication";
        final TsBeanModel interfaceModel = new TsBeanModel(null, false, symbolTable.getSyntheticSymbol(applicationName), null, null, null, null, null, methods, null);
        tsModel.getBeans().add(interfaceModel);
        return tsModel;
    }

    private static Map<String, Long> groupingByMethodName(List<JaxrsMethodModel> methods) {
//        return methods.stream().collect(Collectors.groupingBy(JaxrsMethodModel::getName, Collectors.counting()));
        final Map<String, Long> methodNamesCount = new LinkedHashMap<>();
        for (JaxrsMethodModel method : methods) {
            final String name = method.getName();
            final long count = methodNamesCount.containsKey(name) ? methodNamesCount.get(name) : 0;
            methodNamesCount.put(name, count + 1);
        }
        return methodNamesCount;
    }
        

    private TsMethodModel processJaxrsMethod(SymbolTable symbolTable, String pathPrefix, Symbol responseSymbol, JaxrsMethodModel method, boolean createLongName) {
        final List<String> comments = new ArrayList<>();
        final String path = pathPrefix + method.getPath();
        comments.add("HTTP " + method.getHttpMethod() + " /" + path);
        final List<TsParameterModel> parameters = new ArrayList<>();
        // path params
        for (MethodParameterModel parameter : method.getPathParams()) {
            parameters.add(processParameter(symbolTable, method, parameter));
        }
        // query params
        final List<MethodParameterModel> queryParams = method.getQueryParams();
        if (queryParams != null && !queryParams.isEmpty()) {
            final List<TsProperty> properties = new ArrayList<>();
            for (MethodParameterModel queryParam : queryParams) {
                final TsType type = typeFromJava(symbolTable, queryParam.getType(), method.getName(), method.getOriginClass());
                properties.add(new TsProperty(queryParam.getName(), new TsType.OptionalType(type)));
            }
            final TsParameterModel queryParameter = new TsParameterModel("queryParams", new TsType.OptionalType(new TsType.ObjectType(properties)));
            parameters.add(queryParameter);
        }
        // entity param
        if (method.getEntityParam() != null) {
            parameters.add(processParameter(symbolTable, method, method.getEntityParam()));
        }
        // return type
        final TsType returnType = typeFromJava(symbolTable, method.getReturnType(), method.getName(), method.getOriginClass());
        final TsType wrappedReturnType = new TsType.GenericReferenceType(responseSymbol, Arrays.asList(returnType));
        // method name
        final String nameSuffix;
        if (createLongName) {
            nameSuffix = "$" + method.getHttpMethod() + "$" + path
                    .replaceAll(JaxrsApplicationParser.PathParamPattern.pattern(), "${" + JaxrsApplicationParser.PathParamNameGroupName + "}")
                    .replaceAll("/", "_")
                    .replaceAll("\\W", "");
        } else {
            nameSuffix = "";
        }
        // method
        final TsMethodModel tsMethodModel = new TsMethodModel(method.getName() + nameSuffix, wrappedReturnType, parameters, comments);
        return tsMethodModel;
    }

    private TsParameterModel processParameter(SymbolTable symbolTable, MethodModel method, MethodParameterModel parameter) {
        final TsType parameterType = typeFromJava(symbolTable, parameter.getType(), method.getName(), method.getOriginClass());
        return new TsParameterModel(parameter.getName(), parameterType);
    }

    private TsModel transformDates(SymbolTable symbolTable, TsModel tsModel) {
        final TsAliasModel dateAsNumber = new TsAliasModel(null, symbolTable.getSyntheticSymbol("DateAsNumber"), null, TsType.Number, null);
        final TsAliasModel dateAsString = new TsAliasModel(null, symbolTable.getSyntheticSymbol("DateAsString"), null, TsType.String, null);
        final LinkedHashSet<TsAliasModel> typeAliases = new LinkedHashSet<>(tsModel.getTypeAliases());
        final TsModel model = transformBeanPropertyTypes(tsModel, new TsType.Transformer() {
            @Override
            public TsType transform(TsType type) {
                if (type == TsType.Date) {
                    if (settings.mapDate == DateMapping.asNumber) {
                        typeAliases.add(dateAsNumber);
                        return new TsType.ReferenceType(dateAsNumber.getName());
                    }
                    if (settings.mapDate == DateMapping.asString) {
                        typeAliases.add(dateAsString);
                        return new TsType.ReferenceType(dateAsString.getName());
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
            typeAliases.add(new TsAliasModel(enumModel.getOrigin(), enumModel.getName(), null, union, enumModel.getComments()));
        }
        return tsModel.setTypeAliases(new ArrayList<>(typeAliases));
    }

    private TsModel inlineEnums(final TsModel tsModel, final SymbolTable symbolTable) {
        final Set<TsAliasModel> inlinedAliases = new LinkedHashSet<>();
        final TsModel newTsModel = transformBeanPropertyTypes(tsModel, new TsType.Transformer() {
            @Override
            public TsType transform(TsType tsType) {
                if (tsType instanceof TsType.EnumReferenceType) {
                    final TsAliasModel alias = tsModel.getTypeAlias(getOriginClass(symbolTable, tsType));
                    if (alias != null) {
                        inlinedAliases.add(alias);
                        return alias.getDefinition();
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
            if (!bean.getTaggedUnionClasses().isEmpty()) {
                final Symbol unionName = symbolTable.getSymbol(bean.getOrigin(), "Union");
                final List<TsType> unionTypes = new ArrayList<>();
                for (Class<?> cls : bean.getTaggedUnionClasses()) {
                    final TsType type = new TsType.ReferenceType(symbolTable.getSymbol(cls));
                    unionTypes.add(type);
                }
                final TsType.UnionType union = new TsType.UnionType(unionTypes);
                typeAliases.add(new TsAliasModel(bean.getOrigin(), unionName, null, union, null));
            }
        }
        // use tagged unions
        final TsModel model = transformBeanPropertyTypes(tsModel, new TsType.Transformer() {
            @Override
            public TsType transform(TsType tsType) {
                final Class<?> cls = getOriginClass(symbolTable, tsType);
                if (cls != null && !(tsType instanceof TsType.GenericReferenceType)) {
                    final Symbol unionSymbol = symbolTable.hasSymbol(cls, "Union");
                    if (unionSymbol != null) {
                        return new TsType.ReferenceType(unionSymbol);
                    }
                }
                return tsType;
            }
        });
        return model.setTypeAliases(new ArrayList<>(typeAliases));
    }

    private TsModel sortDeclarations(SymbolTable symbolTable, TsModel tsModel) {
        final List<TsBeanModel> beans = tsModel.getBeans();
        final List<TsAliasModel> aliases = tsModel.getTypeAliases();
        final List<TsEnumModel<?>> enums = tsModel.getEnums();
        if (settings.sortDeclarations) {
            for (TsBeanModel bean : beans) {
                Collections.sort(bean.getProperties());
            }
        }
        if (settings.sortDeclarations || settings.sortTypeDeclarations) {
            Collections.sort(beans);
            Collections.sort(aliases);
            Collections.sort(enums);
        }
        final LinkedHashSet<TsBeanModel> orderedBeans = new LinkedHashSet<>();
        for (TsBeanModel bean : beans) {
            addOrderedClass(symbolTable, tsModel, bean, orderedBeans);
        }
        return tsModel
                    .setBeans(new ArrayList<>(orderedBeans))
                    .setTypeAliases(aliases)
                    .setEnums(enums);
    }

    private static void addOrderedClass(SymbolTable symbolTable, TsModel tsModel, TsBeanModel bean, LinkedHashSet<TsBeanModel> orderedBeans) {
        // for classes first add their parents to ordered list
        if (bean.isClass() && bean.getParent() != null) {
            final TsBeanModel parentBean = tsModel.getBean(getOriginClass(symbolTable, bean.getParent()));
            if (parentBean != null) {
                addOrderedClass(symbolTable, tsModel, parentBean, orderedBeans);
            }
        }
        // add current bean to the ordered list
        orderedBeans.add(bean);
    }

    private static TsModel transformBeanPropertyTypes(TsModel tsModel, TsType.Transformer transformer) {
        final List<TsBeanModel> newBeans = new ArrayList<>();
        for (TsBeanModel bean : tsModel.getBeans()) {
            final List<TsPropertyModel> newProperties = new ArrayList<>();
            for (TsPropertyModel property : bean.getProperties()) {
                final TsType newType = TsType.transformTsType(property.getTsType(), transformer);
                newProperties.add(property.setTsType(newType));
            }
            final List<TsMethodModel> newMethods = new ArrayList<>();
            for (TsMethodModel method : bean.getMethods()) {
                final List<TsParameterModel> newParameters = new ArrayList<>();
                for (TsParameterModel parameter : method.getParameters()) {
                    final TsType newParameterType = TsType.transformTsType(parameter.getTsType(), transformer);
                    newParameters.add(new TsParameterModel(parameter.getName(), newParameterType));
                }
                final TsType newReturnType = TsType.transformTsType(method.getReturnType(), transformer);
                newMethods.add(new TsMethodModel(method.getName(), newReturnType, newParameters, method.getComments()));
            }
            newBeans.add(bean.withProperties(newProperties).withMethods(newMethods));
        }
        return tsModel.setBeans(newBeans);
    }

    private static Class<?> getOriginClass(SymbolTable symbolTable, TsType type) {
        if (type instanceof TsType.ReferenceType) {
            final TsType.ReferenceType referenceType = (TsType.ReferenceType) type;
            return symbolTable.getSymbolClass(referenceType.symbol);
        }
        return null;
    }

}
