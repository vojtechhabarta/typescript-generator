package cz.habarta.typescript.generator;

public enum EmitSAMStrictness {

    /**
     * Emit all single abstract method classes as function signatures
     */
    anyValidSAM,
    /**
     * Emit only single abstract methods classes marked with @FunctionalInterface as function signatures
     */
    byAnnotationOnly,
    /**
     * Emit no single abstract method classes as function signatures
     */
    noEmitSAM

}
