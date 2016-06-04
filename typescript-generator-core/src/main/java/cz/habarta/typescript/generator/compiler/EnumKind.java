
package cz.habarta.typescript.generator.compiler;


public final class EnumKind<T> {

    public static final EnumKind<String> StringBased = new EnumKind<>();
    public static final EnumKind<Number> NumberBased = new EnumKind<>();

    private EnumKind() {
    }

}
