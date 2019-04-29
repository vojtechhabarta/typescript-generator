package cz.habarta.typescript.generator.ext;

import cz.habarta.typescript.generator.Extension;
import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.compiler.EnumMemberModel;
import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.compiler.ModelTransformer;
import cz.habarta.typescript.generator.compiler.Symbol;
import cz.habarta.typescript.generator.compiler.SymbolTable;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.emitter.TsAssignmentExpression;
import cz.habarta.typescript.generator.emitter.TsBeanModel;
import cz.habarta.typescript.generator.emitter.TsConstructorModel;
import cz.habarta.typescript.generator.emitter.TsEnumModel;
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
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * Adds constructor with each required property to every generated class.
 */
public class RequiredPropertyConstructorExtension extends Extension {
    static final String CFG_CLASSES = "classes";

    private List<String> classes;

    @Override
    public EmitterExtensionFeatures getFeatures() {
        EmitterExtensionFeatures features = new EmitterExtensionFeatures();
        features.generatesRuntimeCode = true;
        return features;
    }

    @Override
    public void setConfiguration(Map<String, String> configuration) throws RuntimeException {
        if (configuration.containsKey(CFG_CLASSES)) {
            classes = Arrays.asList(Pattern.compile("\\s+").split(configuration.get(CFG_CLASSES)));
        }
    }

    @Override
    public List<TransformerDefinition> getTransformers() {
        return Arrays.asList(new TransformerDefinition(ModelCompiler.TransformationPhase.BeforeSymbolResolution, new ModelTransformer() {
            @Override
            public TsModel transformModel(SymbolTable symbolTable, TsModel model) {
                List<TsBeanModel> beans = new ArrayList<>();
                for (TsBeanModel bean : model.getBeans()) {
                    TsBeanModel newBean = transformBean(bean, model);
                    beans.add(newBean);
                }
                return model.withBeans(beans);
            }
        }));
    }

    private TsBeanModel transformBean(TsBeanModel bean, TsModel model) {
        if (!bean.isClass() || bean.getConstructor() != null) {
            return bean;
        }
        Optional<TsConstructorModel> constructorOption = createConstructor(bean, model);
        if (!constructorOption.isPresent()) {
            return bean;
        }
        if (classes != null && !classes.contains(bean.getOrigin().getCanonicalName())) {
            return bean;
        }
        return bean.withConstructor(constructorOption.get());
    }

    private static Optional<TsConstructorModel> createConstructor(TsBeanModel bean, TsModel model) {
        List<TsParameterModel> parameters = new ArrayList<>();
        List<TsStatement> body = new ArrayList<>();
        if (bean.getParent() != null) {
            throw new IllegalStateException("Creating constructors for inherited beans is not currently supported");
        }
        for (TsPropertyModel property : bean.getProperties()) {
            if (!property.modifiers.isReadonly) {
                continue;
            }
            TsExpression assignmentExpression;
            Optional<TsExpression> predefinedValue = getPredefinedValueForProperty(property, model);
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

    private static Optional<TsExpression> getPredefinedValueForProperty(TsPropertyModel property, TsModel model) {
        if (property.tsType instanceof TsType.UnionType) {
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
        if (property.tsType instanceof TsType.EnumReferenceType) {
            Symbol symbol = ((TsType.EnumReferenceType) property.tsType).symbol;
            Optional<TsEnumModel> enumModelOption = model.getOriginalStringEnums().stream()
                    .filter(candidate -> candidate.getName().getFullName().equals(symbol.getFullName()))
                    .findAny();
            if (!enumModelOption.isPresent()) {
                return Optional.empty();
            }
            TsEnumModel enumModel = enumModelOption.get();
            if(enumModel.getMembers().size() != 1) {
                return Optional.empty();
            }
            EnumMemberModel singleElement = enumModel.getMembers().iterator().next();
            Object enumValue = singleElement.getEnumValue();
            TsStringLiteral expression = new TsStringLiteral((String) enumValue);
            return Optional.of(expression);
        }
        return Optional.empty();
    }

}
