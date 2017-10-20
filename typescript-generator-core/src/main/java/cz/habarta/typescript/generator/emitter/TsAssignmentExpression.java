
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.Settings;
import java.util.Objects;


public class TsAssignmentExpression extends TsExpression {

    private final TsExpression leftHandSideExpression;
    private final TsExpression assignmentExpression;

    public TsAssignmentExpression(TsExpression leftHandSideExpression, TsExpression assignmentExpression) {
        Objects.requireNonNull(leftHandSideExpression);
        Objects.requireNonNull(assignmentExpression);
        this.leftHandSideExpression = leftHandSideExpression;
        this.assignmentExpression = assignmentExpression;
    }

    public TsExpression getLeftHandSideExpression() {
        return leftHandSideExpression;
    }

    public TsExpression getAssignmentExpression() {
        return assignmentExpression;
    }

    @Override
    public String format(Settings settings) {
        return leftHandSideExpression.format(settings) + " = " + assignmentExpression.format(settings);
    }

}
