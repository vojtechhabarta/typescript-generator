
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.xmldoclet.Class;
import cz.habarta.typescript.generator.xmldoclet.Enum;
import cz.habarta.typescript.generator.xmldoclet.Field;
import cz.habarta.typescript.generator.xmldoclet.Method;
import cz.habarta.typescript.generator.xmldoclet.Package;
import cz.habarta.typescript.generator.xmldoclet.Root;
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
        final Class dClass = findJavadocClass(bean.getBeanClass(), dRoots);
        final List<String> beanComments = getClassComments(dClass);
        final List<PropertyModel> enrichedProperties = new ArrayList<>();
        for (PropertyModel property : bean.getProperties()) {
            final PropertyModel enrichedProperty = enrichProperty(property, dClass);
            enrichedProperties.add(enrichedProperty);
        }
        return new BeanModel(bean.getBeanClass(), bean.getParent(), enrichedProperties, concat(beanComments, bean.getComments()));
    }

    private PropertyModel enrichProperty(PropertyModel property, Class dClass) {
        final List<String> propertyComments;
        if (property.getOriginalMember() instanceof java.lang.reflect.Method) {
            final Method dMethod = findJavadocMethod(property.getOriginalMember().getName(), dClass);
            propertyComments = getMethodComments(dMethod);
        } else if (property.getOriginalMember() instanceof java.lang.reflect.Field) {
            final Field dField = findJavadocField(property.getOriginalMember().getName(), dClass);
            propertyComments = getFieldComments(dField);
        } else {
            final Field dField = findJavadocField(property.getName(), dClass);
            propertyComments = getFieldComments(dField);
        }
        return property.comments(propertyComments);
    }

    private EnumModel enrichEnum(EnumModel enumModel) {
        final Enum dEnum = findJavadocEnum(enumModel.getEnumClass(), dRoots);
        final List<String> enumComments = getEnumComments(dEnum);
        return new EnumModel(enumModel.getEnumClass(), enumModel.getValues(), concat(enumComments, enumModel.getComments()));
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

    private static Field findJavadocField(String name, Class dClass) {
        if (dClass != null) {
            for (Field dField : dClass.getField()) {
                if (dField.getName().equals(name)) {
                    return dField;
                }
            }
        }
        return null;
    }

    private static Method findJavadocMethod(String name, Class dClass) {
        if (dClass != null) {
            for (Method dMethod : dClass.getMethod()) {
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

    // comment getters

    private List<String> getClassComments(Class dClass) {
        return dClass != null ? getComments(dClass.getComment()) : null;
    }

    private List<String> getFieldComments(Field dField) {
        return dField != null ? getComments(dField.getComment()) : null;
    }

    private List<String> getMethodComments(Method dMethod) {
        return dMethod != null ? getComments(dMethod.getComment()) : null;
    }

    private List<String> getEnumComments(Enum dEnum) {
        return dEnum != null ? getComments(dEnum.getComment()) : null;
    }

    private List<String> getComments(String dComments) {
        if (dComments == null) {
            return null;
        }
        final String[] lines = dComments.split("\\r\\n|\\n|\\r");
        final List<String> result = new ArrayList<>();
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
