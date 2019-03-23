
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import cz.habarta.typescript.generator.compiler.EnumMemberModel;
import cz.habarta.typescript.generator.util.Utils;
import cz.habarta.typescript.generator.xmldoclet.Class;
import cz.habarta.typescript.generator.xmldoclet.Enum;
import cz.habarta.typescript.generator.xmldoclet.EnumConstant;
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

    private final String newline;
    private final List<Root> dRoots;

    public Javadoc(Settings settings) {
        Objects.requireNonNull(settings, "settings");
        this.newline = settings.newline;
        this.dRoots = loadJavadocXmlFiles(settings.javadocXmlFiles);
    }

    private static List<Root> loadJavadocXmlFiles(List<File> javadocXmlFiles) {
        final List<Root> dRoots = new ArrayList<>();
        if (javadocXmlFiles != null) {
            for (File file : javadocXmlFiles) {
                TypeScriptGenerator.getLogger().info("Loading Javadoc XML file: " + file);
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
        return new Model(dBeans, dEnums, model.getRestApplications());
    }

    private BeanModel enrichBean(BeanModel bean) {
        if (bean.getOrigin().isInterface()) {
            final Interface dInterface = findJavadocInterface(bean.getOrigin(), dRoots);
            if (dInterface != null) {
                return enrichBean(bean, dInterface.getComment(), dInterface.getTag(), dInterface.getField(), dInterface.getMethod());
            }
        } else {
            final Class dClass = findJavadocClass(bean.getOrigin(), dRoots);
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
        return bean.withProperties(enrichedProperties).withComments(Utils.concat(getComments(beanComment, tags), bean.getComments()));
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
        return property.withComments(getComments(propertyComment, tags));
    }

    private EnumModel enrichEnum(EnumModel enumModel) {
        final Enum dEnum = findJavadocEnum(enumModel.getOrigin(), dRoots);
        final List<EnumMemberModel> enrichedMembers = new ArrayList<>();
        for (EnumMemberModel member : enumModel.getMembers()) {
            final EnumMemberModel enrichedMember = enrichEnumMember(member, dEnum);
            enrichedMembers.add(enrichedMember);
        }
        final String enumComment = dEnum != null ? dEnum.getComment() : null;
        final List<TagInfo> tags = dEnum != null ? dEnum.getTag() : null;
        return enumModel.withMembers(enrichedMembers).withComments(Utils.concat(getComments(enumComment, tags), enumModel.getComments()));
    }

    private EnumMemberModel enrichEnumMember(EnumMemberModel enumMember, Enum dEnum) {
        final EnumConstant dConstant = findJavadocEnumConstant(enumMember.getPropertyName(), dEnum);
        final List<TagInfo> tags = dConstant != null ? dConstant.getTag(): null;
        final String memberComment = dConstant != null ? dConstant.getComment() : null;
        return enumMember.withComments(Utils.concat(getComments(memberComment, tags), enumMember.getComments()));
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

    private static EnumConstant findJavadocEnumConstant(String name, Enum dEnum) {
        if (dEnum != null) {
            for (EnumConstant dEnumConstant : dEnum.getConstant()) {
                if (dEnumConstant.getName().equals(name)) {
                    return dEnumConstant;
                }
            }
        }
        return null;
    }

    private List<String> getComments(String dComments, List<TagInfo> tags) {
        if (dComments == null && (tags == null || tags.isEmpty())) {
            return null;
        }
        final List<String> result = new ArrayList<>();
        if (dComments != null) {
            final String nn = newline + newline;
            final String replacedHtmlLines = dComments
                    .replaceAll("\\s*<br>\\s*", nn)
                    .replaceAll("\\s*<br/>\\s*", nn)
                    .replaceAll("\\s*<br />\\s*", nn)
                    .replaceAll("\\s*<p>\\s*", nn)
                    .replaceAll("\\s*</p>\\s*", nn);
            result.addAll(Utils.splitMultiline(replacedHtmlLines, true));
        }
        if (tags != null) {
            for (TagInfo tag : tags) {
                result.addAll(Utils.splitMultiline(tag.getName() + " " + tag.getText(), true));
            }
        }
        return result;
    }

}
