
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.compiler.EnumMemberModel;
import cz.habarta.typescript.generator.util.DeprecationUtils;
import cz.habarta.typescript.generator.util.Utils;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;


public class DeprecationEnricher {

    public Model enrichModel(Model model) {
        final List<BeanModel> beans = mapList(model.getBeans(), this::enrichBean);
        final List<EnumModel> enums = mapList(model.getEnums(), this::enrichEnum);
        final List<RestApplicationModel> restApplications = mapList(model.getRestApplications(), this::enrichRestApplication);
        return new Model(beans, enums, restApplications);
    }

    private BeanModel enrichBean(BeanModel bean) {
        final List<PropertyModel> properties = mapList(bean.getProperties(), property -> enrichProperty(property));
        return bean
                .withProperties(properties)
                .withComments(addDeprecation(bean.getComments(), bean.getOrigin()));
    }

    private PropertyModel enrichProperty(PropertyModel property) {
        if (property.getOriginalMember() instanceof Method) {
            final Method method = (Method) property.getOriginalMember();
            return enrichMethodProperty(property, method);
        } else if (property.getOriginalMember() instanceof Field) {
            final Field field = (Field) property.getOriginalMember();
            return enrichFieldProperty(property, field);
        } else {
            return property;
        }
    }

    private PropertyModel enrichFieldProperty(PropertyModel property, Field field) {
        return property
                .withComments(addDeprecation(property.getComments(), field));
    }

    private PropertyModel enrichMethodProperty(PropertyModel property, Method method) {
        return property
                .withComments(addDeprecation(property.getComments(), method));
    }

    private EnumModel enrichEnum(EnumModel enumModel) {
        final List<EnumMemberModel> members = mapList(enumModel.getMembers(), enumMember -> enrichEnumMember(enumMember));
        return enumModel
                .withMembers(members)
                .withComments(addDeprecation(enumModel.getComments(), enumModel.getOrigin()));
    }

    private EnumMemberModel enrichEnumMember(EnumMemberModel enumMember) {
        return enumMember
                .withComments(addDeprecation(enumMember.getComments(), enumMember.getOriginalField()));
    }

    private RestApplicationModel enrichRestApplication(RestApplicationModel restApplicationModel) {
        final List<RestMethodModel> enrichedRestMethods = mapList(restApplicationModel.getMethods(), restMethod -> enrichRestMethod(restMethod));
        return restApplicationModel.withMethods(enrichedRestMethods);
    }

    private RestMethodModel enrichRestMethod(RestMethodModel method) {
        return method
                .withComments(addDeprecation(method.getComments(), method.getOriginalMethod()));
    }

    private static <R, T> List<R> mapList(List<T> list, Function<T, R> mapper) {
        return list.stream().map(mapper).collect(Collectors.toList());
    }

    private static List<String> addDeprecation(List<String> comments, AnnotatedElement annotatedElement) {
        if (annotatedElement == null || !annotatedElement.isAnnotationPresent(Deprecated.class) || containsDeprecatedTag(comments)) {
            return comments;
        }

        String deprecatedComment = DeprecationUtils.convertToComment(annotatedElement.getAnnotation(Deprecated.class));
        return Utils.concat(comments, Collections.singletonList(deprecatedComment));
    }

    private static boolean containsDeprecatedTag(List<String> comments) {
        return comments != null
                ? comments.stream().anyMatch(comment -> comment.startsWith(DeprecationUtils.DEPRECATED))
                : false;
    }

}
