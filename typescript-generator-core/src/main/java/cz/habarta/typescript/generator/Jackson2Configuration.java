
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonAutoDetect;


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


    public void setVisibility(
            JsonAutoDetect.Visibility fieldVisibility,
            JsonAutoDetect.Visibility getterVisibility,
            JsonAutoDetect.Visibility isGetterVisibility,
            JsonAutoDetect.Visibility setterVisibility,
            JsonAutoDetect.Visibility creatorVisibility) {
        this.fieldVisibility = fieldVisibility;
        this.getterVisibility = getterVisibility;
        this.isGetterVisibility = isGetterVisibility;
        this.setterVisibility = setterVisibility;
        this.creatorVisibility = creatorVisibility;
    }

}
