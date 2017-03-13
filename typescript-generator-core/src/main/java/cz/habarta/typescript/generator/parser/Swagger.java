
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.util.Utils;
import java.lang.reflect.Method;
import java.util.*;


public class Swagger {

    static SwaggerOperation parseSwaggerAnnotations(Method method) {
        final SwaggerOperation swaggerOperation = new SwaggerOperation();
        // @ApiOperation
        {
            final Object apiOperation = Utils.getAnnotation(method, "io.swagger.annotations.ApiOperation");
            if (apiOperation != null) {
                final Class<?> response = Utils.getAnnotationElementValue(apiOperation, "response", Class.class);
                final String responseContainer = Utils.getAnnotationElementValue(apiOperation, "responseContainer", String.class);
                if (responseContainer == null || responseContainer.isEmpty()) {
                    swaggerOperation.response = response;
                } else {
                    switch (responseContainer) {
                        case "List":
                            swaggerOperation.response = Utils.createParameterizedType(List.class, response);
                            break;
                        case "Set":
                            swaggerOperation.response = Utils.createParameterizedType(Set.class, response);
                            break;
                        case "Map":
                            swaggerOperation.response = Utils.createParameterizedType(Map.class, String.class, response);
                            break;
                    }
                }
                swaggerOperation.hidden = Utils.getAnnotationElementValue(apiOperation, "hidden", Boolean.class);
            }
        }
        // @ApiResponses
        {
            final Object[] apiResponses = Utils.getAnnotationElementValue(method, "io.swagger.annotations.ApiResponses", "value", Object[].class);
            if (apiResponses != null) {
                swaggerOperation.possibleResponses = new ArrayList<>();
                for (Object apiResponse : apiResponses) {
                    swaggerOperation.possibleResponses.add(Utils.getAnnotationElementValue(apiResponse, "response", Class.class));
                }
            }
        }
        return swaggerOperation;
    }

}
