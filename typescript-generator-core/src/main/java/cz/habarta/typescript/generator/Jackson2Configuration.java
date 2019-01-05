
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import java.util.List;


/**
 * This class is used for configuration in Maven and Gradle plugins
 * so we need to pay attention to use only types supported in both build plugins.
 */
public class Jackson2Configuration {

    /**
     * Minimum visibility required for fields to be auto-detected.
     */
    public JsonAutoDetect.Visibility fieldVisibility;

    /**
     * Minimum visibility required for getters to be auto-detected (doesn't include "is getters").
     */
    public JsonAutoDetect.Visibility getterVisibility;

    /**
     * Minimum visibility required for "is getters" to be auto-detected.
     */
    public JsonAutoDetect.Visibility isGetterVisibility;

    /**
     * Minimum visibility required for setters to be auto-detected.
     */
    public JsonAutoDetect.Visibility setterVisibility;

    /**
     * Minimum visibility required for creators to be auto-detected.
     */
    public JsonAutoDetect.Visibility creatorVisibility;

    /**
     * Shape format overrides for specified classes.
     * Multiple overrides can be specified, each using this format: <code>javaClassName:shape</code> 
     * where shape is one of the values from
     * <a href="https://github.com/FasterXML/jackson-annotations/blob/master/src/main/java/com/fasterxml/jackson/annotation/JsonFormat.java">JsonFormat.Shape</a> enum.
     * Example: <code>java.util.Map$Entry:OBJECT</code>
     */
    public List<String> shapeConfigOverrides;

    /**
     * Feature that determines standard Enum values representation:
     * if enabled, return value of <code>Enum.toString()</code> is used;
     * if disabled, return value of <code>Enum.name()</code> is used.<br>
     * (In <code>ObjectMapper</code> this feature is controlled using
     * <code>SerializationFeature.WRITE_ENUMS_USING_TO_STRING</code> and
     * <code>DeserializationFeature.READ_ENUMS_USING_TO_STRING</code> constants.)<br>
     * Default value is <code>false</code>.
     */
    public boolean enumsUsingToString;

}
