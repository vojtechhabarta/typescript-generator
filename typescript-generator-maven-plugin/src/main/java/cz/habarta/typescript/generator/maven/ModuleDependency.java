
package cz.habarta.typescript.generator.maven;

import cz.habarta.typescript.generator.util.Utils;
import java.io.File;
import org.jspecify.annotations.Nullable;

/**
 * A simple data class representing a module dependency for the TypeScript
 * generator Maven plugin. It decouples the ModuleDependency class from the core 
 * TypeScript generator library, allowing foreasier maintenance and potential future
 * changes in the core library without being limited by the Maven plugin's requirements.
 */
public class ModuleDependency {
	@SuppressWarnings("NullAway.Init")
    public String importFrom;
	@SuppressWarnings("NullAway.Init")
    public String importAs;
	@SuppressWarnings("NullAway.Init")
    public File infoJson;
    public @Nullable String npmPackageName;
    public @Nullable String npmVersionRange;

    @Override
    public String toString() {
        return Utils.objectToString(this);
    }

}
