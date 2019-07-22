package cz.habarta.typescript.generator.ext;

import cz.habarta.typescript.generator.Extension;
import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.compiler.EnumMemberModel;
import cz.habarta.typescript.generator.compiler.ModelCompiler;
import cz.habarta.typescript.generator.compiler.Symbol;
import cz.habarta.typescript.generator.compiler.SymbolTable;
import cz.habarta.typescript.generator.emitter.EmitterExtensionFeatures;
import cz.habarta.typescript.generator.emitter.TsAssignmentExpression;
import cz.habarta.typescript.generator.emitter.TsBeanModel;
import cz.habarta.typescript.generator.emitter.TsCallExpression;
import cz.habarta.typescript.generator.emitter.TsConstructorModel;
import cz.habarta.typescript.generator.emitter.TsEnumModel;
import cz.habarta.typescript.generator.emitter.TsExpression;
import cz.habarta.typescript.generator.emitter.TsExpressionStatement;
import cz.habarta.typescript.generator.emitter.TsMemberExpression;
import cz.habarta.typescript.generator.emitter.TsModel;
import cz.habarta.typescript.generator.emitter.TsModifierFlags;
import cz.habarta.typescript.generator.emitter.TsPropertyModel;
import cz.habarta.typescript.generator.emitter.TsStatement;
import cz.habarta.typescript.generator.emitter.TsStringLiteral;
import cz.habarta.typescript.generator.emitter.TsSuperExpression;
import cz.habarta.typescript.generator.emitter.TsThisExpression;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * The extension marks all properties which type allows only one possible value (for instance, enum with only one value
 * or {@link cz.habarta.typescript.generator.TsType.UnionType} with only one option) as read only and sets their
 * value in the constructor.<br/>
 * It may be useful while generating code for class hierarchy where each subclass has a discriminator property that can
 * only have one value. Hence, using the TypeScript code it will not be necessary to set this value manually.
 *
 * @author krzs
 */
public class OnePossiblePropertyValueAssigningExtension extends Extension {

    @Override
    public EmitterExtensionFeatures getFeatures() {
        EmitterExtensionFeatures features = new EmitterExtensionFeatures();
        features.generatesRuntimeCode = true;
        return features;
    }

    @Override
    public List<TransformerDefinition> getTransformers() {
        return Collections.singletonList(
                new TransformerDefinition(ModelCompiler.TransformationPhase.AfterDeclarationSorting,
                        OnePossiblePropertyValueAssigningExtension::transformModel)
        );
    }

    private static TsModel transformModel(SymbolTable symbolTable, TsModel model) {
        List<TsBeanModel> beans = model.getBeans().stream()
                .map(bean -> transformBean(bean, model))
                .collect(Collectors.toList());
        return model.withBeans(beans);
    }

    private static TsBeanModel transformBean(TsBeanModel bean, TsModel model) {
        if (!bean.isClass() || bean.getConstructor() != null) {
            return bean;
        }

        List<TsPropertyModel> newProperties = new ArrayList<>();
        Collection<TsExpressionStatement> valueAssignmentStatements = new ArrayList<>();

        for (TsPropertyModel property : bean.getProperties()) {
            TsPropertyModel newProperty = property;

            Optional<TsExpression> onlyValue = findOnlyValueForProperty(property, model);
            if (onlyValue.isPresent()) {
                newProperty = new TsPropertyModel(property.name, property.tsType,
                        TsModifierFlags.None.setReadonly(), property.ownProperty, property.comments);

                TsExpressionStatement assignmentStatement = createValueAssignmentStatement(newProperty, onlyValue.get());
                valueAssignmentStatements.add(assignmentStatement);
            }

            newProperties.add(newProperty);
        }

        TsBeanModel newBean = bean.withProperties(newProperties);
        if (!valueAssignmentStatements.isEmpty()) {
            TsConstructorModel constructor = createConstructor(bean, valueAssignmentStatements);
            newBean = newBean.withConstructor(constructor);
        }
        return newBean;
    }

    private static TsConstructorModel createConstructor(TsBeanModel bean,
                                                        Collection<TsExpressionStatement> valueAssignmentStatements) {
        List<TsStatement> body = new ArrayList<>();
        if (bean.getParent() != null) {
            body.add(new TsExpressionStatement(new TsCallExpression(new TsSuperExpression())));
        }

        body.addAll(valueAssignmentStatements);

        return new TsConstructorModel(TsModifierFlags.None, Collections.emptyList(), body, null);
    }

    private static TsExpressionStatement createValueAssignmentStatement(TsPropertyModel property, TsExpression value) {
        TsMemberExpression leftHandSideExpression = new TsMemberExpression(new TsThisExpression(), property.name);
        TsExpression assignment = new TsAssignmentExpression(leftHandSideExpression, value);
        return new TsExpressionStatement(assignment);
    }

    private static Optional<TsExpression> findOnlyValueForProperty(TsPropertyModel property, TsModel model) {
        TsType propertyType = property.tsType;
        if (propertyType instanceof TsType.UnionType) {
            return findOnlyValueForUnionType((TsType.UnionType) propertyType);
        }
        if (propertyType instanceof TsType.EnumReferenceType) {
            return findOnlyValueForEnumReferenceType(model, (TsType.EnumReferenceType) propertyType);
        }
        return Optional.empty();
    }

    private static Optional<TsExpression> findOnlyValueForUnionType(TsType.UnionType unionType) {
        List<TsType> unionTypeElements = unionType.types;
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

    private static Optional<TsExpression> findOnlyValueForEnumReferenceType(TsModel model,
                                                                            TsType.EnumReferenceType propertyType) {
        Symbol symbol = propertyType.symbol;
        Optional<TsEnumModel> enumModelOption = model.getOriginalStringEnums().stream()
                .filter(candidate -> candidate.getName().getFullName().equals(symbol.getFullName()))
                .findAny();
        if (!enumModelOption.isPresent()) {
            return Optional.empty();
        }
        TsEnumModel enumModel = enumModelOption.get();
        if (enumModel.getMembers().size() != 1) {
            return Optional.empty();
        }
        EnumMemberModel singleElement = enumModel.getMembers().iterator().next();
        Object enumValue = singleElement.getEnumValue();
        TsStringLiteral expression = new TsStringLiteral((String) enumValue);
        return Optional.of(expression);
    }

}
