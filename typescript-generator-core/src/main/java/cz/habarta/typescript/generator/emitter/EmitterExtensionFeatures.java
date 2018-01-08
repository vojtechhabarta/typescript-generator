
package cz.habarta.typescript.generator.emitter;

import java.util.Map;


public class EmitterExtensionFeatures {

    // declared abilities
    public boolean generatesRuntimeCode = false;
    public boolean generatesModuleCode = false;
    public boolean worksWithPackagesMappedToNamespaces = false;
    public boolean overridesStringEnums = false;
    public boolean addsCodeToGeneratedClasses = false;

    // overridden settings
    public boolean generatesJaxrsApplicationClient = false;
    public String restResponseType = null;
    public String restOptionsType = null;
    public Map<String, String> npmPackageDependencies = null;

}
