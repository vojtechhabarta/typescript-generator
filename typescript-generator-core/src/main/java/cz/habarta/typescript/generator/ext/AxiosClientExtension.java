
package cz.habarta.typescript.generator.ext;

import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import java.util.*;


public class AxiosClientExtension extends AbstractClientExtension {

    @Override
    public EmitterExtensionFeatures getFeatures() {
        final EmitterExtensionFeatures features = new EmitterExtensionFeatures();
        features.generatesRuntimeCode = true;
        features.generatesModuleCode = true;
        features.generatesJaxrsApplicationClient = true;
        features.restResponseType = "Axios.Promise<Axios.GenericAxiosResponse<R>>";
        features.restOptionsType = "Axios.AxiosRequestConfig";
        return features;
    }

    @Override
    protected void emitClient(Writer writer, Settings settings, boolean exportKeyword, String appName) {
        final Map<String, String> replacements = new LinkedHashMap<>();
        replacements.put("\"", settings.quotes);
        replacements.put("/*export*/ ", exportKeyword ? "export " : "");
        replacements.put("$$RestApplicationClient$$", appName);
        replacements.put("$$AxiosRestApplicationClient$$", "Axios" + appName);
        emitTemplate(writer, settings, "AxiosClientExtension.template.ts", replacements);
    }

}
