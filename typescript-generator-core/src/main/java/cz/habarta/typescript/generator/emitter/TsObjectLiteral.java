
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.util.Utils;
import java.util.*;


public class TsObjectLiteral extends TsExpression {

    private final List<TsPropertyDefinition> propertyDefinitions;

    public TsObjectLiteral(TsPropertyDefinition... propertyDefinitions) {
        this(removeNulls(Arrays.asList(propertyDefinitions)));
    }

    private static List<TsPropertyDefinition> removeNulls(List<TsPropertyDefinition> propertyDefinitions) {
        final ArrayList<TsPropertyDefinition> props = new ArrayList<>(propertyDefinitions);
        props.removeAll(Collections.singleton(null));
        return props;
    }

    public TsObjectLiteral(List<TsPropertyDefinition> propertyDefinitions) {
        this.propertyDefinitions = propertyDefinitions;
    }

    public List<TsPropertyDefinition> getPropertyDefinitions() {
        return propertyDefinitions;
    }

    @Override
    public String format(Settings settings) {
        final List<String> props = new ArrayList<>();
        for (TsPropertyDefinition property : propertyDefinitions) {
            props.add(property.format(settings));
        }
        if (props.isEmpty()) {
            return "{}";
        } else {
            return "{ " + Utils.join(props, ", ") + " }";
        }
        
    }

}
