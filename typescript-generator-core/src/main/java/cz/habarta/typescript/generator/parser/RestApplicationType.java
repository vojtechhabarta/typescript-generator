
package cz.habarta.typescript.generator.parser;

import cz.habarta.typescript.generator.Settings;
import java.util.function.Function;


public enum RestApplicationType {

    Jaxrs(settings -> settings.generateJaxrsApplicationInterface, settings -> settings.generateJaxrsApplicationClient),
    Spring(settings -> settings.generateSpringApplicationInterface, settings -> settings.generateSpringApplicationClient);

    private RestApplicationType(Function<Settings, Boolean> generateInterface, Function<Settings, Boolean> generateClient) {
        this.generateInterface = generateInterface;
        this.generateClient = generateClient;
    }

    public final Function<Settings, Boolean> generateInterface;
    public final Function<Settings, Boolean> generateClient;

}
