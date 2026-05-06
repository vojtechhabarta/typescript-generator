
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.TsType;
import java.util.Objects;
import org.jspecify.annotations.Nullable;


public class TsVariableDeclarationStatement extends TsStatement {

    private final boolean isConst;
    private final String name;
    private final @Nullable TsType type;
    private final TsExpression initializer;

    public TsVariableDeclarationStatement(boolean isConst, String name, @Nullable TsType type, TsExpression initializer) {
        Objects.requireNonNull(name);
        this.isConst = isConst;
        this.name = name;
        this.type = type;
        this.initializer = initializer;
    }

    public boolean isConst() {
        return isConst;
    }

    public String getName() {
        return name;
    }

    public @Nullable TsType getType() {
        return type;
    }

    public TsExpression getInitializer() {
        return initializer;
    }

}
