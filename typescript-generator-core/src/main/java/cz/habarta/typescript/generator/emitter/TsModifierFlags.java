
package cz.habarta.typescript.generator.emitter;


public class TsModifierFlags {

    public static final TsModifierFlags None = new TsModifierFlags(false, false);

    public final boolean isStatic;
    public final boolean isReadonly;

    private TsModifierFlags(boolean isStatic, boolean isReadonly) {
        this.isStatic = isStatic;
        this.isReadonly = isReadonly;
    }

    public TsModifierFlags setStatic() {
        return new TsModifierFlags(true, isReadonly);
    }

    public TsModifierFlags setStatic(boolean isStatic) {
        return new TsModifierFlags(isStatic, isReadonly);
    }

    public TsModifierFlags setReadonly() {
        return new TsModifierFlags(isStatic, true);
    }

    public TsModifierFlags setReadonly(boolean isReadonly) {
        return new TsModifierFlags(isStatic, isReadonly);
    }

}
