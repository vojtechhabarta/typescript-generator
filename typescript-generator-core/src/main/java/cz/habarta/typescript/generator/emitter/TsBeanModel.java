
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.*;
import java.util.*;


public class TsBeanModel extends TsDeclarationModel {

    private final TsType parent;
    private final List<Class<?>> taggedUnionClasses;
    private final List<TsType> interfaces;
    private final List<TsPropertyModel> properties;

    public TsBeanModel(TsType name, TsType parent, List<Class<?>> taggedUnionClasses, List<TsType> interfaces, List<TsPropertyModel> properties, List<String> comments) {
        this(null, name, parent, taggedUnionClasses, interfaces, properties, comments);
    }

    public TsBeanModel(Class<?> origin, TsType name, TsType parent, List<Class<?>> taggedUnionClasses, List<TsType> interfaces, List<TsPropertyModel> properties, List<String> comments) {
        super(origin, name, comments);
        this.parent = parent;
        this.taggedUnionClasses = taggedUnionClasses;
        this.interfaces = interfaces;
        this.properties = properties;
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

    public List<TsPropertyModel> getProperties() {
        return properties;
    }

    public TsBeanModel withProperties(List<TsPropertyModel> properties) {
        return new TsBeanModel(origin, name, parent, taggedUnionClasses, interfaces, properties, comments);
    }

}
