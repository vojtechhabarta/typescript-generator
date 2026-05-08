
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.compiler.Symbol;
import cz.habarta.typescript.generator.util.Utils;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;


public class TsBeanModel extends TsDeclarationModel {

    private final boolean isClass;
    private final List<TsDecorator> decorators;
    private final List<TsType.GenericVariableType> typeParameters;
    private final @Nullable TsType parent;
    private final List<TsType> extendsList;
    private final List<TsType> implementsList;
    private final List<Class<?>> taggedUnionClasses;
    private final @Nullable String discriminantProperty;
    private final @Nullable String discriminantLiteral;
    private final @Nullable TsAliasModel taggedUnionAlias;
    private final List<TsPropertyModel> properties;
    private final @Nullable TsConstructorModel constructor;
    private final List<TsMethodModel> methods;

    public TsBeanModel(
        @Nullable Class<?> origin,
        TsBeanCategory category,
        boolean isClass,
        Symbol name,
        @Nullable List<TsType.GenericVariableType> typeParameters,
        @Nullable TsType parent,
        @Nullable List<TsType> extendsList,
        @Nullable List<TsType> implementsList,
        @Nullable List<TsPropertyModel> properties,
        @Nullable TsConstructorModel constructor,
        @Nullable List<TsMethodModel> methods,
        @Nullable List<String> comments
    ) {
        this(origin, category, isClass, null, name, typeParameters, parent, extendsList, implementsList, null, null, null, null, properties, constructor, methods, comments);
    }

    private TsBeanModel(
        @Nullable Class<?> origin,
        @Nullable TsBeanCategory category,
        boolean isClass,
        @Nullable List<TsDecorator> decorators,
        Symbol name,
        @Nullable List<TsType.GenericVariableType> typeParameters,
        @Nullable TsType parent,
        @Nullable List<TsType> extendsList,
        @Nullable List<TsType> implementsList,
        @Nullable List<Class<?>> taggedUnionClasses,
        @Nullable String discriminantProperty,
        @Nullable String discriminantLiteral,
        @Nullable TsAliasModel taggedUnionAlias,
        @Nullable List<TsPropertyModel> properties,
        @Nullable TsConstructorModel constructor,
        @Nullable List<TsMethodModel> methods,
        @Nullable List<String> comments
    ) {
        super(origin, category, name, comments);
        this.isClass = isClass;
        this.decorators = Utils.listFromNullable(decorators);
        this.typeParameters = Utils.listFromNullable(typeParameters);
        this.parent = parent;
        this.extendsList = Utils.listFromNullable(extendsList);
        this.implementsList = Utils.listFromNullable(implementsList);
        this.taggedUnionClasses = Utils.listFromNullable(taggedUnionClasses);
        this.discriminantProperty = discriminantProperty;
        this.discriminantLiteral = discriminantLiteral;
        this.taggedUnionAlias = taggedUnionAlias;
        this.properties = Utils.listFromNullable(properties);
        this.constructor = constructor;
        this.methods = Utils.listFromNullable(methods);
    }

    public boolean isClass() {
        return isClass;
    }

    public List<TsDecorator> getDecorators() {
        return decorators;
    }

    public TsBeanModel withDecorators(List<TsDecorator> decorators) {
        return new TsBeanModel(origin, category, isClass, decorators, name, typeParameters, parent, extendsList, implementsList,
            taggedUnionClasses, discriminantProperty, discriminantLiteral, taggedUnionAlias, properties, constructor, methods, comments);
    }

    public List<TsType.GenericVariableType> getTypeParameters() {
        return typeParameters;
    }

    public @Nullable TsType getParent() {
        return parent;
    }

    public List<TsType> getExtendsList() {
        return extendsList;
    }

    public List<TsType> getImplementsList() {
        return implementsList;
    }

    public List<TsType> getAllParents() {
        final List<TsType> parents = new ArrayList<>();
        parents.addAll(extendsList);
        parents.addAll(implementsList);
        return parents;
    }

    public List<Class<?>> getTaggedUnionClasses() {
        return taggedUnionClasses;
    }

    public @Nullable String getDiscriminantProperty() {
        return discriminantProperty;
    }

    public @Nullable String getDiscriminantLiteral() {
        return discriminantLiteral;
    }

    public TsBeanModel withTaggedUnion(@Nullable List<Class<?>> taggedUnionClasses, @Nullable String discriminantProperty, @Nullable String discriminantLiteral) {
        return new TsBeanModel(origin, category, isClass, decorators, name, typeParameters, parent, extendsList, implementsList,
            taggedUnionClasses, discriminantProperty, discriminantLiteral, taggedUnionAlias, properties, constructor, methods, comments);
    }

    public @Nullable TsAliasModel getTaggedUnionAlias() {
        return taggedUnionAlias;
    }

    public TsBeanModel withTaggedUnionAlias(@Nullable TsAliasModel taggedUnionAlias) {
        return new TsBeanModel(origin, category, isClass, decorators, name, typeParameters, parent, extendsList, implementsList,
            taggedUnionClasses, discriminantProperty, discriminantLiteral, taggedUnionAlias, properties, constructor, methods, comments);
    }

    public List<TsPropertyModel> getProperties() {
        return properties;
    }

    public TsBeanModel withProperties(List<TsPropertyModel> properties) {
        return new TsBeanModel(origin, category, isClass, decorators, name, typeParameters, parent, extendsList, implementsList,
            taggedUnionClasses, discriminantProperty, discriminantLiteral, taggedUnionAlias, properties, constructor, methods, comments);
    }

    public @Nullable TsConstructorModel getConstructor() {
        return constructor;
    }

    public TsBeanModel withConstructor(TsConstructorModel constructor) {
        return new TsBeanModel(origin, category, isClass, decorators, name, typeParameters, parent, extendsList, implementsList,
            taggedUnionClasses, discriminantProperty, discriminantLiteral, taggedUnionAlias, properties, constructor, methods, comments);
    }

    public List<TsMethodModel> getMethods() {
        return methods;
    }

    public TsBeanModel withMethods(List<TsMethodModel> methods) {
        return new TsBeanModel(origin, category, isClass, decorators, name, typeParameters, parent, extendsList, implementsList,
            taggedUnionClasses, discriminantProperty, discriminantLiteral, taggedUnionAlias, properties, constructor, methods, comments);
    }

    public boolean isJaxrsApplicationClientBean() {
        return category == TsBeanCategory.Service && isClass;
    }

    public boolean isDataClass() {
        return category == TsBeanCategory.Data && isClass;
    }

    public TsBeanModel withImplements(List<TsType> implementsList) {
        return new TsBeanModel(origin, category, isClass, decorators, name, typeParameters, parent, extendsList, implementsList,
            taggedUnionClasses, discriminantProperty, discriminantLiteral, taggedUnionAlias, properties, constructor, methods, comments);
    }

    public TsBeanModel withExtends(List<TsType> extendsList) {
        return new TsBeanModel(origin, category, isClass, decorators, name, typeParameters, parent, extendsList, implementsList,
            taggedUnionClasses, discriminantProperty, discriminantLiteral, taggedUnionAlias, properties, constructor, methods, comments);
    }

}
