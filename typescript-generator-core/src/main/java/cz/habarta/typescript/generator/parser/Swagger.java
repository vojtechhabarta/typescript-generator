
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.util.Utils;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.*;


public class Swagger {

    static SwaggerOperation parseSwaggerAnnotations(Method method) {
        final SwaggerOperation swaggerOperation = new SwaggerOperation();
        // @ApiOperation
        {
            final Annotation apiOperation = Utils.getAnnotation(method, "io.swagger.annotations.ApiOperation");
            if (apiOperation != null) {
                final Class<?> response = Utils.getAnnotationElementValue(apiOperation, "response", Class.class);
                final String responseContainer = Utils.getAnnotationElementValue(apiOperation, "responseContainer", String.class);
                if (responseContainer == null || responseContainer.isEmpty()) {
                    swaggerOperation.responseType = response;
                } else {
                    switch (responseContainer) {
                        case "List":
                            swaggerOperation.responseType = Utils.createParameterizedType(List.class, response);
                            break;
                        case "Set":
                            swaggerOperation.responseType = Utils.createParameterizedType(Set.class, response);
                            break;
                        case "Map":
                            swaggerOperation.responseType = Utils.createParameterizedType(Map.class, String.class, response);
                            break;
                    }
                }
                swaggerOperation.hidden = Utils.getAnnotationElementValue(apiOperation, "hidden", Boolean.class);
                swaggerOperation.comment = Utils.getAnnotationElementValue(apiOperation, "value", String.class);
                swaggerOperation.comment = swaggerOperation.comment.isEmpty() ? null : swaggerOperation.comment;
            }
        }
        // @ApiResponses
        {
            final Annotation[] apiResponses = Utils.getAnnotationElementValue(method, "io.swagger.annotations.ApiResponses", "value", Annotation[].class);
            if (apiResponses != null) {
                swaggerOperation.possibleResponses = new ArrayList<>();
                for (Annotation apiResponse : apiResponses) {
                    final SwaggerResponse response = new SwaggerResponse();
                    response.code = Utils.getAnnotationElementValue(apiResponse, "code", Integer.class);
                    response.comment = Utils.getAnnotationElementValue(apiResponse, "message", String.class);
                    response.responseType = Utils.getAnnotationElementValue(apiResponse, "response", Class.class);
                    swaggerOperation.possibleResponses.add(response);
                }
            }
        }
        return swaggerOperation;
    }

    static List<String> getOperationComments(SwaggerOperation operation) {
        final List<String> comments = new ArrayList<>();
        if (operation.comment != null) {
            comments.add(operation.comment);
        }
        if (operation.possibleResponses != null) {
            for (SwaggerResponse response : operation.possibleResponses) {
                comments.add(String.format("Response code %s - %s", response.code, response.comment));
            }
        }
        return comments.isEmpty() ? null : comments;
    }

    public static Model enrichModel(Model model) {
        final List<BeanModel> dBeans = new ArrayList<>();
        for (BeanModel bean : model.getBeans()) {
            final BeanModel dBean = enrichBean(bean);
            dBeans.add(dBean);
        }
        return new Model(dBeans, model.getEnums(), model.getRestApplications());
    }

    private static BeanModel enrichBean(BeanModel bean) {
        final List<PropertyModel> enrichedProperties = new ArrayList<>();
        for (PropertyModel property : bean.getProperties()) {
            final PropertyModel enrichedProperty = enrichProperty(property);
            enrichedProperties.add(enrichedProperty);
        }
        final String comment = Utils.getAnnotationElementValue(bean.getOrigin(), "io.swagger.annotations.ApiModel", "description", String.class);
        final List<String> comments = comment != null && !comment.isEmpty() ? Arrays.asList(comment) : null;
        return bean.withProperties(enrichedProperties).withComments(Utils.concat(comments, bean.getComments()));
    }

    private static PropertyModel enrichProperty(PropertyModel property) {
        if (property.getOriginalMember() instanceof AnnotatedElement) {
            final AnnotatedElement annotatedElement = (AnnotatedElement) property.getOriginalMember();
            final String comment = Utils.getAnnotationElementValue(annotatedElement, "io.swagger.annotations.ApiModelProperty", "value", String.class);
            final List<String> comments = comment != null && !comment.isEmpty() ? Arrays.asList(comment) : null;
            final PropertyModel propertyModel = property.withComments(Utils.concat(comments, property.getComments()));
            final String dataTypeString = Utils.getAnnotationElementValue(annotatedElement, "io.swagger.annotations.ApiModelProperty", "dataType", String.class);
            if (dataTypeString == null || dataTypeString.isEmpty()) {
                return propertyModel;
            }
            try {
                final Type type = Class.forName(dataTypeString);
                final boolean required = Utils.getAnnotationElementValue(annotatedElement, "io.swagger.annotations.ApiModelProperty", "required", Boolean.class);
                return propertyModel.withType(type).withOptional(!required);
            } catch (ClassNotFoundException | ClassCastException e) {
                return propertyModel;
            }
        } else {
            return property;
        }
    }

}
