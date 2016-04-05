
package cz.habarta.typescript.generator.compiler;

import cz.habarta.typescript.generator.Settings;
import java.util.*;


public class SymbolTable {

    private final Settings settings;
    private final LinkedHashMap<Class<?>, Symbol> symbols = new LinkedHashMap<>();
    private final LinkedHashMap<String, Symbol> syntheticSymbols = new LinkedHashMap<>();

    public SymbolTable(Settings settings) {
        this.settings = settings;
    }

    public Symbol getSymbol(Class<?> cls) {
        if (!symbols.containsKey(cls)) {
            symbols.put(cls, new Symbol("$" + cls.getName().replace('.', '$') + "$"));
        }
        return symbols.get(cls);
    }

    public Symbol getSyntheticSymbol(String name) {
        if (!syntheticSymbols.containsKey(name)) {
            syntheticSymbols.put(name, new Symbol(name));
        }
        return syntheticSymbols.get(name);
    }

    public void resolveSymbolNames(Settings settings) {
//        TODO check for conflicts
        for (Map.Entry<Class<?>, Symbol> entry : symbols.entrySet()) {
            final Class<?> cls = entry.getKey();
            final Symbol symbol = entry.getValue();
            symbol.name = getMappedName(cls);
        }
    }

    private String getMappedName(Class<?> cls) {
        if (cls == null) {
            return null;
        }
        String name = cls.getSimpleName();
        if (settings.removeTypeNamePrefix != null && name.startsWith(settings.removeTypeNamePrefix)) {
            name = name.substring(settings.removeTypeNamePrefix.length(), name.length());
        }
        if (settings.removeTypeNameSuffix != null && name.endsWith(settings.removeTypeNameSuffix)) {
            name = name.substring(0, name.length() - settings.removeTypeNameSuffix.length());
        }
        if (settings.addTypeNamePrefix != null) {
            name = settings.addTypeNamePrefix + name;
        }
        if (settings.addTypeNameSuffix != null) {
            name = name + settings.addTypeNameSuffix;
        }
        return name;
    }

}
