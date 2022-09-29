
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.compiler.TsModelTransformer;
import cz.habarta.typescript.generator.emitter.Emitter;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.emitter.TsBeanModel;
import cz.habarta.typescript.generator.emitter.TsBooleanLiteral;
import cz.habarta.typescript.generator.emitter.TsDecorator;
import cz.habarta.typescript.generator.emitter.TsIdentifierReference;
import cz.habarta.typescript.generator.emitter.TsMethodModel;
import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.emitter.TsPropertyModel;
import cz.habarta.typescript.generator.emitter.TsStringLiteral;
import cz.habarta.typescript.generator.parser.Model;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;


public class DecoratorsTest {

    @Test
    public void testDecoratorOnClassAndProperty() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.outputKind = TypeScriptOutputKind.module;
        settings.mapClasses = ClassMapping.asClasses;
        settings.importDeclarations.add("import { JsonObject, JsonProperty } from \"json2typescript\"");
        settings.extensions.add(new ClassNameDecoratorExtension());
        settings.optionalProperties = OptionalProperties.useLibraryDefinition;
        final String output = new TypeScriptGenerator(settings).generateTypeScript(Input.from(City.class));
        Assertions.assertTrue(output.contains("@JsonObject(\"City\")"));
        Assertions.assertTrue(output.contains("@JsonProperty(\"name\", String)"));
    }

    private static class ClassNameDecoratorExtension extends Extension {

        @Override
        public EmitterExtensionFeatures getFeatures() {
            final EmitterExtensionFeatures features = new EmitterExtensionFeatures();
            features.generatesRuntimeCode = true;
            return features;
        }

        @Override
        public List<TransformerDefinition> getTransformers() {
            return Arrays.asList(
                    new TransformerDefinition(ModelCompiler.TransformationPhase.BeforeEnums, new TsModelTransformer() {
                        @Override
                        public TsModel transformModel(Context context, TsModel model) {
                            return model.withBeans(model.getBeans().stream()
                                    .map(ClassNameDecoratorExtension.this::decorateClass)
                                    .collect(Collectors.toList())
                            );
                        }
                    })
            );
        }

        private TsBeanModel decorateClass(TsBeanModel bean) {
            if (!bean.isClass()) {
                return bean;
            }
            return bean
                    .withDecorators(Arrays.asList(new TsDecorator(
                            new TsIdentifierReference("JsonObject"),
                            Arrays.asList(new TsStringLiteral(bean.getOrigin().getSimpleName()))
                    )))
                    .withProperties(bean.getProperties().stream()
                            .map(ClassNameDecoratorExtension.this::decorateProperty)
                            .collect(Collectors.toList())
                    );
        }

        private TsPropertyModel decorateProperty(TsPropertyModel property) {
            return property
                    .withDecorators(Arrays.asList(new TsDecorator(
                            new TsIdentifierReference("JsonProperty"),
                            Arrays.asList(
                                    new TsStringLiteral(property.getName()),
                                    new TsIdentifierReference("String")
                            )
                    )));
        }

    }

    public static class City {
        public String name;
    }

    @Test
    public void testDecoratorsOnParameterAndMethod() {
        final Settings settings = TestUtils.settings();
        settings.outputFileType = TypeScriptFileType.implementationFile;
        settings.outputKind = TypeScriptOutputKind.module;
        settings.mapClasses = ClassMapping.asClasses;
        settings.generateConstructors = true;
        final TypeScriptGenerator typeScriptGenerator = new TypeScriptGenerator(settings);
        final Model model = typeScriptGenerator.getModelParser().parseModel(City.class);
        final TsModel tsModel = typeScriptGenerator.getModelCompiler().javaToTypeScript(model);
        final TsBeanModel bean = tsModel.getBean(City.class);
        final TsBeanModel bean2 = bean
                .withConstructor(bean.getConstructor()
                        .withParameters(Arrays.asList(bean.getConstructor().getParameters().get(0)
                                .withDecorators(Arrays.asList(new TsDecorator(
                                        new TsIdentifierReference("Inject"),
                                        Arrays.asList(new TsStringLiteral("token"))
                                )))
                        ))
                )
                .withMethods(Arrays.asList(new TsMethodModel("greet", null, null, Collections.emptyList(), TsType.Void, Collections.emptyList(), null)
                        .withDecorators(Arrays.asList(new TsDecorator(
                                new TsIdentifierReference("enumerable"),
                                Arrays.asList(new TsBooleanLiteral(false))
                        )))
                ));
        final TsModel tsModel2 = tsModel.withBeans(Arrays.asList(bean2));
        final String output = emit(typeScriptGenerator.getEmitter(), tsModel2);
        Assertions.assertTrue(output.contains("@Inject(\"token\")"));
        Assertions.assertTrue(output.contains("@enumerable(false)"));
    }

    private static String emit(Emitter emitter, TsModel model) {
        final StringWriter writer = new StringWriter();
        emitter.emit(model, writer, "test", true);
        return writer.toString();
    }

}
