
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.*;
import cz.habarta.typescript.generator.compiler.Symbol;
import cz.habarta.typescript.generator.util.Utils;
import java.util.*;


public class TsBeanModel extends TsDeclarationModel {

    private final boolean isClass;
    private final List<TsType.GenericVariableType> typeParameters;
    private final TsType parent;
    private final List<Class<?>> taggedUnionClasses;
    private final String discriminantProperty;
    private final String discriminantLiteral;
    private final TsAliasModel taggedUnionAlias;
    private final List<TsType> interfaces;
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
            List<TsType> interfaces,
            List<TsPropertyModel> properties,
            TsConstructorModel constructor,
            List<TsMethodModel> methods,
            List<String> comments) {
        this(origin, category, isClass, name, typeParameters, parent, null, null, null, null, interfaces, properties, constructor, methods, comments);
    }

    private TsBeanModel(
            Class<?> origin,
            TsBeanCategory category,
            boolean isClass,
            Symbol name,
            List<TsType.GenericVariableType> typeParameters,
            TsType parent,
            List<Class<?>> taggedUnionClasses,
            String discriminantProperty,
            String discriminantLiteral,
            TsAliasModel taggedUnionAlias,
            List<TsType> interfaces,
            List<TsPropertyModel> properties,
            TsConstructorModel constructor,
            List<TsMethodModel> methods,
            List<String> comments) {
        super(origin, category, name, comments);
        this.isClass = isClass;
        this.typeParameters = Utils.listFromNullable(typeParameters);
        this.parent = parent;
        this.taggedUnionClasses = Utils.listFromNullable(taggedUnionClasses);
        this.discriminantProperty = discriminantProperty;
        this.discriminantLiteral = discriminantLiteral;
        this.taggedUnionAlias = taggedUnionAlias;
        this.interfaces = Utils.listFromNullable(interfaces);
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
        return new TsBeanModel(origin, category, isClass, name, typeParameters, parent, taggedUnionClasses, discriminantProperty, discriminantLiteral, taggedUnionAlias, interfaces, properties, constructor, methods, comments);
    }

    public TsAliasModel getTaggedUnionAlias() {
        return taggedUnionAlias;
    }

    public TsBeanModel withTaggedUnionAlias(TsAliasModel taggedUnionAlias) {
        return new TsBeanModel(origin, category, isClass, name, typeParameters, parent, taggedUnionClasses, discriminantProperty, discriminantLiteral, taggedUnionAlias, interfaces, properties, constructor, methods, comments);
    }

    public List<TsType> getInterfaces() {
        return interfaces;
    }

    public List<TsType> getParentAndInterfaces() {
        final List<TsType> parents = new ArrayList<>();
        if (parent != null) {
            parents.add(parent);
        }
        parents.addAll(interfaces);
        return parents;
    }

    public List<TsType> getExtendsList() {
        return isClass
                ? Utils.listFromNullable(parent)
                : getParentAndInterfaces();
    }

    public List<TsType> getImplementsList() {
        return isClass
                ? interfaces
                : Collections.<TsType>emptyList();
    }

    public List<TsPropertyModel> getProperties() {
        return properties;
    }

    
    public TsBeanModel withProperties(List<TsPropertyModel> properties) {
        return new TsBeanModel(origin, category, isClass, name, typeParameters, parent, taggedUnionClasses, discriminantProperty, discriminantLiteral, taggedUnionAlias, interfaces, properties, constructor, methods, comments);
    }

    public TsConstructorModel getConstructor() {
        return constructor;
    }

    public List<TsMethodModel> getMethods() {
        return methods;
    }

    public TsBeanModel withMethods(List<TsMethodModel> methods) {
        return new TsBeanModel(origin, category, isClass, name, typeParameters, parent, taggedUnionClasses, discriminantProperty, discriminantLiteral, taggedUnionAlias, interfaces, properties, constructor, methods, comments);
    }

    public boolean isJaxrsApplicationClientBean() {
        return category == TsBeanCategory.Service && isClass;
    }
    
    public boolean isDataClass() {
        return category == TsBeanCategory.Data && isClass;
    }
    
}
