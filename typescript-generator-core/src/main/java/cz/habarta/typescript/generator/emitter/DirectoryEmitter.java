package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.Output;
import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TsProperty;
import cz.habarta.typescript.generator.TsType;
import cz.habarta.typescript.generator.TsType.BasicArrayType;
import cz.habarta.typescript.generator.TsType.FunctionType;
import cz.habarta.typescript.generator.TsType.GenericBasicType;
import cz.habarta.typescript.generator.TsType.GenericReferenceType;
import cz.habarta.typescript.generator.TsType.IndexedArrayType;
import cz.habarta.typescript.generator.TsType.IntersectionType;
import cz.habarta.typescript.generator.TsType.MappedType;
import cz.habarta.typescript.generator.TsType.ObjectType;
import cz.habarta.typescript.generator.TsType.OptionalType;
import cz.habarta.typescript.generator.TsType.ReferenceType;
import cz.habarta.typescript.generator.TsType.UnionType;
import cz.habarta.typescript.generator.TypeScriptGenerator;
import cz.habarta.typescript.generator.TypeScriptOutputKind;
import cz.habarta.typescript.generator.compiler.Symbol;
import java.io.File;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DirectoryEmitter extends Emitter {

    private TsModel currentModel;

    public DirectoryEmitter(Settings settings) {
        super(settings);
    }

    public Output getOutput() {
        return output;
    }

    @Override
    public void emit(TsModel model, Writer output, String outputName, boolean closeOutput, boolean forceExportKeyword, int initialIndentationLevel) {
        initSettings(output, forceExportKeyword, initialIndentationLevel);
        this.currentModel = model;

        emitFileComment();

        final boolean exportElements = settings.outputKind == TypeScriptOutputKind.module;

        emitElements(model, exportElements, false);

        emitUmdNamespace();

        if (closeOutput) {
            close();
        }
    }

    @Override
    protected void emitFullyQualifiedDeclaration(TsDeclarationModel declaration, boolean exportKeyword, boolean declareKeyword) {
        Output fileOutput = null;
        final File outputFile = new File(this.output.getName());

        String simpleName = convertTypeScriptFileName(declaration.getName().getSimpleName());
        String fullName = declaration.getName().getNamespace() != null ? declaration.getName().getNamespace() + "." + simpleName : simpleName;
        String fileName = fullName.replace('.', '/') + settings.getExtension();
        fileOutput = Output.to(new File(outputFile.getParent(), fileName));
        Writer oldWriter = this.writer;
        this.writer = fileOutput.getWriter();
        TypeScriptGenerator.getLogger().info("Writing declarations to: " + fileName);

        emitFileComment();
        emitReferences();
        emitImports();
        emitFileImports(declaration);

        super.emitDeclaration(declaration, exportKeyword, declareKeyword);

        emitDirectoryExtensions(currentModel, declaration, exportKeyword);

        close();

        this.writer = oldWriter;
    }

    private void emitFileImports(TsDeclarationModel declaration) {
        Set<TsType> list = new HashSet<>();

        if (declaration instanceof TsBeanModel) {
            TsBeanModel bean = (TsBeanModel) declaration;
            collectTypes(list, bean.getAllParents());
            collectTypes(list, bean.getProperties().stream().map(TsPropertyModel::getTsType).collect(Collectors.toList()));
        } else if (declaration instanceof TsAliasModel) {
            TsAliasModel alias = (TsAliasModel) declaration;
            collectType(list, alias.getDefinition());
        } else if (declaration instanceof TsEnumModel) {
            // do nothing
        } else {
            throw new RuntimeException("Unknown declaration type: " + declaration.getClass().getName());
        }

        Set<Symbol> genericTypes = new HashSet<>();
        for (TsType tsType : list) {
            TsType type = tsType;
            if (type instanceof ReferenceType) {
                ReferenceType refType = (ReferenceType) type;
                if (type instanceof GenericReferenceType) {
                    Symbol symbol = ((GenericReferenceType) type).symbol;
                    if (!genericTypes.add(symbol)) {
                        continue;
                    }
                }
                if (refType.symbol.getNamespace() != null) {
                    String importPath = calculateImportPath(declaration.name, refType);
                    if (importPath != null) {
                        writeIndentedLine("import { " + refType.symbol.getSimpleName() + " } from " + quote("./" + importPath, settings) + ";");
                    }
                }
            }
        }
    }

    private String convertTypeScriptFileName(String simpleName) {
        return simpleName.replaceAll("(.)(\\p{Upper})", "$1-$2").toLowerCase();
    }

    private String calculateImportPath(Symbol basePath, ReferenceType refType) {
        String baseNamespace = basePath.getNamespace();
        String refNamespace = refType.symbol.getNamespace();

        if (baseNamespace == null || basePath.equals(refType.symbol)) {
            return null;
        }

        List<String> base = new ArrayList<>(Arrays.asList(baseNamespace.split("\\.")));
        List<String> ref = new ArrayList<>(Arrays.asList(refNamespace.split("\\.")));

        while (!base.isEmpty() && !ref.isEmpty() && base.get(0).equals(ref.get(0))) {
            base.remove(0);
            ref.remove(0);
        }

        for (int i = 0; i < base.size(); i++) {
            ref.add(0, "..");
        }

        ref.add(convertTypeScriptFileName(refType.symbol.getSimpleName()));

        return String.join("/", ref);
    }

    private void emitDirectoryExtensions(TsModel model, TsDeclarationModel declaration, boolean exportKeyword) {
        for (EmitterExtension emitterExtension : settings.extensions) {
            if (emitterExtension instanceof DirectoryEmitterExtension) {
                DirectoryEmitterExtension directoryEmitter = (DirectoryEmitterExtension) emitterExtension;
                directoryEmitter.setOutput(output);

                final List<String> extensionLines = new ArrayList<>();
                final EmitterExtension.Writer extensionWriter = line -> extensionLines.add(line);
                directoryEmitter.emitElement(extensionWriter, settings, exportKeyword, model, declaration);
                if (!extensionLines.isEmpty()) {
                    writeNewLine();
                    writeNewLine();
                    writeIndentedLine(String.format("// Added by '%s' extension", emitterExtension.getClass().getSimpleName()));
                    for (String line : extensionLines) {
                        this.writeIndentedLine(line);
                    }
                }
            }
        }
    }

    private void collectTypes(Set<TsType> list, Collection<TsType> types) {
        for (TsType type : types) {
            collectType(list, type);
        }
    }

    private void collectType(Set<TsType> list, TsType type) {
        if (type instanceof BasicArrayType) {
            collectType(list, ((BasicArrayType) type).elementType);
        } else if (type instanceof IndexedArrayType) {
            collectType(list, ((IndexedArrayType) type).elementType);
            collectType(list, ((IndexedArrayType) type).indexType);
        } else if (type instanceof MappedType) {
            collectType(list, ((MappedType) type).parameterType);
            collectType(list, ((MappedType) type).type);
        } else if (type instanceof ObjectType) {
            List<TsProperty> properties = ((ObjectType) type).properties;
            for (TsProperty tsProperty : properties) {
                collectType(list, tsProperty.tsType);
            }
        } else if (type instanceof OptionalType) {
            collectType(list, ((OptionalType) type).type);
        } else if (type instanceof FunctionType) {
            collectType(list, ((FunctionType) type).type);
        } else if (type instanceof GenericBasicType) {
            collectTypes(list, ((GenericBasicType) type).typeArguments);
        } else if (type instanceof GenericReferenceType) {
            list.add(type);
            collectTypes(list, ((GenericReferenceType) type).typeArguments);
        } else if (type instanceof UnionType) {
            List<TsType> types2 = ((UnionType) type).types;
            for (TsType tsType2 : types2) {
                collectType(list, tsType2);
            }
        } else if (type instanceof IntersectionType) {
             for (TsType tsType : ((IntersectionType) type).types) {
                 collectType(list, tsType);
             }
        } else {
            list.add(type);
        }
    }

}
