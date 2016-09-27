
package cz.habarta.typescript.generator.compiler;

import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.util.Pair;
import java.util.*;


/**
 * Name table.
 */
public class SymbolTable {

    private final Settings settings;
    private final LinkedHashMap<Pair<Class<?>, String>, Symbol> symbols = new LinkedHashMap<>();
    private final LinkedHashMap<String, Symbol> syntheticSymbols = new LinkedHashMap<>();

    public SymbolTable(Settings settings) {
        this.settings = settings;
    }

    public Symbol getSymbol(Class<?> cls) {
        return getSymbol(cls, null);
    }

    public Symbol getSymbol(Class<?> cls, String suffix) {
        final Pair<Class<?>, String> key = Pair.<Class<?>, String>of(cls, suffix);
        if (!symbols.containsKey(key)) {
            final String suffixString = suffix != null ? suffix : "";
            symbols.put(key, new Symbol("$" + cls.getName().replace('.', '$') + suffixString + "$"));
        }
        return symbols.get(key);
    }

    public Symbol hasSymbol(Class<?> cls, String suffix) {
        return symbols.get(Pair.<Class<?>, String>of(cls, suffix));
    }

    public Class<?> getSymbolClass(Symbol symbol) {
        for (Map.Entry<Pair<Class<?>, String>, Symbol> entry : symbols.entrySet()) {
            if (entry.getValue() == symbol) {
                return entry.getKey().getValue1();
            }
        }
        return null;
    }

    public Symbol getSyntheticSymbol(String name) {
        if (!syntheticSymbols.containsKey(name)) {
            syntheticSymbols.put(name, new Symbol(name));
        }
        return syntheticSymbols.get(name);
    }

    public void resolveSymbolNames() {
        final Map<String, List<Class<?>>> names = new LinkedHashMap<>();
        for (Map.Entry<Pair<Class<?>, String>, Symbol> entry : symbols.entrySet()) {
            final Class<?> cls = entry.getKey().getValue1();
            final String suffix = entry.getKey().getValue2();
            final Symbol symbol = entry.getValue();
            final String suffixString = suffix != null ? suffix : "";
            final String name = getMappedName(cls) + suffixString;
            symbol.name = name;
            if (!names.containsKey(name)) {
                names.put(name, new ArrayList<Class<?>>());
            }
            names.get(name).add(cls);
        }
        reportConflicts(names);
    }

    private static void reportConflicts(Map<String, List<Class<?>>> names) {
        boolean conflict = false;
        for (Map.Entry<String, List<Class<?>>> entry : names.entrySet()) {
            final String name = entry.getKey();
            final List<Class<?>> classes = entry.getValue();
            if (classes.size() > 1) {
                System.out.println(String.format("Multiple classes are mapped to '%s' name. Conflicting classes: %s", name, classes));
                conflict = true;
            }
        }
        if (conflict) {
            throw new NameConflictException("Multiple classes are mapped to the same name. You can use 'customTypeNaming' setting to resolve conflicts or exclude conflicting class if it was added accidentally.");
        }
    }

    private String getMappedName(Class<?> cls) {
        if (cls == null) {
            return null;
        }
        final String customName = settings.customTypeNaming.get(cls.getName());
        if (customName != null) {
            return customName;
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


    public static class NameConflictException extends RuntimeException {
        
        private static final long serialVersionUID = 1L;

        public NameConflictException() {
        }

        public NameConflictException(String message) {
            super(message);
        }

        public NameConflictException(String message, Throwable cause) {
            super(message, cause);
        }

        public NameConflictException(Throwable cause) {
            super(cause);
        }

    }

}
