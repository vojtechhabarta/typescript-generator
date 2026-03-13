
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
import jakarta.xml.bind.JAXB;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;


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
        for (BeanModel bean : model.getBeans()) {
            final BeanModel dBean = enrichBean(bean);
            dBeans.add(dBean);
        }
        final List<EnumModel> dEnums = new ArrayList<>();
        for (EnumModel enumModel : model.getEnums()) {
            final EnumModel dEnumModel = enrichEnum(enumModel);
            dEnums.add(dEnumModel);
        }
        final List<RestApplicationModel> dRestApplications = new ArrayList<>();
        for (RestApplicationModel restApplication : model.getRestApplications()) {
            final RestApplicationModel dRestApplication = enrichRestApplication(restApplication);
            dRestApplications.add(dRestApplication);
        }
        return new Model(dBeans, dEnums, dRestApplications);
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
        return bean
                .withProperties(enrichedProperties)
                .withComments(combineComments(getComments(beanComment, tags), bean.getComments()));
    }

    private PropertyModel enrichProperty(PropertyModel property, List<Field> dFields, List<Method> dMethods) {
        String propertyComment = null;
        List<TagInfo> tags = null;
        if (property.getOriginalMember() instanceof java.lang.reflect.Method) {
            final java.lang.reflect.Method method = (java.lang.reflect.Method) property.getOriginalMember();
            final Method dMethod = findJavadocMethod(method.getName(), dMethods);
            propertyComment = dMethod != null ? dMethod.getComment() : null;
            tags = dMethod != null ? dMethod.getTag() : null;
        } else if (property.getOriginalMember() instanceof java.lang.reflect.Field) {
            final java.lang.reflect.Field field = (java.lang.reflect.Field) property.getOriginalMember();
            final Field dField = findJavadocField(field.getName(), dFields);
            propertyComment = dField != null ? dField.getComment() : null;
            tags = dField != null ? dField.getTag() : null;
        } 
        if (propertyComment == null )  {
            //give a chance for comments on fields but not on getter setters
            final Field dField = findJavadocField(property.getName(), dFields);
            propertyComment = dField != null ? dField.getComment() : null;
            tags = dField != null ? dField.getTag() : null;
        }
        return property
                .withComments(combineComments(getComments(propertyComment, tags), property.getComments()));
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
        return enumModel
                .withMembers(enrichedMembers)
                .withComments(combineComments(getComments(enumComment, tags), enumModel.getComments()));
    }

    private EnumMemberModel enrichEnumMember(EnumMemberModel enumMember, Enum dEnum) {
        final EnumConstant dConstant = findJavadocEnumConstant(enumMember.getPropertyName(), dEnum);
        final List<TagInfo> tags = dConstant != null ? dConstant.getTag(): null;
        final String memberComment = dConstant != null ? dConstant.getComment() : null;
        return enumMember
                .withComments(combineComments(getComments(memberComment, tags), enumMember.getComments()));
    }

    private RestApplicationModel enrichRestApplication(RestApplicationModel restApplicationModel) {
        final List<RestMethodModel> enrichedRestMethods = new ArrayList<>();
        for (RestMethodModel restMethod : restApplicationModel.getMethods()) {
            final RestMethodModel enrichedRestMethod = enrichRestMethod(restMethod);
            enrichedRestMethods.add(enrichedRestMethod);
        }
        return restApplicationModel.withMethods(enrichedRestMethods);
    }

    private RestMethodModel enrichRestMethod(RestMethodModel method) {
        final Method dMethod = findJavadocMethod(method.getOriginClass(), method.getName(), dRoots);
        final String comment = dMethod != null ? dMethod.getComment() : null;
        final List<TagInfo> tags = dMethod != null ? dMethod.getTag() : null;
        return method
                .withComments(combineComments(getComments(comment, tags), method.getComments()));
    }

    // finders

    private static Method findJavadocMethod(java.lang.Class<?> cls, String name, List<Root> dRoots) {
        final Class dClass = findJavadocClass(cls, dRoots);
        final Interface dInterface = findJavadocInterface(cls, dRoots);
        if (dClass != null) {
            return findJavadocMethod(name, dClass.getMethod());
        } else if (dInterface != null) {
            return findJavadocMethod(name, dInterface.getMethod());
        } else {
            return null;
        }
    }

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
            result.addAll(Utils.splitMultiline(convertAnchorToLink(replacedHtmlLines), true));
        }
        if (tags != null) {
            for (TagInfo tag : tags) {
                result.addAll(Utils.splitMultiline(tag.getName() + " " + convertAnchorToLink(tag.getText()), true));
            }
        }
        return result;
    }

    private static List<String> combineComments(List<String> firstComments, List<String> secondComments) {
        // consider putting tags (from both comments) after regular comments
        return Utils.concat(firstComments, secondComments);
    }

    /**
     * Replaces all anchor tags in the given string with TSdoc-style links.
     *
     * @param comment comment to convert
     * @return comment with Javadoc anchor tags converted to TSdoc links
     */
    private static String convertAnchorToLink(String comment) {
        return comment.replaceAll("<a.*?href=(?:\\s+)?\"(.*)\">(?:\\s+)?(.*)</a>", "{@link $1 $2}");
    }

}
