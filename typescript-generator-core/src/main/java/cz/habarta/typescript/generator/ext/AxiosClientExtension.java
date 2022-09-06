
package cz.habarta.typescript.generator.ext;

import cz.habarta.typescript.generator.Extension;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.emitter.Emitter;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.emitter.TsBeanModel;
import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.util.Utils;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AxiosClientExtension extends Extension {

    public static final String CFG_AXIOS_VERSION = "axiosVersion";

    private String axiosVersion = "0.21.1";

    @Override
    public void setConfiguration(Map<String, String> configuration) throws RuntimeException {
        if (configuration.containsKey(CFG_AXIOS_VERSION)) {
            this.axiosVersion = configuration.get(CFG_AXIOS_VERSION);
        }
    }

    @Override
    public EmitterExtensionFeatures getFeatures() {
        final EmitterExtensionFeatures features = new EmitterExtensionFeatures();
        features.generatesRuntimeCode = true;
        features.generatesModuleCode = true;
        features.worksWithPackagesMappedToNamespaces = true;
        features.generatesJaxrsApplicationClient = true;
        features.restResponseType = "Promise<Axios.GenericAxiosResponse<R>>";
        features.restOptionsType = "<O>";
        features.npmPackageDependencies = Collections.singletonMap("axios", axiosVersion);
        return features;
    }

    @Override
    public void emitElements(Writer writer, Settings settings, boolean exportKeyword, TsModel model) {
        emitSharedPart(writer, settings);
        for (TsBeanModel bean : model.getBeans()) {
            if (bean.isJaxrsApplicationClientBean()) {
                final String clientName = bean.getName().getSimpleName();
                final String clientFullName = settings.mapPackagesToNamespaces ? bean.getName().getFullName(): bean.getName().getSimpleName();
                emitClient(writer, settings, exportKeyword, clientName, clientFullName);
            }
        }
    }

    private void emitSharedPart(Writer writer, Settings settings) {
        final List<String> template = Utils.readLines(getClass().getResourceAsStream("AxiosClientExtension-shared.template.ts"));
        Emitter.writeTemplate(writer, settings, template, null);
    }

    private void emitClient(Writer writer, Settings settings, boolean exportKeyword, String clientName, String clientFullName) {
        final List<String> template = Utils.readLines(getClass().getResourceAsStream("AxiosClientExtension-client.template.ts"));
        final Map<String, String> replacements = new LinkedHashMap<>();
        replacements.put("\"", settings.quotes);
        replacements.put("/*export*/ ", exportKeyword ? "export " : "");
        replacements.put("$$RestApplicationClient$$", clientName);
        replacements.put("$$RestApplicationClientFullName$$", clientFullName);
        replacements.put("$$AxiosRestApplicationClient$$", "Axios" + clientName);
        Emitter.writeTemplate(writer, settings, template, replacements);
    }

}
