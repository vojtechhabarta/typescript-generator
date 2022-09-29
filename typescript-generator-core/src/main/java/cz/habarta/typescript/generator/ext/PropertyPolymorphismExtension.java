package cz.habarta.typescript.generator.ext;

import cz.habarta.typescript.generator.Extension;
import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.TsType.ReferenceType;
import cz.habarta.typescript.generator.compiler.ModelCompiler.TransformationPhase;
import cz.habarta.typescript.generator.compiler.Symbol;
import cz.habarta.typescript.generator.compiler.TsModelTransformer;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.emitter.TsBeanCategory;
import cz.habarta.typescript.generator.emitter.TsBeanModel;
import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.emitter.TsPropertyModel;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Predicate;

/**
 * Extension to support property based polymorphism. Given the following
 * classes:
 *
 * <pre>
 * class ReferencingType {
 *     Base ref;
 * }
 *
 * class Base {
 * }
 *
 * class A extends Base {
 *     String foo;
 * }
 *
 * class B extends Base {
 *     String bar;
 * }
 * </pre>
 *
 * A serialization of an instance of {@code ReferencingType} would become:
 *
 * <pre>
 * {"ref": { "a": { "foo": "Hello World"} }}
 * </pre>
 *
 * Thus a synthetic object is inserted into the reference, with a property for
 * each sub type (in this case "a"). In typescript this becomes:
 *
 * <pre>
 * interface ReferencingType{ ref: BaseRef}
 * interface BaseRef{a: A; b: B}
 * interface Base{}
 * interface A extends Base {foo: string}
 * interface B extends Base{bar: string}
 * </pre>
 *
 * <b> Configuration </b><br>
 * When instantiating from java, the
 * {@link #PropertyPolymorphismExtension(Predicate, Function)} constructor or
 * the {@link #isPolymorphicBase} and {@link #getPropertyName} fields can be
 * used to supply appropriate strategies. When using in a build system, use the
 * {@value #MARKER_ANNOTATION}, {@value #NAME_ANNOTATION} and
 * {@value #NAME_ELEMENT} extensions properties.
 *
 */
public class PropertyPolymorphismExtension extends Extension {

    /**
     * Fully qualified type name of the annotation marking polymorphic base classes.
     * Required configuration parameter.
     */
    public static final String MARKER_ANNOTATION = "markerAnnotation";

    /**
     * Fully qualified type name of the annotation overriding the property name of a
     * subclass. If absent or not configured at all, the
     * {@link Class#getSimpleName()}, converted from UpperCamel to lowerCamel, is
     * used.
     */
    public static final String NAME_ANNOTATION = "nameAnnotation";

    /**
     * Name of the element in the {@value #NAME_ANNOTATION} to use as property name.
     * If not configured the {@code value} element is used.
     */
    public static final String NAME_ELEMENT = "nameElement";

    /**
     * Predicate to determine if a class is a polymorphic base class, thus
     * references to it should be replaced with a reference to the synthetic ref
     * type.
     */
    public Predicate<Class<?>> isPolymorphicBase;

    /**
     * Function to get the property name in the synthetic ref type for the given sub
     * class.
     */
    public Function<Class<?>, String> getPropertyName;

    PropertyPolymorphismExtension() {

    }

    public PropertyPolymorphismExtension(Predicate<Class<?>> isPolymorphicBase,
            Function<Class<?>, String> getPropertyName) {
        this.isPolymorphicBase = isPolymorphicBase;
        this.getPropertyName = getPropertyName;
    }

    @Override
    public EmitterExtensionFeatures getFeatures() {
        return new EmitterExtensionFeatures();
    }

