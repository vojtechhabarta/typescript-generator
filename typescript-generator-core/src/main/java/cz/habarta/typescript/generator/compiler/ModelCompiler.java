
package cz.habarta.typescript.generator.compiler;

import cz.habarta.typescript.generator.DateMapping;
import cz.habarta.typescript.generator.EnumMapping;
import cz.habarta.typescript.generator.Extension;
import cz.habarta.typescript.generator.IdentifierCasing;
import cz.habarta.typescript.generator.MapMapping;
import cz.habarta.typescript.generator.NullabilityDefinition;
import cz.habarta.typescript.generator.OptionalPropertiesDeclaration;
import cz.habarta.typescript.generator.RestNamespacing;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TsParameter;
import cz.habarta.typescript.generator.TsProperty;
import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.TypeProcessor;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import cz.habarta.typescript.generator.emitter.EmitterExtension;
import cz.habarta.typescript.generator.emitter.TsAccessibilityModifier;
import cz.habarta.typescript.generator.emitter.TsAliasModel;
import cz.habarta.typescript.generator.emitter.TsAssignmentExpression;
import cz.habarta.typescript.generator.emitter.TsBeanCategory;
import cz.habarta.typescript.generator.emitter.TsBeanModel;
import cz.habarta.typescript.generator.emitter.TsCallExpression;
import cz.habarta.typescript.generator.emitter.TsConstructorModel;
import cz.habarta.typescript.generator.emitter.TsEnumModel;
import cz.habarta.typescript.generator.emitter.TsExpression;
import cz.habarta.typescript.generator.emitter.TsExpressionStatement;
import cz.habarta.typescript.generator.emitter.TsHelper;
import cz.habarta.typescript.generator.emitter.TsIdentifierReference;
import cz.habarta.typescript.generator.emitter.TsMemberExpression;
import cz.habarta.typescript.generator.emitter.TsMethodModel;
import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.emitter.TsModifierFlags;
import cz.habarta.typescript.generator.emitter.TsObjectLiteral;
import cz.habarta.typescript.generator.emitter.TsParameterModel;
import cz.habarta.typescript.generator.emitter.TsPropertyDefinition;
import cz.habarta.typescript.generator.emitter.TsPropertyModel;
import cz.habarta.typescript.generator.emitter.TsReturnStatement;
import cz.habarta.typescript.generator.emitter.TsStatement;
import cz.habarta.typescript.generator.emitter.TsStringLiteral;
import cz.habarta.typescript.generator.emitter.TsSuperExpression;
import cz.habarta.typescript.generator.emitter.TsTaggedTemplateLiteral;
import cz.habarta.typescript.generator.emitter.TsTemplateLiteral;
import cz.habarta.typescript.generator.emitter.TsThisExpression;
import cz.habarta.typescript.generator.parser.BeanModel;
import cz.habarta.typescript.generator.parser.EnumModel;
import cz.habarta.typescript.generator.parser.MethodModel;
import cz.habarta.typescript.generator.parser.MethodParameterModel;
import cz.habarta.typescript.generator.parser.Model;
import cz.habarta.typescript.generator.parser.PathTemplate;
import cz.habarta.typescript.generator.parser.PropertyAccess;
import cz.habarta.typescript.generator.parser.PropertyModel;
import cz.habarta.typescript.generator.parser.RestApplicationModel;
import cz.habarta.typescript.generator.parser.RestMethodModel;
import cz.habarta.typescript.generator.parser.RestQueryParam;
import cz.habarta.typescript.generator.type.JTypeWithNullability;
import cz.habarta.typescript.generator.util.DeprecationUtils;
import cz.habarta.typescript.generator.util.GenericsResolver;
import cz.habarta.typescript.generator.util.Pair;
import cz.habarta.typescript.generator.util.Utils;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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

    public enum TransformationPhase {
        BeforeTsModel,
        BeforeEnums,
        BeforeSymbolResolution,
        AfterDeclarationSorting,
    }

    public TsModel javaToTypeScript(Model model) {
        final SymbolTable symbolTable = new SymbolTable(settings);
        final List<Extension.TransformerDefinition> extensionTransformers = getExtensionTransformers();
        model = applyExtensionModelTransformers(symbolTable, model, extensionTransformers);
        TsModel tsModel = processModel(symbolTable, model);
        tsModel = addCustomTypeAliases(symbolTable, tsModel);
        tsModel = removeInheritedProperties(symbolTable, tsModel);
        tsModel = addImplementedProperties(symbolTable, tsModel);
        tsModel = sortPropertiesDeclarations(symbolTable, tsModel);
        if (settings.generateConstructors) {
            tsModel = addConstructors(symbolTable, tsModel);
        }

        // REST
        if (settings.isGenerateRest()) {
            final Symbol responseSymbol = createRestResponseType(symbolTable, tsModel);
            final TsType optionsType = settings.restOptionsType != null
                    ? new TsType.VerbatimType(settings.restOptionsType)
                    : null;
            final TsType.GenericVariableType optionsGenericVariable = settings.restOptionsTypeIsGeneric
                    ? new TsType.GenericVariableType(settings.restOptionsType)
                    : null;
            final List<RestApplicationModel> restApplicationsWithInterface = model.getRestApplications().stream()
                    .filter(restApplication -> restApplication.getType().generateInterface.apply(settings))
                    .collect(Collectors.toList());
            final List<RestApplicationModel> restApplicationsWithClient = model.getRestApplications().stream()
                    .filter(restApplication -> restApplication.getType().generateClient.apply(settings))
                    .collect(Collectors.toList());
            if (!restApplicationsWithInterface.isEmpty()) {
                createRestInterfaces(tsModel, symbolTable, restApplicationsWithInterface, responseSymbol, optionsGenericVariable, optionsType);
            }
            if (!restApplicationsWithClient.isEmpty()) {
                createRestClients(tsModel, symbolTable, restApplicationsWithClient, responseSymbol, optionsGenericVariable, optionsType);
            }
        }

        // maps
        tsModel = transformMaps(symbolTable, tsModel);

        // dates
        tsModel = transformDates(symbolTable, tsModel);

        // enums
        tsModel = applyExtensionTransformers(symbolTable, model, tsModel, TransformationPhase.BeforeEnums, extensionTransformers);
        tsModel = addEnumValuesToJavadoc(tsModel);
        if (settings.enumMemberCasing != null && settings.enumMemberCasing != IdentifierCasing.keepOriginal) {
            tsModel = transformEnumMembersCase(tsModel);
        }
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

        // after enum transformations transform Maps with rest of the enums (not unions) used in keys
        tsModel = transformNonStringEnumKeyMaps(symbolTable, tsModel);

        // tagged unions
        tsModel = createAndUseTaggedUnions(symbolTable, tsModel);

        // nullable types and optional properties
        tsModel = makeUndefinablePropertiesAndParametersOptional(symbolTable, tsModel);
        tsModel = transformNullableTypes(symbolTable, tsModel);
        tsModel = eliminateUndefinedFromOptionalPropertiesAndParameters(symbolTable, tsModel);
        tsModel = transformOptionalProperties(symbolTable, tsModel);

        tsModel = applyExtensionTransformers(symbolTable, model, tsModel, TransformationPhase.BeforeSymbolResolution, extensionTransformers);
        symbolTable.resolveSymbolNames();
        tsModel = sortTypeDeclarations(symbolTable, tsModel);
        tsModel = applyExtensionTransformers(symbolTable, model, tsModel, TransformationPhase.AfterDeclarationSorting, extensionTransformers);
        return tsModel;
    }

    private List<Extension.TransformerDefinition> getExtensionTransformers() {
        final List<Extension.TransformerDefinition> transformers = new ArrayList<>();
        for (EmitterExtension emitterExtension : settings.extensions) {
            if (emitterExtension instanceof Extension) {
                final Extension extension = (Extension) emitterExtension;
                transformers.addAll(extension.getTransformers());
            }
        }
        return transformers;
    }

    private static Model applyExtensionModelTransformers(SymbolTable symbolTable, Model model,
            List<Extension.TransformerDefinition> transformerDefinitions
    ) {
        for (Extension.TransformerDefinition definition : transformerDefinitions) {
            if (definition.phase == TransformationPhase.BeforeTsModel) {
                model = definition.transformer.transformModel(symbolTable, model);
            }
        }
        return model;
    }

    private static TsModel applyExtensionTransformers(SymbolTable symbolTable, Model model, TsModel tsModel,
            TransformationPhase phase, List<Extension.TransformerDefinition> transformerDefinitions
    ) {
        final TsModelTransformer.Context context = new TsModelTransformer.Context(symbolTable, model);
        for (Extension.TransformerDefinition definition : transformerDefinitions) {
            if (definition.phase == phase) {
                tsModel = definition.tsTransformer.transformModel(context, tsModel);
            }
        }
        return tsModel;
    }

    public TsType javaToTypeScript(Type type) {
        final BeanModel beanModel = new BeanModel(Object.class, Object.class, null, null, null, Collections.<Type>emptyList(),
                Collections.singletonList(new PropertyModel("property", type, false, null, null, null, null, null)), null);
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
                final Type processedAncestor = Utils.getRawClassOrNull(ancestor);
                if (!children.containsKey(processedAncestor)) {
                    children.put(processedAncestor, new ArrayList<>());
                }
                children.get(processedAncestor).add(bean);
            }
        }
        return children;
    }

    private <T> TsBeanModel processBean(SymbolTable symbolTable, Model model, Map<Type, List<BeanModel>> children, BeanModel bean) {
        final boolean isClass = mappedToClass(bean.getOrigin());
        final List<TsType> extendsList = new ArrayList<>();
        final List<TsType> implementsList = new ArrayList<>();

        final TsType parentTypeFromJava = typeFromJava(symbolTable, bean.getParent());
        final TsType parentType = parentTypeFromJava != null && !parentTypeFromJava.equals(TsType.Any)
                ? parentTypeFromJava
                : null;
        if (parentType != null) {
            final boolean isParentMappedToClass = mappedToClass(getOriginClass(symbolTable, parentType));
            if (isClass && !isParentMappedToClass) {
                implementsList.add(parentType);
            } else {
                extendsList.add(parentType);
            }
        }

        final List<TsType> interfaces = new ArrayList<>();
        for (Type aInterface : bean.getInterfaces()) {
            final TsType interfaceType = typeFromJava(symbolTable, aInterface);
            if (!interfaceType.equals(TsType.Any)) {
                interfaces.add(interfaceType);
            }
        }
        if (isClass) {
            implementsList.addAll(interfaces);
        } else {
            extendsList.addAll(interfaces);
        }

        final List<TsPropertyModel> properties = processProperties(symbolTable, model, bean);

        boolean isTaggedUnion = false;
        if (bean.getDiscriminantProperty() != null && bean.getProperty(bean.getDiscriminantProperty()) == null) {
            isTaggedUnion = true;
            boolean isDisciminantProperty = true;
            final List<BeanModel> selfAndDescendants = getSelfAndDescendants(bean, children);
            final List<TsType.StringLiteralType> literals = new ArrayList<>();
            for (BeanModel descendant : selfAndDescendants) {
                if (descendant.getDiscriminantProperty() == null || descendant.getProperty(bean.getDiscriminantProperty()) != null) {
                    // do not handle bean as tagged union if any descendant or it itself has duplicate discriminant property
                    isTaggedUnion = false;
                    isDisciminantProperty = false;
                }
                if (descendant.getDiscriminantLiteral() != null) {
                    literals.add(new TsType.StringLiteralType(descendant.getDiscriminantLiteral()));
                }
            }
            final List<BeanModel> descendants = selfAndDescendants.subList(1, selfAndDescendants.size());
            for (BeanModel descendant : descendants) {
                // do not handle bean as tagged union if any descendant has "non-related" generic parameter
                final List<String> mappedGenericVariables = GenericsResolver.mapGenericVariablesToBase(descendant.getOrigin(), bean.getOrigin());
                if (mappedGenericVariables.contains(null)) {
                    isTaggedUnion = false;
                }
            }
            final TsType discriminantType = isDisciminantProperty && !literals.isEmpty()
                    ? new TsType.UnionType(literals)
                    : TsType.String;
            final TsModifierFlags modifiers = TsModifierFlags.None.setReadonly(settings.declarePropertiesAsReadOnly);
            properties.add(0, new TsPropertyModel(bean.getDiscriminantProperty(), discriminantType, modifiers, /*ownProperty*/ true, null));
        }

        final TsBeanModel tsBean = new TsBeanModel(
                bean.getOrigin(),
                TsBeanCategory.Data,
                isClass,
                symbolTable.getSymbol(bean.getOrigin()),
                getTypeParameters(bean.getOrigin()),
                parentType,
                extendsList,
                implementsList,
                properties,
                /*constructor*/ null,
                /*methods*/ null,
                bean.getComments());
        return isTaggedUnion
                ? tsBean.withTaggedUnion(bean.getTaggedUnionClasses(), bean.getDiscriminantProperty(), bean.getDiscriminantLiteral())
                : tsBean;
    }

    private boolean mappedToClass(Class<?> cls) {
        return cls != null && !cls.isInterface() && settings.getMapClassesAsClassesFilter().test(cls.getName());
    }

    private static List<TsType.GenericVariableType> getTypeParameters(Class<?> cls) {
        final List<TsType.GenericVariableType> typeParameters = new ArrayList<>();
        for (TypeVariable<?> typeParameter : cls.getTypeParameters()) {
            typeParameters.add(new TsType.GenericVariableType(typeParameter.getName()));
        }
        return typeParameters;
    }

    private List<TsPropertyModel> processProperties(SymbolTable symbolTable, Model model, BeanModel bean) {
        return processProperties(symbolTable, model, bean, "", "");
    }

    private List<TsPropertyModel> processProperties(SymbolTable symbolTable, Model model, BeanModel bean, String prefix, String suffix) {
        final List<TsPropertyModel> properties = new ArrayList<>();
        for (PropertyModel property : bean.getProperties()) {
            boolean pulled = false;
            final PropertyModel.PullProperties pullProperties = property.getPullProperties();
            if (pullProperties != null) {
                final Type type = JTypeWithNullability.getPlainType(property.getType());
                if (type instanceof Class<?>) {
                    final BeanModel pullBean = model.getBean((Class<?>) type);
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

    private TsPropertyModel processProperty(SymbolTable symbolTable, BeanModel bean, PropertyModel property, String prefix, String suffix) {
        final TsType type = typeFromJava(symbolTable, property.getType(), property.getContext(), property.getName(), bean.getOrigin());
        final TsType tsType = property.isOptional() ? type.optional() : type;
        final TsModifierFlags modifiers = TsModifierFlags.None.setReadonly(settings.declarePropertiesAsReadOnly);
        final List<String> comments = settings.generateReadonlyAndWriteonlyJSDocTags
                ? Utils.concat(property.getComments(), getPropertyAccessComments(property.getAccess()))
                : property.getComments();
        return new TsPropertyModel(prefix + property.getName() + suffix, tsType, modifiers, /*ownProperty*/ false, comments);
    }

    private static List<String> getPropertyAccessComments(PropertyAccess access) {
        final String accessTag =
                access == PropertyAccess.ReadOnly ? "@readonly" :
                access == PropertyAccess.WriteOnly ? "@writeonly" :
                null;
        return accessTag != null ? Collections.singletonList(accessTag) : null;
    }

    private TsEnumModel processEnum(SymbolTable symbolTable, EnumModel enumModel) {
        final Symbol beanIdentifier = symbolTable.getSymbol(enumModel.getOrigin());
        TsEnumModel tsEnumModel = TsEnumModel.fromEnumModel(beanIdentifier, enumModel, isEnumNonConst(enumModel));
        return tsEnumModel;
    }

    private boolean isEnumNonConst(EnumModel enumModel) {
        boolean isNonConst = settings.nonConstEnums;
        if (!isNonConst) {
            for (Class<? extends Annotation> nonConstAnnotation : settings.nonConstEnumAnnotations) {
                if (enumModel.getOrigin().isAnnotationPresent(nonConstAnnotation)) {
                    isNonConst = true;
                    break;
                }
            }
        }
        return isNonConst;
    }

    private TsType typeFromJava(SymbolTable symbolTable, Type javaType) {
        return typeFromJava(symbolTable, javaType, null, null);
    }

    private TsType typeFromJava(SymbolTable symbolTable, Type javaType, String usedInProperty, Class<?> usedInClass) {
        return typeFromJava(symbolTable, javaType, null, usedInProperty, usedInClass);
    }

    private TsType typeFromJava(SymbolTable symbolTable, Type javaType, Object typeContext, String usedInProperty, Class<?> usedInClass) {
        if (javaType == null) {
            return null;
        }
        final TypeProcessor.Context context = new TypeProcessor.Context(symbolTable, typeProcessor, typeContext);
        final TypeProcessor.Result result = context.processType(javaType);
        if (result != null) {
            return result.getTsType();
        } else {
            if (usedInClass != null && usedInProperty != null) {
                TypeScriptGenerator.getLogger().warning(String.format("Unsupported type '%s' used in '%s.%s'", javaType, usedInClass.getSimpleName(), usedInProperty));
            } else {
                TypeScriptGenerator.getLogger().warning(String.format("Unsupported type '%s'", javaType));
            }
            return TsType.Any;
        }
    }

    private TsModel addCustomTypeAliases(SymbolTable symbolTable, TsModel tsModel) {
        final List<TsAliasModel> aliases = new ArrayList<>(tsModel.getTypeAliases());
        for (Settings.CustomTypeAlias customTypeAlias : settings.getValidatedCustomTypeAliases()) {
            final Symbol name = symbolTable.getSyntheticSymbol(customTypeAlias.tsType.rawName);
            final List<TsType.GenericVariableType> typeParameters = customTypeAlias.tsType.typeParameters != null
                    ? customTypeAlias.tsType.typeParameters.stream()
                            .map(TsType.GenericVariableType::new)
                            .collect(Collectors.toList())
                    : null;
            final TsType definition = new TsType.VerbatimType(customTypeAlias.tsDefinition);
            aliases.add(new TsAliasModel(null, name, typeParameters, definition, null));
        }
        return tsModel.withTypeAliases(aliases);
    }

    private TsModel removeInheritedProperties(SymbolTable symbolTable, TsModel tsModel) {
        final List<TsBeanModel> beans = new ArrayList<>();
        for (TsBeanModel bean : tsModel.getBeans()) {
            final Map<String, TsType> inheritedPropertyTypes = getInheritedProperties(symbolTable, tsModel, bean.getAllParents());
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
                classPropertyNames.addAll(getInheritedProperties(symbolTable, tsModel, bean.getExtendsList()).keySet());

                final List<TsPropertyModel> implementedProperties = getImplementedProperties(symbolTable, tsModel, bean.getImplementsList());
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

    private TsModel addConstructors(SymbolTable symbolTable, TsModel tsModel) {
        final List<TsBeanModel> beans = new ArrayList<>();
        for (TsBeanModel bean : tsModel.getBeans()) {
            final Symbol beanIdentifier = symbolTable.getSymbol(bean.getOrigin());
            final List<TsType.GenericVariableType> typeParameters = getTypeParameters(bean.getOrigin());
            final TsType.ReferenceType dataType = typeParameters.isEmpty()
                    ? new TsType.ReferenceType(beanIdentifier)
                    : new TsType.GenericReferenceType(beanIdentifier, typeParameters);
            final List<TsStatement> body = new ArrayList<>();
            if (bean.getParent() != null) {
                body.add(new TsExpressionStatement(
                        new TsCallExpression(
                                new TsSuperExpression(),
                                new TsIdentifierReference("data")
                        )
                ));
            }
            for (TsPropertyModel property : bean.getProperties()) {
                final Map<String, TsType> inheritedProperties = ModelCompiler.getInheritedProperties(symbolTable, tsModel, Utils.listFromNullable(bean.getParent()));
                if (!inheritedProperties.containsKey(property.getName())) {
                    body.add(new TsExpressionStatement(new TsAssignmentExpression(
                            new TsMemberExpression(new TsThisExpression(), property.name),
                            new TsMemberExpression(new TsIdentifierReference("data"), property.name)
                    )));
                }
            }
            if (bean.isClass()) {
                final TsConstructorModel constructor = new TsConstructorModel(
                        TsModifierFlags.None,
                        Arrays.asList(new TsParameterModel("data", dataType)),
                        body,
                        /*comments*/ null
                );
                beans.add(bean.withConstructor(constructor));
            } else {
                beans.add(bean);
            }
        }
        return tsModel.withBeans(beans);
    }

    public static Map<String, TsType> getInheritedProperties(SymbolTable symbolTable, TsModel tsModel, List<TsType> parents) {
        final Map<String, TsType> properties = new LinkedHashMap<>();
        for (TsType parentType : parents) {
            final TsBeanModel parent = tsModel.getBean(getOriginClass(symbolTable, parentType));
            if (parent != null) {
                properties.putAll(getInheritedProperties(symbolTable, tsModel, parent.getAllParents()));
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
                properties.addAll(getImplementedProperties(symbolTable, tsModel, bean.getExtendsList()));
                properties.addAll(bean.getProperties());
            }
        }
        return properties;
    }

    private Symbol createRestResponseType(SymbolTable symbolTable, TsModel tsModel) {
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

    private void createRestInterfaces(TsModel tsModel, SymbolTable symbolTable, List<RestApplicationModel> restApplications,
            Symbol responseSymbol, TsType.GenericVariableType optionsGenericVariable, TsType optionsType) {
        final List<TsType.GenericVariableType> typeParameters = Utils.listFromNullable(optionsGenericVariable);
        final Map<Symbol, List<TsMethodModel>> groupedMethods = processRestMethods(tsModel, restApplications, symbolTable, null, responseSymbol, optionsType, false);
        for (Map.Entry<Symbol, List<TsMethodModel>> entry : groupedMethods.entrySet()) {
            final TsBeanModel interfaceModel = new TsBeanModel(null, TsBeanCategory.Service, false, entry.getKey(), typeParameters, null, null, null, null, null, entry.getValue(), null);
            tsModel.getBeans().add(interfaceModel);
        }
    }

    private void createRestClients(TsModel tsModel, SymbolTable symbolTable, List<RestApplicationModel> restApplications,
            Symbol responseSymbol, TsType.GenericVariableType optionsGenericVariable, TsType optionsType) {
        final Symbol httpClientSymbol = symbolTable.getSyntheticSymbol("HttpClient");
        final List<TsType.GenericVariableType> typeParameters = Utils.listFromNullable(optionsGenericVariable);

        // HttpClient interface
        final TsType.GenericVariableType returnGenericVariable = new TsType.GenericVariableType("R");
        tsModel.getBeans().add(new TsBeanModel(null, TsBeanCategory.ServicePrerequisite, false, httpClientSymbol, typeParameters, null, null, null, null, null, Arrays.asList(
                new TsMethodModel("request", TsModifierFlags.None, Arrays.asList(returnGenericVariable), Arrays.asList(
                        new TsParameterModel("requestConfig", new TsType.ObjectType(
                                new TsProperty("method", TsType.String),
                                new TsProperty("url", TsType.String),
                                new TsProperty("queryParams", new TsType.OptionalType(TsType.Any)),
                                new TsProperty("data", new TsType.OptionalType(TsType.Any)),
                                new TsProperty("copyFn", new TsType.OptionalType(new TsType.FunctionType(Arrays.asList(new TsParameter("data", returnGenericVariable)), returnGenericVariable))),
                                optionsType != null ? new TsProperty("options", new TsType.OptionalType(optionsType)) : null
                        ))
                ), new TsType.GenericReferenceType(responseSymbol, returnGenericVariable), null, null)
        ), null));

        // application client classes
        final TsType.ReferenceType httpClientType = optionsGenericVariable != null
                ? new TsType.GenericReferenceType(httpClientSymbol, optionsGenericVariable)
                : new TsType.ReferenceType(httpClientSymbol);
        final TsConstructorModel constructor = new TsConstructorModel(
                TsModifierFlags.None,
                Arrays.asList(new TsParameterModel(TsAccessibilityModifier.Protected, "httpClient", httpClientType)),
                Collections.<TsStatement>emptyList(),
                null
        );
        final boolean bothInterfacesAndClients = settings.generateJaxrsApplicationInterface || settings.generateSpringApplicationInterface;
        final String groupingSuffix = bothInterfacesAndClients ? null : "Client";
        final Map<Symbol, List<TsMethodModel>> groupedMethods = processRestMethods(tsModel, restApplications, symbolTable, groupingSuffix, responseSymbol, optionsType, true);
        for (Map.Entry<Symbol, List<TsMethodModel>> entry : groupedMethods.entrySet()) {
            final Symbol symbol = bothInterfacesAndClients ? symbolTable.addSuffixToSymbol(entry.getKey(), "Client") : entry.getKey();
            final TsType interfaceType = bothInterfacesAndClients ? new TsType.ReferenceType(entry.getKey()) : null;
            final TsBeanModel clientModel = new TsBeanModel(null, TsBeanCategory.Service, true, symbol, typeParameters, null, null,
                    Utils.listFromNullable(interfaceType), null, constructor, entry.getValue(), null);
            tsModel.getBeans().add(clientModel);
        }
        // helper
        tsModel.getHelpers().add(TsHelper.loadFromResource("/helpers/uriEncoding.ts"));
    }

    private Map<Symbol, List<TsMethodModel>> processRestMethods(TsModel tsModel, List<RestApplicationModel> restApplications, SymbolTable symbolTable, String nameSuffix, Symbol responseSymbol, TsType optionsType, boolean implement) {
        final Map<Symbol, List<TsMethodModel>> result = new LinkedHashMap<>();
        final Map<Symbol, List<Pair<RestApplicationModel, RestMethodModel>>> groupedMethods = groupingByMethodContainer(restApplications, symbolTable, nameSuffix);
        for (Map.Entry<Symbol, List<Pair<RestApplicationModel, RestMethodModel>>> entry : groupedMethods.entrySet()) {
            result.put(entry.getKey(), processRestMethodGroup(tsModel, symbolTable, entry.getValue(), responseSymbol, optionsType, implement));
        }
        return result;
    }

    private List<TsMethodModel> processRestMethodGroup(TsModel tsModel, SymbolTable symbolTable, List<Pair<RestApplicationModel, RestMethodModel>> methods, Symbol responseSymbol, TsType optionsType, boolean implement) {
        final List<TsMethodModel> resultMethods = new ArrayList<>();
        final Map<String, Long> methodNamesCount = groupingByMethodName(methods);
        for (Pair<RestApplicationModel, RestMethodModel> pair : methods) {
            final RestApplicationModel restApplication = pair.getValue1();
            final RestMethodModel method = pair.getValue2();
            final boolean createLongName = methodNamesCount.get(method.getName()) > 1;
            resultMethods.add(processRestMethod(tsModel, symbolTable, restApplication.getApplicationPath(), responseSymbol, method, createLongName, optionsType, implement));
        }
        return resultMethods;
    }

    private Map<Symbol, List<Pair<RestApplicationModel, RestMethodModel>>> groupingByMethodContainer(List<RestApplicationModel> restApplications, SymbolTable symbolTable, String nameSuffix) {
        return restApplications.stream()
                .flatMap(restApplication -> restApplication.getMethods().stream().map(method -> Pair.of(restApplication, method)))
                .collect(Collectors.groupingBy(
                        pair -> getContainerSymbol(pair.getValue1(), symbolTable, nameSuffix, pair.getValue2()),
                        Utils.toSortedList(Comparator.comparing(pair -> pair.getValue2().getPath()))
                ));
    }

    private Symbol getContainerSymbol(RestApplicationModel restApplication, SymbolTable symbolTable, String nameSuffix, RestMethodModel method) {
        if (settings.restNamespacing == RestNamespacing.perResource) {
            return symbolTable.getSymbol(method.getRootResource(), nameSuffix);
        }
        if (settings.restNamespacing == RestNamespacing.byAnnotation) {
            final Annotation annotation = method.getRootResource().getAnnotation(settings.restNamespacingAnnotation);
            final String element = settings.restNamespacingAnnotationElement != null ? settings.restNamespacingAnnotationElement : "value";
            final String annotationValue = Utils.getAnnotationElementValue(annotation, element, String.class);
            if (annotationValue != null) {
                if (isValidIdentifierName(annotationValue)) {
                    return symbolTable.getSyntheticSymbol(annotationValue, nameSuffix);
                } else {
                    TypeScriptGenerator.getLogger().warning(String.format("Ignoring annotation value '%s' since it is not a valid identifier, '%s' will be in default namespace", annotationValue, method.getOriginClass().getName() + "." + method.getName()));
                }
            }
        }
        final String applicationName = getApplicationName(restApplication);
        return symbolTable.getSyntheticSymbol(applicationName, nameSuffix);
    }

    private static String getApplicationName(RestApplicationModel restApplication) {
        return restApplication.getApplicationName() != null ? restApplication.getApplicationName() : "RestApplication";
    }

    private static Map<String, Long> groupingByMethodName(List<Pair<RestApplicationModel, RestMethodModel>> methods) {
        return methods.stream()
                .map(pair -> pair.getValue2())
                .collect(Collectors.groupingBy(RestMethodModel::getName, Collectors.counting()));
    }

    private TsMethodModel processRestMethod(TsModel tsModel, SymbolTable symbolTable, String pathPrefix, Symbol responseSymbol, RestMethodModel method, boolean createLongName, TsType optionsType, boolean implement) {
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
        final List<RestQueryParam> queryParams = method.getQueryParams();
        final TsParameterModel queryParameter;
        if (queryParams != null && !queryParams.isEmpty()) {
            final List<TsType> types = new ArrayList<>();
            if (queryParams.stream().anyMatch(param -> param instanceof RestQueryParam.Map)) {
                types.add(new TsType.IndexedArrayType(TsType.String, TsType.Any));
            } else {
                final List<TsProperty> currentSingles = new ArrayList<>();
                final Runnable flushSingles = () -> {
                    if (!currentSingles.isEmpty()) {
                        types.add(new TsType.ObjectType(currentSingles));
                        currentSingles.clear();
                    }
                };
                for (RestQueryParam restQueryParam : queryParams) {
                    if (restQueryParam instanceof RestQueryParam.Single) {
                        final MethodParameterModel queryParam = ((RestQueryParam.Single) restQueryParam).getQueryParam();
                        final TsType type = typeFromJava(symbolTable, queryParam.getType(), method.getName(), method.getOriginClass());
                        currentSingles.add(new TsProperty(queryParam.getName(), restQueryParam.required ? type : new TsType.OptionalType(type)));
                    }
                    if (restQueryParam instanceof RestQueryParam.Bean) {
                        final BeanModel queryBean = ((RestQueryParam.Bean) restQueryParam).getBean();
                        flushSingles.run();
                        final Symbol queryParamsSymbol = symbolTable.getSymbol(queryBean.getOrigin(), "QueryParams");
                        if (tsModel.getBean(queryParamsSymbol) == null) {
                            tsModel.getBeans().add(new TsBeanModel(
                                    queryBean.getOrigin(),
                                    TsBeanCategory.Data,
                                    /*isClass*/false,
                                    queryParamsSymbol,
                                    /*typeParameters*/null,
                                    /*parent*/null,
                                    /*extendsList*/null,
                                    /*implementsList*/null,
                                    processProperties(symbolTable, null, queryBean),
                                    /*constructor*/null,
                                    /*methods*/null,
                                    /*comments*/null
                            ));
                        }
                        types.add(new TsType.ReferenceType(queryParamsSymbol));
                    }
                }
                flushSingles.run();
            }
            boolean allQueryParamsOptional = queryParams.stream().noneMatch(queryParam -> queryParam.required);
            TsType.IntersectionType queryParamType = new TsType.IntersectionType(types);
            queryParameter = new TsParameterModel("queryParams", allQueryParamsOptional ? new TsType.OptionalType(queryParamType) : queryParamType);
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
        final TsMethodModel tsMethodModel = new TsMethodModel(method.getName() + nameSuffix, TsModifierFlags.None, null, parameters, wrappedReturnType, body, comments);
        return tsMethodModel;
    }

    private TsParameterModel processParameter(SymbolTable symbolTable, MethodModel method, MethodParameterModel parameter) {
        final String parameterName = parameter.getName();
        final TsType parameterType = typeFromJava(symbolTable, parameter.getType(), method.getName(), method.getOriginClass());
        return new TsParameterModel(parameterName, parameterType);
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
                spans.add(new TsIdentifierReference(parameter.getValidName()));
            }
        }
        return new TsTaggedTemplateLiteral(new TsIdentifierReference("uriEncoding"), spans);
    }

    private TsModel transformMaps(SymbolTable symbolTable, TsModel tsModel) {
        if (settings.mapMap != MapMapping.asRecord) {
            return tsModel;
        }
        final TsModel model = transformBeanPropertyTypes(tsModel, new TsType.Transformer() {
            @Override
            public TsType transform(TsType.Context context, TsType type) {
                if (type instanceof TsType.IndexedArrayType) {
                    final TsType.IndexedArrayType indexedArrayType = (TsType.IndexedArrayType) type;
                    return new TsType.GenericBasicType("Record", indexedArrayType.indexType, indexedArrayType.elementType);
                }
                return type;
            }
        });
        return model;
    }

    private TsModel transformDates(SymbolTable symbolTable, TsModel tsModel) {
        final TsAliasModel dateAsNumber = new TsAliasModel(null, symbolTable.getSyntheticSymbol("DateAsNumber"), null, TsType.Number, null);
        final TsAliasModel dateAsString = new TsAliasModel(null, symbolTable.getSyntheticSymbol("DateAsString"), null, TsType.String, null);
        final LinkedHashSet<TsAliasModel> typeAliases = new LinkedHashSet<>(tsModel.getTypeAliases());
        final TsModel model = transformBeanPropertyTypes(tsModel, new TsType.Transformer() {
            @Override
            public TsType transform(TsType.Context context, TsType type) {
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

    static List<String> splitIdentifierIntoWords(String identifier) {
        final String pattern = String.join("|",
                "_",  // example: UPPER CASE
                "(?<=\\p{javaUpperCase})" + "(?=\\p{javaUpperCase}\\p{javaLowerCase})",  // example: XML Http
                "(?<=[^_\\p{javaUpperCase}])" + "(?=\\p{javaUpperCase})",  // example: camel Case
                "(?<=[\\p{javaUpperCase}\\p{javaLowerCase}])" + "(?=[^\\p{javaUpperCase}\\p{javaLowerCase}])",  // example: string 2
                "(?<=[^_\\p{javaUpperCase}\\p{javaLowerCase}])" + "(?=[\\p{javaUpperCase}\\p{javaLowerCase}])"  // example: 2 json
        );
        return Arrays.asList(identifier.split(pattern));
    }

    private String convertIdentifierCasing(String identifier) {
        final List<String> words = splitIdentifierIntoWords(identifier);
        final String pascalCase = words.stream()
                .map(word -> word.substring(0, 1).toUpperCase() + word.substring(1).toLowerCase())
                .collect(Collectors.joining());
        if (settings.enumMemberCasing == IdentifierCasing.PascalCase) {
            return pascalCase;
        }
        if (settings.enumMemberCasing == IdentifierCasing.camelCase) {
            return pascalCase.substring(0, 1).toLowerCase() + pascalCase.substring(1);
        }
        return identifier;
    }

    private TsModel transformEnumMembersCase(TsModel tsModel) {
        final List<TsEnumModel> originalEnums = tsModel.getEnums();
        final LinkedHashSet<TsEnumModel> enums = new LinkedHashSet<>();
        for (TsEnumModel enumModel : originalEnums) {
            final List<EnumMemberModel> members = new ArrayList<>();
            for (EnumMemberModel member : enumModel.getMembers()) {
                members.add(member.withPropertyName(convertIdentifierCasing(member.getPropertyName())));
            }
            enums.add(enumModel.withMembers(members));
        }
        return tsModel.withRemovedEnums(originalEnums).withAddedEnums(new ArrayList<>(enums));
    }

    private TsModel transformEnumsToUnions(TsModel tsModel) {
        final List<TsEnumModel> stringEnums = tsModel.getEnums(EnumKind.StringBased);
        final LinkedHashSet<TsAliasModel> typeAliases = new LinkedHashSet<>(tsModel.getTypeAliases());
        for (TsEnumModel enumModel : stringEnums) {
            final List<TsType> values = new ArrayList<>();
            for (EnumMemberModel member : enumModel.getMembers()) {
                values.add(member.getEnumValue() instanceof Number
                        ? new TsType.NumberLiteralType((Number) member.getEnumValue())
                        : new TsType.StringLiteralType(String.valueOf(member.getEnumValue()))
                );
            }
            final TsType union = new TsType.UnionType(values);
            typeAliases.add(new TsAliasModel(enumModel.getOrigin(), enumModel.getName(), null, union, enumModel.getComments()));
        }
        return tsModel.withRemovedEnums(stringEnums).withTypeAliases(new ArrayList<>(typeAliases));
    }

    private TsModel inlineEnums(final TsModel tsModel, final SymbolTable symbolTable) {
        final Set<TsAliasModel> inlinedAliases = new LinkedHashSet<>();
        final TsModel newTsModel = transformBeanPropertyTypes(tsModel, new TsType.Transformer() {
            @Override
            public TsType transform(TsType.Context context, TsType tsType) {
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
        return newTsModel.withRemovedTypeAliases(new ArrayList<>(inlinedAliases));
    }

    private TsModel transformEnumsToNumberBasedEnum(TsModel tsModel) {
        final List<TsEnumModel> stringEnums = tsModel.getEnums(EnumKind.StringBased);
        final LinkedHashSet<TsEnumModel> enums = new LinkedHashSet<>();
        for (TsEnumModel enumModel : stringEnums) {
            final List<EnumMemberModel> members = new ArrayList<>();
            for (EnumMemberModel member : enumModel.getMembers()) {
                members.add(new EnumMemberModel(member.getPropertyName(), (Number) null, member.getOriginalField(), member.getComments()));
            }
            enums.add(enumModel.withMembers(members));
        }
        return tsModel.withRemovedEnums(stringEnums).withAddedEnums(new ArrayList<>(enums));
    }

    private TsModel transformNonStringEnumKeyMaps(SymbolTable symbolTable, TsModel tsModel) {
        return transformBeanPropertyTypes(tsModel, new TsType.Transformer() {
            @Override
            public TsType transform(TsType.Context context, TsType tsType) {
                if (tsType instanceof TsType.MappedType) {
                    final TsType.MappedType mappedType = (TsType.MappedType) tsType;
                    if (mappedType.parameterType instanceof TsType.EnumReferenceType) {
                        final TsType.EnumReferenceType enumType = (TsType.EnumReferenceType) mappedType.parameterType;
                        final Class<?> enumClass = symbolTable.getSymbolClass(enumType.symbol);
                        final TsEnumModel enumModel = tsModel.getEnums().stream()
                                .filter(model -> Objects.equals(model.getOrigin(), enumClass))
                                .findFirst()
                                .orElse(null);
                        if (settings.mapEnum == EnumMapping.asNumberBasedEnum
                                || enumModel != null && enumModel.getKind() == EnumKind.NumberBased
                                || enumModel != null && enumModel.getMembers().stream().anyMatch(member -> !(member.getEnumValue() instanceof String))) {
                            return new TsType.IndexedArrayType(TsType.String, mappedType.type);
                        }
                    }
                }
                return tsType;
            }
        });
    }

    private static TsModel addEnumValuesToJavadoc(TsModel tsModel) {
        return tsModel.withEnums(tsModel.getEnums().stream()
                .map(enumModel -> addEnumValuesToJavadoc(enumModel))
                .collect(Collectors.toList())
        );
    }

    private static TsEnumModel addEnumValuesToJavadoc(TsEnumModel enumModel) {
        final boolean hasComments = enumModel.getComments() != null && !enumModel.getComments().isEmpty();
        final boolean hasMemberComments = enumModel.getMembers().stream()
                .anyMatch(enumMember -> enumMember.getComments() != null && !enumMember.getComments().isEmpty());
        if (hasComments || hasMemberComments) {
            return enumModel.withComments(Stream
                    .of(
                            Utils.listFromNullable(enumModel.getComments()).stream(),
                            (hasComments ? Stream.of("") : Stream.<String>empty()),
                            Stream.of("Values:"),
                            enumModel.getMembers().stream()
                                    .map(enumMember -> "- `" + enumMember.getEnumValue() + "`"
                                            + getEnumItemCommentAsString(enumMember))
                    )
                    .flatMap(Function.identity())
                    .collect(Collectors.toList())
            );
        } else {
            return enumModel;
        }
    }

    private static String getEnumItemCommentAsString(EnumMemberModel enumMember) {
        if (enumMember.getComments() == null) {
            return "";
        }
        return " - " + enumMember.getComments().stream()
                .map(s -> s.startsWith(DeprecationUtils.DEPRECATED) ? s.substring(1) : s)
                .collect(Collectors.joining(" "));
    }

    private TsModel createAndUseTaggedUnions(final SymbolTable symbolTable, TsModel tsModel) {
        if (settings.disableTaggedUnions) {
            return tsModel;
        }
        // create tagged unions
        final List<TsBeanModel> beans = new ArrayList<>();
        final LinkedHashSet<TsAliasModel> typeAliases = new LinkedHashSet<>(tsModel.getTypeAliases());
        for (TsBeanModel bean : tsModel.getBeans()) {
            if (!bean.getTaggedUnionClasses().isEmpty() && bean.getDiscriminantProperty() != null) {
                final Symbol unionName = symbolTable.getSymbol(bean.getOrigin(), "Union");
                final boolean isGeneric = !bean.getTypeParameters().isEmpty();
                final List<TsType> unionTypes = new ArrayList<>();
                for (Class<?> cls : bean.getTaggedUnionClasses()) {
                    final TsType type;
                    if (isGeneric && cls.getTypeParameters().length != 0) {
                        final List<String> mappedGenericVariables = GenericsResolver.mapGenericVariablesToBase(cls, bean.getOrigin());
                        type = new TsType.GenericReferenceType(
                                symbolTable.getSymbol(cls),
                                mappedGenericVariables.stream()
                                        .map(TsType.GenericVariableType::new)
                                        .collect(Collectors.toList()));
                    } else {
                        type = new TsType.ReferenceType(symbolTable.getSymbol(cls));
                    }
                    unionTypes.add(type);
                }
                final TsType.UnionType union = new TsType.UnionType(unionTypes);
                final TsAliasModel tsAliasModel = new TsAliasModel(bean.getOrigin(), unionName, bean.getTypeParameters(), union, null);
                beans.add(bean.withTaggedUnionAlias(tsAliasModel));
                typeAliases.add(tsAliasModel);
            } else {
                beans.add(bean);
            }
        }
        final TsModel modelWithTaggedUnions = tsModel.withBeans(beans).withTypeAliases(new ArrayList<>(typeAliases));
        // use tagged unions
        final TsModel modelWithUsedTaggedUnions = transformBeanPropertyTypes(modelWithTaggedUnions, new TsType.Transformer() {
            @Override
            public TsType transform(TsType.Context context, TsType tsType) {
                final Class<?> cls = getOriginClass(symbolTable, tsType);
                if (cls != null) {
                    final Symbol unionSymbol = symbolTable.hasSymbol(cls, "Union");
                    if (unionSymbol != null) {
                        if (tsType instanceof TsType.GenericReferenceType) {
                            final TsType.GenericReferenceType genericReferenceType = (TsType.GenericReferenceType) tsType;
                            return new TsType.GenericReferenceType(unionSymbol, genericReferenceType.typeArguments);
                        } else {
                            return new TsType.ReferenceType(unionSymbol);
                        }
                    }
                }
                return tsType;
            }
        });
        return modelWithUsedTaggedUnions;
    }

    // example: transforms property `text: string | undefined` to `text?: string | undefined`
    private TsModel makeUndefinablePropertiesAndParametersOptional(final SymbolTable symbolTable, TsModel tsModel) {
        final NullabilityDefinition nullabilityDefinition = settings.getNullabilityDefinition();
        if (!nullabilityDefinition.containsUndefined()) {
            return tsModel;
        }
        return tsModel.withBeans(tsModel.getBeans().stream()
                .map(bean -> {
                    bean = bean.withProperties(bean.getProperties().stream()
                            .map(property -> property.withTsType(makeNullableTypeOptional(property.getTsType())))
                            .collect(Collectors.toList())
                    );
                    bean = bean.withMethods(bean.getMethods().stream()
                            .map(method -> method.withParameters(method.getParameters().stream()
                                    .map(parameter -> parameter.withTsType(makeNullableTypeOptional(parameter.getTsType())))
                                    .collect(Collectors.toList())
                            ))
                            .collect(Collectors.toList())
                    );
                    return bean;
                })
                .collect(Collectors.toList())
        );
    }

    private static TsType makeNullableTypeOptional(TsType type) {
        return type instanceof TsType.NullableType
                ? new TsType.OptionalType(type)
                : type;
    }

    private TsModel transformNullableTypes(final SymbolTable symbolTable, TsModel tsModel) {
        final AtomicBoolean declareNullableType = new AtomicBoolean(false);
        final NullabilityDefinition nullabilityDefinition = settings.getNullabilityDefinition();
        TsModel transformedModel = transformBeanPropertyTypes(tsModel, new TsType.Transformer() {
            @Override
            public TsType transform(TsType.Context context, TsType tsType) {
                if (tsType instanceof TsType.NullableType) {
                    final TsType.NullableType nullableType = (TsType.NullableType) tsType;
                    if (nullabilityDefinition.isInline()) {
                        return new TsType.UnionType(nullableType.type).add(nullabilityDefinition.getTypes());
                    } else {
                        declareNullableType.set(true);
                    }
                }
                return tsType;
            }
        });
        // type Nullable<T> = T | ...
        if (declareNullableType.get()) {
            final TsType.GenericVariableType tVar = new TsType.GenericVariableType("T");
            transformedModel = transformedModel.withAddedTypeAliases(Arrays.asList(new TsAliasModel(
                    /*origin*/ null,
                    symbolTable.getSyntheticSymbol(TsType.NullableType.AliasName),
                    Arrays.asList(tVar),
                    new TsType.UnionType(tVar).add(nullabilityDefinition.getTypes()),
                    /*comments*/ null
            )));
        }
        return transformedModel;
    }

    // example: transforms property `text?: string | null | undefined` to `text?: string | null`
    private TsModel eliminateUndefinedFromOptionalPropertiesAndParameters(final SymbolTable symbolTable, TsModel tsModel) {
        return tsModel.withBeans(tsModel.getBeans().stream()
                .map(bean -> {
                    bean = bean.withProperties(bean.getProperties().stream()
                            .map(property -> property.withTsType(eliminateUndefinedFromOptionalType(property.getTsType())))
                            .collect(Collectors.toList())
                    );
                    bean = bean.withMethods(bean.getMethods().stream()
                            .map(method -> method.withParameters(method.getParameters().stream()
                                    .map(parameter -> parameter.withTsType(eliminateUndefinedFromOptionalType(parameter.getTsType())))
                                    .collect(Collectors.toList())
                            ))
                            .collect(Collectors.toList())
                    );
                    return bean;
                })
                .collect(Collectors.toList())
        );
    }

    private static TsType eliminateUndefinedFromOptionalType(TsType type) {
        if (type instanceof TsType.OptionalType) {
            final TsType.OptionalType optionalType = (TsType.OptionalType) type;
            if (optionalType.type instanceof TsType.UnionType) {
                final TsType.UnionType unionType = (TsType.UnionType) optionalType.type;
                if (unionType.types.contains(TsType.Undefined)) {
                    return new TsType.OptionalType(unionType.remove(Arrays.asList(TsType.Undefined)));
                }
            }
        }
        return type;
    }

    private TsModel transformOptionalProperties(final SymbolTable symbolTable, TsModel tsModel) {
        return tsModel.withBeans(tsModel.getBeans().stream()
                .map(bean -> {
                    if (bean.getCategory() != TsBeanCategory.Data) {
                        return bean;
                    }
                    return bean.withProperties(bean.getProperties().stream()
                            .map(property -> {
                                if (property.getTsType() instanceof TsType.OptionalType) {
                                    final TsType.OptionalType optionalType = (TsType.OptionalType) property.getTsType();
                                    if (settings.optionalPropertiesDeclaration == OptionalPropertiesDeclaration.nullableType) {
                                        return property.withTsType(
                                                TsType.UnionType.combine(Arrays.asList(optionalType.type, TsType.Null)));
                                    }
                                    if (settings.optionalPropertiesDeclaration == OptionalPropertiesDeclaration.questionMarkAndNullableType) {
                                        return property.withTsType(
                                                new TsType.OptionalType(
                                                        TsType.UnionType.combine(Arrays.asList(optionalType.type, TsType.Null))));
                                    }
                                    if (settings.optionalPropertiesDeclaration == OptionalPropertiesDeclaration.nullableAndUndefinableType) {
                                        return property.withTsType(
                                                TsType.UnionType.combine(Arrays.asList(optionalType.type, TsType.Null, TsType.Undefined)));
                                    }
                                    if (settings.optionalPropertiesDeclaration == OptionalPropertiesDeclaration.undefinableType) {
                                        return property.withTsType(
                                                TsType.UnionType.combine(Arrays.asList(optionalType.type, TsType.Undefined)));
                                    }
                                }
                                return property;
                            })
                            .collect(Collectors.toList())
                    );
                })
                .collect(Collectors.toList())
        );
    }

    private TsModel sortPropertiesDeclarations(SymbolTable symbolTable, TsModel tsModel) {
        if (settings.sortDeclarations) {
            for (TsBeanModel bean : tsModel.getBeans()) {
                Collections.sort(bean.getProperties());
            }
        }
        return tsModel;
    }

    private TsModel sortTypeDeclarations(SymbolTable symbolTable, TsModel tsModel) {
        final List<TsBeanModel> beans = tsModel.getBeans();
        final List<TsAliasModel> aliases = tsModel.getTypeAliases();
        final List<TsEnumModel> enums = tsModel.getEnums();
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
            final TsType.Context context = new TsType.Context();
            final List<TsPropertyModel> newProperties = new ArrayList<>();
            for (TsPropertyModel property : bean.getProperties()) {
                final TsType newType = TsType.transformTsType(context, property.getTsType(), transformer);
                newProperties.add(property.withTsType(newType));
            }
            final List<TsMethodModel> newMethods = new ArrayList<>();
            for (TsMethodModel method : bean.getMethods()) {
                final List<TsParameterModel> newParameters = new ArrayList<>();
                for (TsParameterModel parameter : method.getParameters()) {
                    final TsType newParameterType = TsType.transformTsType(context, parameter.getTsType(), transformer);
                    newParameters.add(new TsParameterModel(parameter.getAccessibilityModifier(), parameter.getName(), newParameterType));
                }
                final TsType newReturnType = TsType.transformTsType(context, method.getReturnType(), transformer);
                newMethods.add(new TsMethodModel(method.getName(), method.getModifiers(), method.getTypeParameters(), newParameters, newReturnType, method.getBody(), method.getComments()));
            }
            final List<TsType> newImplements = new ArrayList<>();
            for (TsType type: bean.getImplementsList()) {
                if (type instanceof TsType.GenericBasicType || type instanceof TsType.GenericReferenceType) {
                    newImplements.add(TsType.transformTsType(context, type, transformer));
                } else {
                    newImplements.add(type);
                }
            }
            final List<TsType> newExtends = new ArrayList<>();
            for (TsType type: bean.getExtendsList()) {
                if (type instanceof TsType.GenericBasicType || type instanceof TsType.GenericReferenceType) {
                    newExtends.add(TsType.transformTsType(context, type, transformer));
                } else {
                    newExtends.add(type);
                }
            }
            newBeans.add(bean.withProperties(newProperties).withMethods(newMethods).withImplements(newImplements).withExtends(newExtends));
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

    public static String getValidIdentifierName(String name) {
        final String identifier = removeInvalidIdentifierCharacters(replaceDashPattern(name));
        final String prefix = SymbolTable.isReservedWord(identifier) ? "_" : "";
        return prefix + identifier;
    }

    private static String replaceDashPattern(String name) {
        final StringBuffer sb = new StringBuffer();
        final Matcher matcher = Pattern.compile("-[^-]").matcher(name);
        while (matcher.find()) {
            matcher.appendReplacement(sb, Matcher.quoteReplacement("" + Character.toUpperCase(matcher.group().charAt(1))));
        }
        matcher.appendTail(sb);
        return sb.toString();
    }

    private static String removeInvalidIdentifierCharacters(String name) {
        final StringBuilder sb = new StringBuilder();
        for (char c : name.toCharArray()) {
            if (sb.length() == 0 ? isValidIdentifierStart(c) : isValidIdentifierPart(c)) {
                sb.append(c);
            }
        }
        return sb.toString();
    }

    public static boolean isValidIdentifierName(String name) {
        if (name == null || name.isEmpty()) {
            return false;
        }
        for (int i = 0; i < name.length(); i++) {
            final char c = name.charAt(i);
            if (i == 0 ? !isValidIdentifierStart(c) : !isValidIdentifierPart(c)) {
                return false;
            }
        }
        return true;
    }

    // https://github.com/Microsoft/TypeScript/blob/master/doc/spec-ARCHIVED.md#222-property-names
    // http://www.ecma-international.org/ecma-262/6.0/index.html#sec-names-and-keywords

    private static boolean isValidIdentifierStart(char start) {
        return Character.isUnicodeIdentifierStart(start) || start == '$' || start == '_';
    }

    private static boolean isValidIdentifierPart(char c) {
        return Character.isUnicodeIdentifierPart(c) || c == '$' || c == '_' || c == '\u200C' || c == '\u200D';
    }

}
