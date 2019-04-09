
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.*;
import cz.habarta.typescript.generator.compiler.Symbol;
import cz.habarta.typescript.generator.util.Utils;
import java.util.*;


public class TsBeanModel extends TsDeclarationModel {

    private final boolean isClass;
    private final List<TsType.GenericVariableType> typeParameters;
    private final TsType parent;
    private final List<TsType> extendsList;
    private final List<TsType> implementsList;
    private final List<Class<?>> taggedUnionClasses;
    private final String discriminantProperty;
    private final String discriminantLiteral;
    private final TsAliasModel taggedUnionAlias;
    private final List<TsPropertyModel> properties;
    private final TsConstructorModel constructor;
    private final List<TsMethodModel> methods;

    public TsBeanModel(
            Class<?> origin,
            TsBeanCategory category,
            boolean isClass,
            Symbol name,
            List<TsType.GenericVariableType> typeParameters,
            TsType parent,
            List<TsType> extendsList,
            List<TsType> implementsList,
            List<TsPropertyModel> properties,
            TsConstructorModel constructor,
            List<TsMethodModel> methods,
            List<String> comments) {
        this(origin, category, isClass, name, typeParameters, parent, extendsList, implementsList, null, null, null, null, properties, constructor, methods, comments);
    }

    private TsBeanModel(
            Class<?> origin,
            TsBeanCategory category,
            boolean isClass,
            Symbol name,
            List<TsType.GenericVariableType> typeParameters,
            TsType parent,
            List<TsType> extendsList,
            List<TsType> implementsList,
            List<Class<?>> taggedUnionClasses,
            String discriminantProperty,
            String discriminantLiteral,
            TsAliasModel taggedUnionAlias,
            List<TsPropertyModel> properties,
            TsConstructorModel constructor,
            List<TsMethodModel> methods,
            List<String> comments) {
        super(origin, category, name, comments);
        this.isClass = isClass;
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

    public List<TsType.GenericVariableType> getTypeParameters() {
        return typeParameters;
    }

    public TsType getParent() {
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

    public String getDiscriminantProperty() {
        return discriminantProperty;
    }

    public String getDiscriminantLiteral() {
        return discriminantLiteral;
    }

    public TsBeanModel withTaggedUnion(List<Class<?>> taggedUnionClasses, String discriminantProperty, String discriminantLiteral) {
        return new TsBeanModel(origin, category, isClass, name, typeParameters, parent, extendsList, implementsList, taggedUnionClasses, discriminantProperty, discriminantLiteral, taggedUnionAlias, properties, constructor, methods, comments);
    }

    public TsAliasModel getTaggedUnionAlias() {
        return taggedUnionAlias;
    }

    public TsBeanModel withTaggedUnionAlias(TsAliasModel taggedUnionAlias) {
        return new TsBeanModel(origin, category, isClass, name, typeParameters, parent, extendsList, implementsList, taggedUnionClasses, discriminantProperty, discriminantLiteral, taggedUnionAlias, properties, constructor, methods, comments);
    }

    public List<TsPropertyModel> getProperties() {
        return properties;
    }

    public TsBeanModel withProperties(List<TsPropertyModel> properties) {
        return new TsBeanModel(origin, category, isClass, name, typeParameters, parent, extendsList, implementsList, taggedUnionClasses, discriminantProperty, discriminantLiteral, taggedUnionAlias, properties, constructor, methods, comments);
    }

    public TsConstructorModel getConstructor() {
        return constructor;
    }

    public TsBeanModel withConstructor(TsConstructorModel constructor) {
        return new TsBeanModel(origin, category, isClass, name, typeParameters, parent, extendsList, implementsList, taggedUnionClasses, discriminantProperty, discriminantLiteral, taggedUnionAlias, properties, constructor, methods, comments);
    }

    public List<TsMethodModel> getMethods() {
        return methods;
    }

    public TsBeanModel withMethods(List<TsMethodModel> methods) {
        return new TsBeanModel(origin, category, isClass, name, typeParameters, parent, extendsList, implementsList, taggedUnionClasses, discriminantProperty, discriminantLiteral, taggedUnionAlias, properties, constructor, methods, comments);
    }

    public boolean isJaxrsApplicationClientBean() {
        return category == TsBeanCategory.Service && isClass;
    }
    
    public boolean isDataClass() {
        return category == TsBeanCategory.Data && isClass;
    }
    
}
