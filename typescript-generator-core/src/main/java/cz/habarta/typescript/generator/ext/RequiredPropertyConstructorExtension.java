package cz.habarta.typescript.generator.ext;

import cz.habarta.typescript.generator.Extension;
import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.compiler.ModelTransformer;
import cz.habarta.typescript.generator.compiler.SymbolTable;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.emitter.TsAssignmentExpression;
import cz.habarta.typescript.generator.emitter.TsBeanModel;
import cz.habarta.typescript.generator.emitter.TsConstructorModel;
import cz.habarta.typescript.generator.emitter.TsExpression;
import cz.habarta.typescript.generator.emitter.TsExpressionStatement;
import cz.habarta.typescript.generator.emitter.TsIdentifierReference;
import cz.habarta.typescript.generator.emitter.TsMemberExpression;
import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.emitter.TsModifierFlags;
import cz.habarta.typescript.generator.emitter.TsParameterModel;
import cz.habarta.typescript.generator.emitter.TsPropertyModel;
import cz.habarta.typescript.generator.emitter.TsStatement;
import cz.habarta.typescript.generator.emitter.TsStringLiteral;
import cz.habarta.typescript.generator.emitter.TsThisExpression;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Adds constructor with each required property to every generated class.
 */
public class RequiredPropertyConstructorExtension extends Extension {
    @Override
    public EmitterExtensionFeatures getFeatures() {
        EmitterExtensionFeatures features = new EmitterExtensionFeatures();
        features.generatesRuntimeCode = true;
        return features;
    }

    @Override
    public List<TransformerDefinition> getTransformers() {
        return Arrays.asList(new TransformerDefinition(ModelCompiler.TransformationPhase.BeforeEnums, new ModelTransformer() {
            @Override
            public TsModel transformModel(SymbolTable symbolTable, TsModel model) {
                List<TsBeanModel> beans = new ArrayList<>();
                for (TsBeanModel bean : model.getBeans()) {
                    TsBeanModel newBean = transformBean(bean);
                    beans.add(newBean);
                }
                return model.withBeans(beans);
            }
        }));
    }

    private static TsBeanModel transformBean(TsBeanModel bean) {
        if (!bean.isClass() || bean.getConstructor() != null) {
            return bean;
        }
        Optional<TsConstructorModel> constructorOption = createConstructor(bean);
        if (!constructorOption.isPresent()) {
            return bean;
        }
        return bean.withConstructor(constructorOption.get());
    }

    private static Optional<TsConstructorModel> createConstructor(TsBeanModel bean) {
        List<TsParameterModel> parameters = new ArrayList<>();
        List<TsStatement> body = new ArrayList<>();
        for (TsPropertyModel property : bean.getProperties()) {
            if (!property.modifiers.isReadonly) {
                continue;
            }
            TsExpression assignmentExpression;
            Optional<TsExpression> predefinedValue = getPredefinedValueForProperty(property);
            if (predefinedValue.isPresent()) {
                assignmentExpression = predefinedValue.get();
            } else {
                parameters.add(new TsParameterModel(property.name, property.tsType));
                assignmentExpression = new TsIdentifierReference(property.name);
            }
            TsMemberExpression leftHandSideExpression = new TsMemberExpression(new TsThisExpression(), property.name);
            TsExpression assignment = new TsAssignmentExpression(leftHandSideExpression, assignmentExpression);
            TsExpressionStatement assignmentStatement = new TsExpressionStatement(assignment);
            body.add(assignmentStatement);
        }
        if(parameters.isEmpty() && body.isEmpty()) {
            return Optional.empty();
        }
        TsConstructorModel constructor = new TsConstructorModel(TsModifierFlags.None, parameters, body, null);
        return Optional.of(constructor);
    }

    private static Optional<TsExpression> getPredefinedValueForProperty(TsPropertyModel property) {
        if (!(property.tsType instanceof TsType.UnionType)) {
            return Optional.empty();
        }
        List<TsType> unionTypeElements = ((TsType.UnionType) property.tsType).types;
        if (unionTypeElements.size() != 1) {
            return Optional.empty();
        }
        TsType onlyElement = unionTypeElements.iterator().next();
        if (!(onlyElement instanceof TsType.StringLiteralType)) {
            return Optional.empty();
        }
        TsType.StringLiteralType onlyValue = (TsType.StringLiteralType) onlyElement;
        TsStringLiteral expression = new TsStringLiteral(onlyValue.literal);
        return Optional.of(expression);
    }

}
