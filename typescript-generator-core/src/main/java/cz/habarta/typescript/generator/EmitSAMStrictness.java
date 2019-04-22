/*
 * Copyright (C) 2010-2019 Evergage, Inc.
 * All rights reserved.
 */

package cz.habarta.typescript.generator;

public enum EmitSAMStrictness {

    /**
     * Emit all single abstract method classes as function signatures
     */
    byClassDefinitionOnly,
    /**
     * Emit only single abstract methods classes marked with @FunctionalInterface as function signatures
     */
    byClassDefinitionAndAnnotation,
    /**
     * Emit no single abstract method classes as function signatures
     */
    noEmitSAM

}
