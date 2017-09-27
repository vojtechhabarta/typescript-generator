
package cz.habarta.typescript.generator.compiler;

import cz.habarta.typescript.generator.*;
import cz.habarta.typescript.generator.emitter.*;
import cz.habarta.typescript.generator.parser.*;
import cz.habarta.typescript.generator.util.Utils;
import java.lang.annotation.Annotation;
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
        if (settings.generateJaxrsApplicationInterface || settings.generateJaxrsApplicationClient) {
            final JaxrsApplicationModel jaxrsApplication = model.getJaxrsApplication() != null ? model.getJaxrsApplication() : new JaxrsApplicationModel();
            final Symbol responseSymbol = createJaxrsResponseType(symbolTable, tsModel);
            final TsType optionsType = settings.restOptionsType != null
                    ? new TsType.VerbatimType(settings.restOptionsType)
                    : null;
            final TsType.GenericVariableType optionsGenericVariable = settings.restOptionsTypeIsGeneric
                    ? new TsType.GenericVariableType(settings.restOptionsType)
                    : null;

            if (settings.generateJaxrsApplicationInterface) {
                tsModel = createJaxrsInterfaces(symbolTable, tsModel, jaxrsApplication, responseSymbol, optionsGenericVariable, optionsType);
            }
            if (settings.generateJaxrsApplicationClient) {
                tsModel = createJaxrsClients(symbolTable, tsModel, jaxrsApplication, responseSymbol, optionsGenericVariable, optionsType);
            }
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
            if (settings.mapEnum == EnumMapping.asNumberBasedEnum) {
                tsModel = transformEnumsToNumberBasedEnum(tsModel);
            }
        }

        // tagged unions
        tsModel = createAndUseTaggedUnions(symbolTable, tsModel);

        symbolTable.resolveSymbolNames();
        tsModel = sortDeclarations(symbolTable, tsModel);
        return tsModel;
    }

    public TsType javaToTypeScript(Type type) {
        final BeanModel beanModel = new BeanModel(Object.class, Object.class, null, null, null, Collections.<Type>emptyList(),
                Collections.singletonList(new PropertyModel("property", type, false, null, null, null)), null);
        final Model model = new Model(Collections.singletonList(beanModel), Collections.<EnumModel>emptyList(), null);
        final TsModel tsModel = javaToTypeScript(model);
        return tsModel.getBeans().get(0).getProperties().get(0).getTsType();
    }

    private TsModel processModel(SymbolTable symbolTable, Model model) {
        final Map<Type, List<BeanModel>> children = createChildrenMap(model);
        final List<TsBeanModel> beans = new ArrayList<>();
        for (BeanModel bean : model.getBeans()) {
            beans.add(processBean(symbolTable, model, children, bean));
        }
        final List<TsEnumModel> enums = new ArrayList<>();
        final List<TsEnumModel> stringEnums = new ArrayList<>();
        for (EnumModel enumModel : model.getEnums()) {
            final TsEnumModel tsEnumModel = processEnum(symbolTable, enumModel);
            enums.add(tsEnumModel);
            if (tsEnumModel.getKind() == EnumKind.StringBased) {
                stringEnums.add(tsEnumModel);
            }
        }
        return new TsModel().withBeans(beans).withEnums(enums).withOriginalStringEnums(stringEnums);
    }

    private Map<Type, List<BeanModel>> createChildrenMap(Model model) {
        final Map<Type, List<BeanModel>> children = new LinkedHashMap<>();
        for (BeanModel bean : model.getBeans()) {
            for (Type ancestor : bean.getParentAndInterfaces()) {
                Type processedAncestor = processTypeForDescendantLookup(ancestor);

                if (!children.containsKey(processedAncestor)) {
                    children.put(processedAncestor, new ArrayList<BeanModel>());
                }
                children.get(processedAncestor).add(bean);
            }
        }
        return children;
    }

    private <T> TsBeanModel processBean(SymbolTable symbolTable, Model model, Map<Type, List<BeanModel>> children, BeanModel bean) {
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
        final List<TsPropertyModel> properties = processProperties(symbolTable, model, bean, "", "");

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
            properties.add(0, new TsPropertyModel(bean.getDiscriminantProperty(), discriminantType, settings.declarePropertiesAsReadOnly, /*ownProperty*/ true, null));
        }

        return new TsBeanModel(bean.getOrigin(), TsBeanCategory.Data, isClass, beanIdentifier, typeParameters, parentType, bean.getTaggedUnionClasses(), interfaces, properties, null, null, bean.getComments());
    }

    private List<TsPropertyModel> processProperties(SymbolTable symbolTable, Model model, BeanModel bean, String prefix, String suffix) {
        final List<TsPropertyModel> properties = new ArrayList<>();
        for (PropertyModel property : bean.getProperties()) {
            boolean pulled = false;
            final PropertyModel.PullProperties pullProperties = property.getPullProperties();
            if (pullProperties != null) {
                if (property.getType() instanceof Class<?>) {
                    final BeanModel pullBean = model.getBean((Class<?>) property.getType());
                    if (pullBean != null) {
                        properties.addAll(processProperties(symbolTable, model, pullBean, prefix + pullProperties.prefix, pullProperties.suffix + suffix));
                        pulled = true;
                    }
                }
            }
            if (!pulled) {
                properties.add(processProperty(symbolTable, bean, property, prefix, suffix));
            }
        }
        return properties;
    }

    /**
     * Given a type, returns the type that should be used for the purpose of looking up implementations of that type.
     */
    private static Type processTypeForDescendantLookup(Type type) {
        if (type instanceof ParameterizedType) {
            return ((ParameterizedType) type).getRawType();
        } else {
            return type;
        }
    }

    private static List<BeanModel> getSelfAndDescendants(BeanModel bean, Map<Type, List<BeanModel>> children) {
        final List<BeanModel> descendants = new ArrayList<>();
        descendants.add(bean);
        final List<BeanModel> directDescendants = children.get(processTypeForDescendantLookup(bean.getOrigin()));
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

    private TsPropertyModel processProperty(SymbolTable symbolTable, BeanModel bean, PropertyModel property, String prefix, String suffix) {
        final TsType type = typeFromJava(symbolTable, property.getType(), property.getName(), bean.getOrigin());
        final TsType tsType = property.isOptional() ? type.optional() : type;
        return new TsPropertyModel(prefix + property.getName() + suffix, tsType, settings.declarePropertiesAsReadOnly, false, property.getComments());
    }

    private TsEnumModel processEnum(SymbolTable symbolTable, EnumModel enumModel) {
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
                if (property.isOwnProperty() || !Objects.equals(property.getTsType(), inheritedPropertyTypes.get(property.getName()))) {
                    properties.add(property);
                }
            }
            beans.add(bean.withProperties(properties));
        }
        return tsModel.withBeans(beans);
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
        return tsModel.withBeans(beans);
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

    private Symbol createJaxrsResponseType(SymbolTable symbolTable, TsModel tsModel) {
        // response type
        final Symbol responseSymbol = symbolTable.getSyntheticSymbol("RestResponse");
        final TsType.GenericVariableType varR = new TsType.GenericVariableType("R");
        final TsAliasModel responseTypeAlias;
        if (settings.restResponseType != null) {
            responseTypeAlias = new TsAliasModel(null, responseSymbol, Arrays.asList(varR), new TsType.VerbatimType(settings.restResponseType), null);
        } else {
            final TsType.GenericReferenceType responseTypeDefinition = new TsType.GenericReferenceType(symbolTable.getSyntheticSymbol("Promise"), varR);
            responseTypeAlias = new TsAliasModel(null, responseSymbol, Arrays.asList(varR), responseTypeDefinition, null);
        }
        tsModel.getTypeAliases().add(responseTypeAlias);
        return responseSymbol;
    }

    private TsModel createJaxrsInterfaces(SymbolTable symbolTable, TsModel tsModel, JaxrsApplicationModel jaxrsApplication,
            Symbol responseSymbol, TsType.GenericVariableType optionsGenericVariable, TsType optionsType) {
        final List<TsType.GenericVariableType> typeParameters = Utils.listFromNullable(optionsGenericVariable);
        final Map<Symbol, List<TsMethodModel>> groupedMethods = processJaxrsMethods(jaxrsApplication, symbolTable, null, responseSymbol, optionsType, false);
        for (Map.Entry<Symbol, List<TsMethodModel>> entry : groupedMethods.entrySet()) {
            final TsBeanModel interfaceModel = new TsBeanModel(null, TsBeanCategory.Service, false, entry.getKey(), typeParameters, null, null, null, null, null, entry.getValue(), null);
            tsModel.getBeans().add(interfaceModel);
        }
        return tsModel;
    }

    private TsModel createJaxrsClients(SymbolTable symbolTable, TsModel tsModel, JaxrsApplicationModel jaxrsApplication,
            Symbol responseSymbol, TsType.GenericVariableType optionsGenericVariable, TsType optionsType) {
        final Symbol httpClientSymbol = symbolTable.getSyntheticSymbol("HttpClient");
        final List<TsType.GenericVariableType> typeParameters = Utils.listFromNullable(optionsGenericVariable);

        // HttpClient interface
        tsModel.getBeans().add(new TsBeanModel(null, TsBeanCategory.ServicePrerequisite, false, httpClientSymbol, typeParameters, null, null, null, null, null, Arrays.asList(
                new TsMethodModel("request", new TsType.GenericReferenceType(responseSymbol, TsType.Any), Arrays.asList(
                        new TsParameterModel("requestConfig", new TsType.ObjectType(
                                new TsProperty("method", TsType.String),
                                new TsProperty("url", TsType.String),
                                new TsProperty("queryParams", new TsType.OptionalType(TsType.Any)),
                                new TsProperty("data", new TsType.OptionalType(TsType.Any)),
                                optionsType != null ? new TsProperty("options", new TsType.OptionalType(optionsType)) : null
                        ))
                ), null, null)
        ), null));

        // application client classes
        final TsType.ReferenceType httpClientType = optionsGenericVariable != null
                ? new TsType.GenericReferenceType(httpClientSymbol, optionsGenericVariable)
                : new TsType.ReferenceType(httpClientSymbol);
        final TsConstructorModel constructor = new TsConstructorModel(
                Arrays.asList(new TsParameterModel(TsAccessibilityModifier.Protected, "httpClient", httpClientType)),
                Collections.<TsStatement>emptyList(),
                null
        );
        final String groupingSuffix = settings.generateJaxrsApplicationInterface ? null : "Client";
        final Map<Symbol, List<TsMethodModel>> groupedMethods = processJaxrsMethods(jaxrsApplication, symbolTable, groupingSuffix, responseSymbol, optionsType, true);
        for (Map.Entry<Symbol, List<TsMethodModel>> entry : groupedMethods.entrySet()) {
            final Symbol symbol = settings.generateJaxrsApplicationInterface ? symbolTable.addSuffixToSymbol(entry.getKey(), "Client") : entry.getKey();
            final TsType interfaceType = settings.generateJaxrsApplicationInterface ? new TsType.ReferenceType(entry.getKey()) : null;
            final TsBeanModel clientModel = new TsBeanModel(null, TsBeanCategory.Service, true, symbol, typeParameters, null, null,
                    Utils.listFromNullable(interfaceType), null, constructor, entry.getValue(), null);
            tsModel.getBeans().add(clientModel);
        }
        // helper
        tsModel.getHelpers().add(TsHelper.loadFromResource("/helpers/uriEncoding.ts"));
        return tsModel;
    }

    private Map<Symbol, List<TsMethodModel>> processJaxrsMethods(JaxrsApplicationModel jaxrsApplication, SymbolTable symbolTable, String nameSuffix, Symbol responseSymbol, TsType optionsType, boolean implement) {
        final Map<Symbol, List<TsMethodModel>> result = new LinkedHashMap<>();
        final Map<Symbol, List<JaxrsMethodModel>> groupedMethods = groupingByMethodContainer(jaxrsApplication, symbolTable, nameSuffix);
        for (Map.Entry<Symbol, List<JaxrsMethodModel>> entry : groupedMethods.entrySet()) {
            result.put(entry.getKey(), processJaxrsMethodGroup(jaxrsApplication, entry.getValue(), symbolTable, responseSymbol, optionsType, implement));
        }
        return result;
    }

    private List<TsMethodModel> processJaxrsMethodGroup(JaxrsApplicationModel jaxrsApplication, List<JaxrsMethodModel> methods, SymbolTable symbolTable, Symbol responseSymbol, TsType optionsType, boolean implement) {
        final List<TsMethodModel> resultMethods = new ArrayList<>();
        final Map<String, Long> methodNamesCount = groupingByMethodName(methods);
        for (JaxrsMethodModel method : methods) {
            final boolean createLongName = methodNamesCount.get(method.getName()) > 1;
            resultMethods.add(processJaxrsMethod(symbolTable, jaxrsApplication.getApplicationPath(), responseSymbol, method, createLongName, optionsType, implement));
        }
        return resultMethods;
    }

    private Map<Symbol, List<JaxrsMethodModel>> groupingByMethodContainer(JaxrsApplicationModel jaxrsApplication, SymbolTable symbolTable, String nameSuffix) {
        // rewrite on Java 8 using streams
        final Map<Symbol, List<JaxrsMethodModel>> groupedMethods = new LinkedHashMap<>();
        for (JaxrsMethodModel method : jaxrsApplication.getMethods()) {
            final Symbol symbol = getContainerSymbol(jaxrsApplication, symbolTable, nameSuffix, method);
            if (!groupedMethods.containsKey(symbol)) {
                groupedMethods.put(symbol, new ArrayList<JaxrsMethodModel>());
            }
            groupedMethods.get(symbol).add(method);
        }
        return groupedMethods;
    }

    private Symbol getContainerSymbol(JaxrsApplicationModel jaxrsApplication, SymbolTable symbolTable, String nameSuffix, JaxrsMethodModel method) {
        if (settings.jaxrsNamespacing == JaxrsNamespacing.perResource) {
            return symbolTable.getSymbol(method.getRootResource(), nameSuffix);
        }
        if (settings.jaxrsNamespacing == JaxrsNamespacing.byAnnotation) {
            final Annotation annotation = method.getRootResource().getAnnotation(settings.jaxrsNamespacingAnnotation);
            final String element = settings.jaxrsNamespacingAnnotationElement != null ? settings.jaxrsNamespacingAnnotationElement : "value";
            final String annotationValue = Utils.getAnnotationElementValue(annotation, element, String.class);
            if (annotationValue != null) {
                if (Emitter.isValidIdentifierName(annotationValue)) {
                    return symbolTable.getSyntheticSymbol(annotationValue, nameSuffix);
                } else {
                    System.out.println(String.format("Warning: Ignoring annotation value '%s' since it is not a valid identifier, '%s' will be in default namespace", annotationValue, method.getOriginClass().getName() + "." + method.getName()));
                }
            }
        }
        final String applicationName = getApplicationName(jaxrsApplication);
        return symbolTable.getSyntheticSymbol(applicationName, nameSuffix);
    }

    private static String getApplicationName(JaxrsApplicationModel jaxrsApplication) {
        return jaxrsApplication.getApplicationName() != null ? jaxrsApplication.getApplicationName() : "RestApplication";
    }

    private static Map<String, Long> groupingByMethodName(List<JaxrsMethodModel> methods) {
        // Java 8
//        return methods.stream().collect(Collectors.groupingBy(JaxrsMethodModel::getName, Collectors.counting()));
        final Map<String, Long> methodNamesCount = new LinkedHashMap<>();
        for (JaxrsMethodModel method : methods) {
            final String name = method.getName();
            final long count = methodNamesCount.containsKey(name) ? methodNamesCount.get(name) : 0;
            methodNamesCount.put(name, count + 1);
        }
        return methodNamesCount;
    }

    private TsMethodModel processJaxrsMethod(SymbolTable symbolTable, String pathPrefix, Symbol responseSymbol, JaxrsMethodModel method, boolean createLongName, TsType optionsType, boolean implement) {
        final String path = Utils.joinPath(pathPrefix, method.getPath());
        final PathTemplate pathTemplate = PathTemplate.parse(path);
        final List<String> comments = Utils.concat(method.getComments(), Arrays.asList(
            "HTTP " + method.getHttpMethod() + " /" + path,
            "Java method: " + method.getOriginClass().getName() + "." + method.getName()
        ));
        final List<TsParameterModel> parameters = new ArrayList<>();
        // path params
        for (MethodParameterModel parameter : method.getPathParams()) {
            parameters.add(processParameter(symbolTable, method, parameter));
        }
        // entity param
        if (method.getEntityParam() != null) {
            parameters.add(processParameter(symbolTable, method, method.getEntityParam()));
        }
        // query params
        final List<MethodParameterModel> queryParams = method.getQueryParams();
        final TsParameterModel queryParameter;
        if (queryParams != null && !queryParams.isEmpty()) {
            final List<TsProperty> properties = new ArrayList<>();
            for (MethodParameterModel queryParam : queryParams) {
                final TsType type = typeFromJava(symbolTable, queryParam.getType(), method.getName(), method.getOriginClass());
                properties.add(new TsProperty(queryParam.getName(), new TsType.OptionalType(type)));
            }
            queryParameter = new TsParameterModel("queryParams", new TsType.OptionalType(new TsType.ObjectType(properties)));
            parameters.add(queryParameter);
        } else {
            queryParameter = null;
        }
        if (optionsType != null) {
            final TsParameterModel optionsParameter = new TsParameterModel("options", new TsType.OptionalType(optionsType));
            parameters.add(optionsParameter);
        }
        // return type
        final TsType returnType = typeFromJava(symbolTable, method.getReturnType(), method.getName(), method.getOriginClass());
        final TsType wrappedReturnType = new TsType.GenericReferenceType(responseSymbol, returnType);
        // method name
        final String nameSuffix;
        if (createLongName) {
            nameSuffix = "$" + method.getHttpMethod() + "$" + pathTemplate.format("", "", false)
                    .replaceAll("/", "_")
                    .replaceAll("\\W", "");
        } else {
            nameSuffix = "";
        }
        // implementation
        final List<TsStatement> body;
        if (implement) {
            body = new ArrayList<>();
            body.add(new TsReturnStatement(
                    new TsCallExpression(
                            new TsMemberExpression(new TsMemberExpression(new TsThisExpression(), "httpClient"), "request"),
                            new TsObjectLiteral(
                                    new TsPropertyDefinition("method", new TsStringLiteral(method.getHttpMethod())),
                                    new TsPropertyDefinition("url", processPathTemplate(pathTemplate)),
                                    queryParameter != null ? new TsPropertyDefinition("queryParams", new TsIdentifierReference("queryParams")) : null,
                                    method.getEntityParam() != null ? new TsPropertyDefinition("data", new TsIdentifierReference(method.getEntityParam().getName())) : null,
                                    optionsType != null ? new TsPropertyDefinition("options", new TsIdentifierReference("options")) : null
                            )
                    )
            ));
        } else {
            body = null;
        }
        // method
        final TsMethodModel tsMethodModel = new TsMethodModel(method.getName() + nameSuffix, wrappedReturnType, parameters, body, comments);
        return tsMethodModel;
    }

    private TsParameterModel processParameter(SymbolTable symbolTable, MethodModel method, MethodParameterModel parameter) {
        final TsType parameterType = typeFromJava(symbolTable, parameter.getType(), method.getName(), method.getOriginClass());
        return new TsParameterModel(parameter.getName(), parameterType);
    }

    private static TsTemplateLiteral processPathTemplate(PathTemplate pathTemplate) {
        final List<TsExpression> spans = new ArrayList<>();
        for (PathTemplate.Part part : pathTemplate.getParts()) {
            if (part instanceof PathTemplate.Literal) {
                final PathTemplate.Literal literal = (PathTemplate.Literal) part;
                spans.add(new TsStringLiteral(literal.getLiteral()));
            }
            if (part instanceof PathTemplate.Parameter) {
                final PathTemplate.Parameter parameter = (PathTemplate.Parameter) part;
                spans.add(new TsIdentifierReference(parameter.getName()));
            }
        }
        return new TsTaggedTemplateLiteral(new TsIdentifierReference("uriEncoding"), spans);
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
        return model.withTypeAliases(new ArrayList<>(typeAliases));
    }

    private TsModel transformEnumsToUnions(TsModel tsModel) {
        final List<TsEnumModel> stringEnums = tsModel.getEnums(EnumKind.StringBased);
        final LinkedHashSet<TsAliasModel> typeAliases = new LinkedHashSet<>(tsModel.getTypeAliases());
        for (TsEnumModel enumModel : stringEnums) {
            final List<TsType> values = new ArrayList<>();
            for (EnumMemberModel member : enumModel.getMembers()) {
                values.add(new TsType.StringLiteralType((String) member.getEnumValue()));
            }
            final TsType union = new TsType.UnionType(values);
            typeAliases.add(new TsAliasModel(enumModel.getOrigin(), enumModel.getName(), null, union, enumModel.getComments()));
        }
        return tsModel.withoutEnums(stringEnums).withTypeAliases(new ArrayList<>(typeAliases));
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
        return newTsModel.withoutTypeAliases(new ArrayList<>(inlinedAliases));
    }

    private TsModel transformEnumsToNumberBasedEnum(TsModel tsModel) {
        final List<TsEnumModel> stringEnums = tsModel.getEnums(EnumKind.StringBased);
        final LinkedHashSet<TsEnumModel> enums = new LinkedHashSet<>();
        for (TsEnumModel enumModel : stringEnums) {
            final List<EnumMemberModel> members = new ArrayList<>();
            for (EnumMemberModel member : enumModel.getMembers()) {
                members.add(new EnumMemberModel(member.getPropertyName(), (Number) null, member.getComments()));
            }
            enums.add(enumModel.withMembers(members));
        }
        return tsModel.withoutEnums(stringEnums).withEnums(new ArrayList<>(enums));
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
        return model.withTypeAliases(new ArrayList<>(typeAliases));
    }

    private TsModel sortDeclarations(SymbolTable symbolTable, TsModel tsModel) {
        final List<TsBeanModel> beans = tsModel.getBeans();
        final List<TsAliasModel> aliases = tsModel.getTypeAliases();
        final List<TsEnumModel> enums = tsModel.getEnums();
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
                    .withBeans(new ArrayList<>(orderedBeans))
                    .withTypeAliases(aliases)
                    .withEnums(enums);
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
                    newParameters.add(new TsParameterModel(parameter.getAccessibilityModifier(), parameter.getName(), newParameterType));
                }
                final TsType newReturnType = TsType.transformTsType(method.getReturnType(), transformer);
                newMethods.add(new TsMethodModel(method.getName(), newReturnType, newParameters, method.getBody(), method.getComments()));
            }
            newBeans.add(bean.withProperties(newProperties).withMethods(newMethods));
        }
        return tsModel.withBeans(newBeans);
    }

    private static Class<?> getOriginClass(SymbolTable symbolTable, TsType type) {
        if (type instanceof TsType.ReferenceType) {
            final TsType.ReferenceType referenceType = (TsType.ReferenceType) type;
            return symbolTable.getSymbolClass(referenceType.symbol);
        }
        return null;
    }

}