    @Override
    public void setConfiguration(Map<String, String> configuration) throws RuntimeException {
        {
            String markerAnnotationName = configuration.get(MARKER_ANNOTATION);
            if (markerAnnotationName == null) {
                throw new RuntimeException("Property '" + MARKER_ANNOTATION + "' has to be configured");
            }
            isPolymorphicBase = cls -> {
                for (Annotation annotation : cls.getAnnotations()) {
                    if (markerAnnotationName.equals(annotation.annotationType().getName())) {
                        return true;
                    }
                }
                return false;
            };
        }

        {
            String nameAnnotationName = configuration.get(NAME_ANNOTATION);
            if (nameAnnotationName == null) {
                getPropertyName = this::defaultPropertyName;
            } else {
                String nameElementName = configuration.get(NAME_ELEMENT);
                if (nameElementName == null) {
                    nameElementName = "value";
                }
                String nameFieldNameFinal = nameElementName;

                getPropertyName = subType -> {
                    try {
                        for (Annotation annotation : subType.getAnnotations()) {
                            if (nameAnnotationName.equals(annotation.annotationType().getName())) {
                                Method method = annotation.annotationType().getDeclaredMethod(nameFieldNameFinal);
                                return (String) method.invoke(annotation);
                            }
                        }
                    } catch (Throwable t) {
                        throw new RuntimeException(t);
                    }
                    return defaultPropertyName(subType);
                };
            }

        }
    }

    private String defaultPropertyName(Class<?> subType) {
        String name = subType.getSimpleName();
        return name.substring(0, 1).toLowerCase(Locale.ENGLISH) + name.substring(1);
    }

    @Override
    public List<TransformerDefinition> getTransformers() {
        return Arrays
                .asList(new TransformerDefinition(TransformationPhase.BeforeSymbolResolution, new TsModelTransformer() {
                    @Override
                    public TsModel transformModel(Context context, TsModel model) {
                        List<TsBeanModel> newBeans = new ArrayList<>();

                        for (TsBeanModel bean : model.getBeans()) {
                            // replace references
                            List<TsPropertyModel> newProperties = new ArrayList<>();
                            for (TsPropertyModel property : bean.getProperties()) {
                                if (property.tsType instanceof ReferenceType) {
                                    ReferenceType type = (ReferenceType) property.tsType;
                                    TsBeanModel referencedBean = model.getBean(type.symbol);
                                    if (isPolymorphicBase.test(referencedBean.getOrigin())) {
                                        Symbol refSymbol = context.getSymbolTable().addSuffixToSymbol(type.symbol, "Ref");
                                        newProperties.add(property.withTsType(new TsType.ReferenceType(refSymbol)));
                                        continue;
                                    }
                                }
                                newProperties.add(property);
                            }
                            newBeans.add(bean.withProperties(newProperties));
                        }

                        // add reference beans
                        {
                            List<TsBeanModel> bases = new ArrayList<>();
                            Map<Class<?>, Set<Class<?>>> subTypes = new HashMap<>();
                            Map<Class<?>, TsBeanModel> beanByOrigin = new HashMap<>();
                            for (TsBeanModel bean : model.getBeans()) {
                                Class<?> origin = bean.getOrigin();
                                if (origin == null) {
                                    continue;
                                }
                                beanByOrigin.put(origin, bean);
                                if (isPolymorphicBase.test(origin)) {
                                    bases.add(bean);
                                }

                                fillSubTypes(origin, subTypes, origin, new HashSet<>());
                            }

                            for (TsBeanModel base : bases) {
                                List<TsPropertyModel> refProperties = new ArrayList<>();
                                for (Class<?> subType : subTypes.getOrDefault(base.getOrigin(),
                                        Collections.emptySet())) {
                                    refProperties.add(new TsPropertyModel(getPropertyName.apply(subType),
                                            new ReferenceType(context.getSymbolTable().getSymbol(subType)), null, true, null));
                                }
                                newBeans.add(new TsBeanModel(base.getOrigin(), TsBeanCategory.Data, false,
                                context.getSymbolTable().addSuffixToSymbol(base.getName(), "Ref"), null, null, null, null,
                                        refProperties, null, null, null));
                            }
                        }

                        return model.withBeans(newBeans);
                    }

                }));
    }

    private void fillSubTypes(Class<?> root, Map<Class<?>, Set<Class<?>>> subTypes, Class<?> cls, Set<Class<?>> seen) {
        if (cls == null || cls == Object.class) {
            return;
        }
        if (!seen.add(cls)) {
            return;
        }
        if (root != cls) {
            subTypes.computeIfAbsent(cls, k -> new HashSet<>()).add(root);
        }

        fillSubTypes(root, subTypes, cls.getSuperclass(), seen);
        for (Class<?> i : cls.getInterfaces()) {
            fillSubTypes(root, subTypes, i, seen);
        }
    }

}
