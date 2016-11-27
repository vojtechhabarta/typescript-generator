
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.*;
import cz.habarta.typescript.generator.util.Utils;
import java.util.*;


public class TsBeanModel extends TsDeclarationModel {

    private final boolean isClass;
    private final TsType parent;
    private final List<Class<?>> taggedUnionClasses;
    private final List<TsType> interfaces;
    private final List<TsPropertyModel> properties;

    public TsBeanModel(Class<?> origin, boolean isClass, TsType name, TsType parent, List<Class<?>> taggedUnionClasses, List<TsType> interfaces, List<TsPropertyModel> properties, List<String> comments) {
        super(origin, name, comments);
        this.isClass = isClass;
        this.parent = parent;
        this.taggedUnionClasses = taggedUnionClasses;
        this.interfaces = interfaces;
        this.properties = properties;
    }

    public boolean isClass() {
        return isClass;
    }

    public TsType getParent() {
        return parent;
    }

    public List<Class<?>> getTaggedUnionClasses() {
        return taggedUnionClasses;
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
        return new TsBeanModel(origin, isClass, name, parent, taggedUnionClasses, interfaces, properties, comments);
    }

}
