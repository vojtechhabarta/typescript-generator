/*
 * Copyright (C) 2019 - present by OpenGamma Inc. and the OpenGamma group of companies
 *
 * Please see distribution for license.
 */
package cz.habarta.typescript.generator;

public enum CustomMappingScope {

    /**
     * A mapping of a concrete class
     */
    CLASS,

    /**
     * A mapping of a super type, such as an abstract class or interface.
     */
    SUPERTYPE,
}
