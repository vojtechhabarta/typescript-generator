
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.compiler.ModelTransformer;
import cz.habarta.typescript.generator.compiler.TsModelTransformer;
import cz.habarta.typescript.generator.emitter.EmitterExtension;
import java.util.Collections;
import java.util.List;
import java.util.Map;


public abstract class Extension extends EmitterExtension {

    public void setConfiguration(Map<String, String> configuration) throws RuntimeException {
    }

    public List<TransformerDefinition> getTransformers() {
        return Collections.emptyList();
    }

    public static class TransformerDefinition {
        public final ModelCompiler.TransformationPhase phase;
        public final ModelTransformer transformer;
        public final TsModelTransformer tsTransformer;

        public TransformerDefinition(ModelCompiler.TransformationPhase phase, ModelTransformer transformer) {
            if (phase != ModelCompiler.TransformationPhase.BeforeTsModel) {
                throw new IllegalArgumentException("ModelTransformer can only be applied in phase 'BeforeTsModel'");
            }
            this.phase = phase;
            this.transformer = transformer;
            this.tsTransformer = null;
        }

        public TransformerDefinition(ModelCompiler.TransformationPhase phase, TsModelTransformer transformer) {
            if (phase == ModelCompiler.TransformationPhase.BeforeTsModel) {
                throw new IllegalArgumentException("TsModelTransformer cannot be applied in phase 'BeforeTsModel'");
            }
            this.phase = phase;
            this.transformer = null;
            this.tsTransformer = transformer;
        }

    }

}
