
package cz.habarta.typescript.generator.emitter;

import java.util.Map;
import org.jspecify.annotations.Nullable;


public class EmitterExtensionFeatures {

    // declared abilities
    public boolean generatesRuntimeCode = false;
    public boolean generatesModuleCode = false;
    public boolean worksWithPackagesMappedToNamespaces = false;
    public boolean overridesStringEnums = false;

    // overridden settings
    public boolean generatesJaxrsApplicationClient = false;
    public @Nullable String restResponseType = null;
    public @Nullable String restOptionsType = null;
    public @Nullable Map<String, String> npmPackageDependencies = null;
    public @Nullable Map<String, String> npmDevDependencies = null;
    public @Nullable Map<String, String> npmPeerDependencies = null;

}
