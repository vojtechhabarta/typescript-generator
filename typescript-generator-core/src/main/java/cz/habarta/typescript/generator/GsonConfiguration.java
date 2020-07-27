
package cz.habarta.typescript.generator;


/**
 * This class is used for configuration in Maven and Gradle plugins so we need
 * to pay attention to use only types supported in both build plugins.
 */
public class GsonConfiguration {

    /**
     * Excludes all class fields that have the specified modifiers.
     * Modifiers are separated with <code>|</code> character.<br>
     * Field exclusion modifiers are <code>public | protected | private | static | final | transient | volatile</code>.<br>
     * Default value is <code>static | transient</code> (the same as in Gson itself).
     * Note: single charater <code>|</code> can be used to pass empty list of modifiers
     * (in Maven empty string is interpreted as <code>null</code> which means "not set").
     */
    public String excludeFieldsWithModifiers;

}
