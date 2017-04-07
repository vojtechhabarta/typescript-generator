
package cz.habarta.typescript.generator.emitter;

import java.util.Map;


public class EmitterExtensionFeatures {

    public boolean generatesRuntimeCode = false;
    public boolean generatesModuleCode = false;
    public boolean generatesJaxrsApplicationClient = false;
    public String restResponseType = null;
    public String restOptionsType = null;
    public Map<String, String> npmPackageDependencies = null;
    public boolean overridesStringEnums = false;

}
