
package cz.habarta.typescript.generator.parser;

import java.lang.reflect.Type;
import java.util.List;


public class SwaggerOperation {
    public Type response;
    public List<Type> possibleResponses;
    public boolean hidden;
}
