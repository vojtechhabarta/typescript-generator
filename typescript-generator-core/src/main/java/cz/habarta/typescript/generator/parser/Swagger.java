
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.util.Utils;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;


public class Swagger {

    public static SwaggerOperation parseSwaggerAnnotations(Method method) {
        return firstResult(
                () -> parseSwaggerAnnotations3(method),
                () -> parseSwaggerAnnotations1(method),
                new SwaggerOperation());
    }

    private static SwaggerOperation parseSwaggerAnnotations1(Method method) {
        final Annotation apiOperation = Utils.getAnnotation(method, "io.swagger.annotations.ApiOperation");
        final Annotation[] apiResponses = Utils.getAnnotationElementValue(method, "io.swagger.annotations.ApiResponses", "value", Annotation[].class);
        if (apiOperation == null && apiResponses == null) {
            return null;
        }
        final SwaggerOperation swaggerOperation = new SwaggerOperation();
        // @ApiOperation
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
        // @ApiResponses
        if (apiResponses != null) {
            swaggerOperation.possibleResponses = new ArrayList<>();
            for (Annotation apiResponse : apiResponses) {
                final SwaggerResponse response = new SwaggerResponse();
                response.code = String.valueOf(Utils.getAnnotationElementValue(apiResponse, "code", Integer.class));
                response.comment = Utils.getAnnotationElementValue(apiResponse, "message", String.class);
                response.responseType = Utils.getAnnotationElementValue(apiResponse, "response", Class.class);
                swaggerOperation.possibleResponses.add(response);
            }
        }
        return swaggerOperation;
    }

    private static SwaggerOperation parseSwaggerAnnotations3(Method method) {
        final Annotation operationAnnotation = Utils.getAnnotation(method, "io.swagger.v3.oas.annotations.Operation");
        final Annotation apiResponseAnnotation = Utils.getAnnotation(method, "io.swagger.v3.oas.annotations.responses.ApiResponse");
        final Annotation apiResponsesAnnotation = Utils.getAnnotation(method, "io.swagger.v3.oas.annotations.responses.ApiResponses");
        if (operationAnnotation == null && apiResponseAnnotation == null && apiResponsesAnnotation == null) {
            return null;
        }
        final SwaggerOperation swaggerOperation = new SwaggerOperation();
        // @Operation
        if (operationAnnotation != null) {
            swaggerOperation.hidden = Utils.getAnnotationElementValue(operationAnnotation, "hidden", Boolean.class);
            swaggerOperation.comment = Utils.getAnnotationElementValue(operationAnnotation, "description", String.class);
            swaggerOperation.comment = swaggerOperation.comment.isEmpty() ? null : swaggerOperation.comment;
        }
        // @ApiResponses
        final List<Annotation> responses = firstResult(
                () -> emptyToNull(Utils.getRepeatableAnnotation(apiResponseAnnotation, apiResponsesAnnotation)),
                () -> emptyToNull(Arrays.asList(Utils.getAnnotationElementValue(operationAnnotation, "responses", Annotation[].class)))
        );
        if (responses != null) {
            swaggerOperation.possibleResponses = new ArrayList<>();
            for (Annotation apiResponse : responses) {
                final SwaggerResponse response = new SwaggerResponse();
                final String code = Utils.getAnnotationElementValue(apiResponse, "responseCode", String.class);
                response.code = Objects.equals(code, "default") ? null : code;
                response.comment = Utils.getAnnotationElementValue(apiResponse, "description", String.class);
                final Annotation[] content = Utils.getAnnotationElementValue(apiResponse, "content", Annotation[].class);
                if (content.length > 0) {
                    final Annotation schema = Utils.getAnnotationElementValue(content[0], "schema", Annotation.class);
                    final Class<?> implementation = Utils.getAnnotationElementValue(schema, "implementation", Class.class);
                    if (!Objects.equals(implementation, Void.class)) {
                        response.responseType = implementation;
                        if (swaggerOperation.responseType == null) {
                            if (response.code == null || isSuccessCode(response.code)) {
                                swaggerOperation.responseType = implementation;
                            }
                        }
                    }
                }
                if (response.code != null) {
                    swaggerOperation.possibleResponses.add(response);
                }
            }
        }
        return swaggerOperation;
    }

