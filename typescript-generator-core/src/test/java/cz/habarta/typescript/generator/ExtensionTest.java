package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.compiler.ModelCompiler.TransformationPhase;
import cz.habarta.typescript.generator.compiler.ModelTransformer;
import cz.habarta.typescript.generator.compiler.SymbolTable;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.parser.BeanModel;
import cz.habarta.typescript.generator.parser.Jackson2Parser;
import cz.habarta.typescript.generator.parser.Model;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.hamcrest.CoreMatchers;
import org.junit.Assert;
import org.junit.Test;

public class ExtensionTest {

    @Test
    public void testBeforeTsExtension() throws Exception {
        final Settings settings = TestUtils.settings();

        settings.extensions.add(new Extension() {

            @Override
            public EmitterExtensionFeatures getFeatures() {
                return new EmitterExtensionFeatures();
            }

            @Override
            public List<TransformerDefinition> getTransformers() {
                return Collections.singletonList(new TransformerDefinition(TransformationPhase.BeforeTsModel, new ModelTransformer() {

                    @Override
                    public TsModel transformModel(SymbolTable symbolTable, TsModel model) {
                        return model;
                    }

                    @Override
                    public Model transformModel(SymbolTable symbolTable, Model model) {
                        List<BeanModel> beans = new ArrayList<>(model.getBeans());

                        BeanModel implementationBean = model.getBean(Implementation.class);
                        BeanModel beanWithComments = implementationBean.withComments(Collections.singletonList("My new comment"));

                        beans.remove(implementationBean);
                        beans.add(beanWithComments);

                        return new Model(beans, model.getEnums(), model.getRestApplications());
                    }
                }));
            }
        });

        final Jackson2Parser jacksonParser = new Jackson2Parser(settings, new DefaultTypeProcessor());
        final Model model = jacksonParser.parseModel(Implementation.class);
        final ModelCompiler modelCompiler = new TypeScriptGenerator(settings).getModelCompiler();

        final TsModel result = modelCompiler.javaToTypeScript(model);

        Assert.assertEquals(1, result.getBean(Implementation.class).getComments().size());
        Assert.assertThat(result.getBean(Implementation.class).getComments().get(0), CoreMatchers.containsString("My new comment"));
    }

    private static class Implementation { }

}
