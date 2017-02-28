
package cz.habarta.typescript.generator.ext;

import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.emitter.EmitterExtension;
import cz.habarta.typescript.generator.emitter.TsBeanModel;
import cz.habarta.typescript.generator.emitter.TsModel;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Pattern;
import javax.ws.rs.core.Application;


public abstract class AbstractClientExtension extends EmitterExtension {

    @Override
    public void emitElements(Writer writer, Settings settings, boolean exportKeyword, TsModel model) {
        final TsBeanModel applicationBean = getJaxrsApplicationClientBean(model);
        if (applicationBean != null) {
            final String appName = applicationBean.getName().toString();
            emitClient(writer, settings, exportKeyword, appName);
        }
    }

    private static TsBeanModel getJaxrsApplicationClientBean(TsModel model) {
        for (TsBeanModel bean : model.getBeans()) {
            if (bean.getOrigin() != null && bean.getOrigin().equals(Application.class) && bean.isClass()) {
                return bean;
            }
        }
        return null;
    }

    protected abstract void emitClient(Writer writer, Settings settings, boolean exportKeyword, String appName);

    protected void emitTemplate(Writer writer, Settings settings, String templateName, Map<String, String> replacements) {
        final List<String> template = readAllLines(getClass().getResource(templateName));
        for (String line : template) {
            for (Map.Entry<String, String> entry : replacements.entrySet()) {
                line = line.replaceAll(Pattern.quote(entry.getKey()), entry.getValue());
            }
            writer.writeIndentedLine(line);
        }
    }

    private static List<String> readAllLines(URL resource) {
        try {
            final List<String> lines = new ArrayList<>();
            final BufferedReader reader = new BufferedReader(new InputStreamReader(resource.openStream(), StandardCharsets.UTF_8));
            String line;
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            return lines;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
