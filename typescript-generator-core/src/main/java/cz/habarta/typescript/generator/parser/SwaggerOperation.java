
package cz.habarta.typescript.generator.parser;

import java.lang.reflect.Type;
import java.util.List;


public class SwaggerOperation {
    public Type responseType;
    public List<SwaggerResponse> possibleResponses;
    public boolean hidden;
    public String comment;
}
