
package cz.habarta.typescript.generator;

import java.io.Serializable;


/**
 * This class is used for configuration in Maven and Gradle plugins
 * so we need to pay attention to use only types supported in both build plugins.
 */
public class JsonbConfiguration implements Serializable {

    /**
     * {@link jakarta.json.bind.config.PropertyNamingStrategy} name.
     */
    public String namingStrategy = "IDENTITY";
}