    private static boolean isSuccessCode(String code) {
        return code.startsWith("2");
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
        final String comment = firstResult(
                () -> Utils.getAnnotationElementValue(bean.getOrigin(), "io.swagger.v3.oas.annotations.media.Schema", "description", String.class),
                () -> Utils.getAnnotationElementValue(bean.getOrigin(), "io.swagger.annotations.ApiModel", "description", String.class));
        final List<String> comments = comment != null && !comment.isEmpty() ? Arrays.asList(comment) : null;
        return bean.withProperties(enrichedProperties).withComments(Utils.concat(comments, bean.getComments()));
    }

    private static PropertyModel enrichProperty(PropertyModel property) {
        if (property.getOriginalMember() instanceof AnnotatedElement) {
            final AnnotatedElement annotatedElement = (AnnotatedElement) property.getOriginalMember();
            return firstResult(
                    () -> enrichProperty3(property, annotatedElement),
                    () -> enrichProperty1(property, annotatedElement),
                    property);
        } else {
            return property;
        }
    }

    private static PropertyModel enrichProperty1(PropertyModel property, AnnotatedElement annotatedElement) {
        final Annotation apiModelProperty = Utils.getAnnotation(annotatedElement, "io.swagger.annotations.ApiModelProperty");
        if (apiModelProperty == null) {
            return null;
        }
        final String comment = Utils.getAnnotationElementValue(apiModelProperty, "value", String.class);
        final List<String> comments = comment != null && !comment.isEmpty() ? Arrays.asList(comment) : null;
        final PropertyModel propertyModel = property.withComments(Utils.concat(comments, property.getComments()));
        final String dataTypeString = Utils.getAnnotationElementValue(apiModelProperty, "dataType", String.class);
        if (dataTypeString == null || dataTypeString.isEmpty()) {
            return propertyModel;
        }
        try {
            final Type type = Class.forName(dataTypeString);
            final boolean required = Utils.getAnnotationElementValue(apiModelProperty, "required", Boolean.class);
            return propertyModel.withType(type).withOptional(!required);
        } catch (ClassNotFoundException | ClassCastException e) {
            return propertyModel;
        }
    }

    private static PropertyModel enrichProperty3(PropertyModel property, AnnotatedElement annotatedElement) {
        final Annotation schema = Utils.getAnnotation(annotatedElement, "io.swagger.v3.oas.annotations.media.Schema");
        if (schema == null) {
            return null;
        }
        final String comment = Utils.getAnnotationElementValue(schema, "description", String.class);
        final List<String> comments = comment != null && !comment.isEmpty() ? Arrays.asList(comment) : null;
        final PropertyModel propertyModel = property.withComments(Utils.concat(comments, property.getComments()));
        final Class<?> implementation = Utils.getAnnotationElementValue(schema, "implementation", Class.class);
        if (implementation == null || Objects.equals(implementation, Void.class)) {
            return propertyModel;
        }
        final boolean required = Utils.getAnnotationElementValue(schema, "required", Boolean.class);
        return propertyModel.withType(implementation).withOptional(!required);
    }

    private static <T> T firstResult(Supplier<? extends T> supplier1, Supplier<? extends T> supplier2) {
        return firstResult(supplier1, supplier2, null);
    }

    private static <T> T firstResult(Supplier<? extends T> supplier1, Supplier<? extends T> supplier2, T defaultResult) {
        final T result1 = supplier1.get();
        if (result1 != null) {
            return result1;
        }
        final T result2 = supplier2.get();
        if (result2 != null) {
            return result2;
        }
        return defaultResult;
    }

    private static <T> List<T> emptyToNull(List<T> list) {
        return list != null && !list.isEmpty() ? list : null;
    }

}
