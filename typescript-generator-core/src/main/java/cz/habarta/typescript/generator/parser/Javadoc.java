
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.xmldoclet.Class;
import cz.habarta.typescript.generator.xmldoclet.Enum;
import cz.habarta.typescript.generator.xmldoclet.Field;
import cz.habarta.typescript.generator.xmldoclet.Interface;
import cz.habarta.typescript.generator.xmldoclet.Method;
import cz.habarta.typescript.generator.xmldoclet.Package;
import cz.habarta.typescript.generator.xmldoclet.Root;
import cz.habarta.typescript.generator.xmldoclet.TagInfo;
import java.io.File;
import java.util.*;
import javax.xml.bind.JAXB;


public class Javadoc {

    private final List<Root> dRoots;

    public Javadoc(List<File> javadocXmlFiles) {
        this.dRoots = loadJavadocXmlFiles(javadocXmlFiles);
    }

    private static List<Root> loadJavadocXmlFiles(List<File> javadocXmlFiles) {
        final List<Root> dRoots = new ArrayList<>();
        if (javadocXmlFiles != null) {
            for (File file : javadocXmlFiles) {
                System.out.println("Loading Javadoc XML file: " + file);
                final Root dRoot = JAXB.unmarshal(file, Root.class);
                dRoots.add(dRoot);
            }
        }
        return dRoots;
    }

    // enrichers

    public Model enrichModel(Model model) {
        final List<BeanModel> dBeans = new ArrayList<>();
        final List<EnumModel> dEnums = new ArrayList<>();
        for (BeanModel bean : model.getBeans()) {
            final BeanModel dBean = enrichBean(bean);
            dBeans.add(dBean);
        }
        for (EnumModel enumModel : model.getEnums()) {
            final EnumModel dEnumModel = enrichEnum(enumModel);
            dEnums.add(dEnumModel);
        }
        return new Model(dBeans, dEnums);
    }

    private BeanModel enrichBean(BeanModel bean) {
        if (bean.getBeanClass().isInterface()) {
            final Interface dInterface = findJavadocInterface(bean.getBeanClass(), dRoots);
            if (dInterface != null) {
                return enrichBean(bean, dInterface.getComment(), dInterface.getTag(), dInterface.getField(), dInterface.getMethod());
            }
        } else {
            final Class dClass = findJavadocClass(bean.getBeanClass(), dRoots);
            if (dClass != null) {
                return enrichBean(bean, dClass.getComment(), dClass.getTag(), dClass.getField(), dClass.getMethod());
            }
        }
        return bean;
    }

    private BeanModel enrichBean(BeanModel bean, String beanComment, List<TagInfo> tags, List<Field> dFields, List<Method> dMethods) {
        final List<PropertyModel> enrichedProperties = new ArrayList<>();
        for (PropertyModel property : bean.getProperties()) {
            final PropertyModel enrichedProperty = enrichProperty(property, dFields, dMethods);
            enrichedProperties.add(enrichedProperty);
        }
        return new BeanModel(bean.getBeanClass(), bean.getParent(), bean.getInterfaces(), enrichedProperties, concat(getComments(beanComment, tags), bean.getComments()));
    }

    private PropertyModel enrichProperty(PropertyModel property, List<Field> dFields, List<Method> dMethods) {
        final String propertyComment;
        final List<TagInfo> tags;
        if (property.getOriginalMember() instanceof java.lang.reflect.Method) {
            final Method dMethod = findJavadocMethod(property.getOriginalMember().getName(), dMethods);
            propertyComment = dMethod != null ? dMethod.getComment() : null;
            tags = dMethod != null ? dMethod.getTag() : null;
        } else if (property.getOriginalMember() instanceof java.lang.reflect.Field) {
            final Field dField = findJavadocField(property.getOriginalMember().getName(), dFields);
            propertyComment = dField != null ? dField.getComment() : null;
            tags = dField != null ? dField.getTag() : null;
        } else {
            final Field dField = findJavadocField(property.getName(), dFields);
            propertyComment = dField != null ? dField.getComment() : null;
            tags = dField != null ? dField.getTag() : null;
        }
        return property.comments(getComments(propertyComment, tags));
    }

    private EnumModel enrichEnum(EnumModel enumModel) {
        final Enum dEnum = findJavadocEnum(enumModel.getEnumClass(), dRoots);
        final String enumComment = dEnum != null ? dEnum.getComment() : null;
        final List<TagInfo> tags = dEnum != null ? dEnum.getTag() : null;
        return new EnumModel(enumModel.getEnumClass(), enumModel.getValues(),
                concat(getComments(enumComment, tags), enumModel.getComments()),
                enumModel.getValueModel());
    }

    // finders

    private static Class findJavadocClass(java.lang.Class<?> cls, List<Root> dRoots) {
        final String name = cls.getName().replace('$', '.');
        for (Root dRoot : dRoots) {
            for (Package dPackage : dRoot.getPackage()) {
                for (Class dClass : dPackage.getClazz()) {
                    if (dClass.getQualified().equals(name)) {
                        return dClass;
                    }
                }
            }
        }
        return null;
    }

    private static Interface findJavadocInterface(java.lang.Class<?> cls, List<Root> dRoots) {
        final String name = cls.getName().replace('$', '.');
        for (Root dRoot : dRoots) {
            for (Package dPackage : dRoot.getPackage()) {
                for (Interface dInterface : dPackage.getInterface()) {
                    if (dInterface.getQualified().equals(name)) {
                        return dInterface;
                    }
                }
            }
        }
        return null;
    }

    private static Field findJavadocField(String name, List<Field> dFields) {
        if (dFields != null) {
            for (Field dField : dFields) {
                if (dField.getName().equals(name)) {
                    return dField;
                }
            }
        }
        return null;
    }

    private static Method findJavadocMethod(String name, List<Method> dMethods) {
        if (dMethods != null) {
            for (Method dMethod : dMethods) {
                if (dMethod.getName().equals(name)) {
                    return dMethod;
                }
            }
        }
        return null;
    }

    private static Enum findJavadocEnum(java.lang.Class<?> cls, List<Root> dRoots) {
        final String name = cls.getName().replace('$', '.');
        for (Root dRoot : dRoots) {
            for (Package dPackage : dRoot.getPackage()) {
                for (Enum dEnum : dPackage.getEnum()) {
                    if (dEnum.getQualified().equals(name)) {
                        return dEnum;
                    }
                }
            }
        }
        return null;
    }

    private static List<String> getComments(String dComments, List<TagInfo> tags) {
        if (dComments == null && (tags == null || tags.isEmpty())) {
            return null;
        }
        final List<String> result = new ArrayList<>();
        if (dComments != null) {
            result.addAll(splitMultiline(dComments));
        }
        if (tags != null) {
            for (TagInfo tag : tags) {
                result.addAll(splitMultiline(tag.getName() + " " + tag.getText()));
            }
        }
        return result;
    }

    private static List<String> splitMultiline(String comments) {
        final List<String> result = new ArrayList<>();
        final String[] lines = comments.split("\\r\\n|\\n|\\r");
        for (String line : lines) {
            result.add(line.trim());
        }
        return result;
    }

    private static <T> List<T> concat(List<? extends T> list1, List<? extends T> list2) {
        if (list1 == null && list2 == null) {
            return null;
        }
        final List<T> result = new ArrayList<>();
        if (list1 != null) result.addAll(list1);
        if (list2 != null) result.addAll(list2);
        return result;
    }
}
