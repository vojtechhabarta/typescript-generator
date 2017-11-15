
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.compiler.ModelTransformer;
import cz.habarta.typescript.generator.emitter.EmitterExtension;
import java.util.*;


public abstract class Extension extends EmitterExtension {

    public void setConfiguration(Map<String, String> configuration) throws RuntimeException {
    }

    public List<TransformerDefinition> getTransformers() {
        return Collections.emptyList();
    }

    public static class TransformerDefinition {
        public final ModelCompiler.TransformationPhase phase;
        public final ModelTransformer transformer;

        public TransformerDefinition(ModelCompiler.TransformationPhase phase, ModelTransformer transformer) {
            this.phase = phase;
            this.transformer = transformer;
        }
    }

}
