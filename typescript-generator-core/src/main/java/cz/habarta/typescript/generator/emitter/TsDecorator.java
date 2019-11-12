
package cz.habarta.typescript.generator.emitter;

import java.util.List;
import java.util.Objects;


public class TsDecorator {

    private final TsIdentifierReference identifierReference;
    private final List<TsExpression> arguments;

    public TsDecorator(TsIdentifierReference identifierReference, List<TsExpression> arguments) {
        this.identifierReference = Objects.requireNonNull(identifierReference);
        this.arguments = arguments;
    }

    public TsIdentifierReference getIdentifierReference() {
        return identifierReference;
    }

    public List<TsExpression> getArguments() {
        return arguments;
    }

}
