
package cz.habarta.typescript.generator.ext;

import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.emitter.Emitter;
import cz.habarta.typescript.generator.emitter.EmitterExtension;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.emitter.TsBeanModel;
import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.util.Utils;
import java.util.*;


public class AxiosClientExtension extends EmitterExtension {

    @Override
    public EmitterExtensionFeatures getFeatures() {
        final EmitterExtensionFeatures features = new EmitterExtensionFeatures();
        features.generatesRuntimeCode = true;
        features.generatesModuleCode = true;
        features.generatesJaxrsApplicationClient = true;
        features.restResponseType = "Promise<Axios.GenericAxiosResponse<R>>";
        features.restOptionsType = "<O>";
        features.npmPackageDependencies = Collections.singletonMap("axios", "0.16.0");
        return features;
    }

    @Override
    public void emitElements(Writer writer, Settings settings, boolean exportKeyword, TsModel model) {
        emitSharedPart(writer, settings);
        for (TsBeanModel bean : model.getBeans()) {
            if (bean.isJaxrsApplicationClientBean()) {
                final String clientName = bean.getName().toString();
                emitClient(writer, settings, exportKeyword, clientName);
            }
        }
    }

    private void emitSharedPart(Writer writer, Settings settings) {
        final List<String> template = Utils.readLines(getClass().getResourceAsStream("AxiosClientExtension-shared.template.ts"));
        Emitter.writeTemplate(writer, settings, template, null);
    }

    private void emitClient(Writer writer, Settings settings, boolean exportKeyword, String clientName) {
        final List<String> template = Utils.readLines(getClass().getResourceAsStream("AxiosClientExtension-client.template.ts"));
        final Map<String, String> replacements = new LinkedHashMap<>();
        replacements.put("\"", settings.quotes);
        replacements.put("/*export*/ ", exportKeyword ? "export " : "");
        replacements.put("$$RestApplicationClient$$", clientName);
        replacements.put("$$AxiosRestApplicationClient$$", "Axios" + clientName);
        Emitter.writeTemplate(writer, settings, template, replacements);
    }

}
