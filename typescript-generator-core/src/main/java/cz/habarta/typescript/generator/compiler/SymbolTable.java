
package cz.habarta.typescript.generator.compiler;

import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.util.Pair;
import java.util.*;
import javax.script.Invocable;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;


/**
 * Name table.
 */
public class SymbolTable {

    private final Settings settings;
    private final LinkedHashMap<Pair<Class<?>, String>, Symbol> symbols = new LinkedHashMap<>();
    private final LinkedHashMap<String, Symbol> syntheticSymbols = new LinkedHashMap<>();
    private CustomTypeNamingFunction customTypeNamingFunction;

    public SymbolTable(Settings settings) {
        this.settings = settings;
    }

    public Symbol getSymbol(Class<?> cls) {
        return getSymbol(cls, null);
    }

    public Symbol getSymbol(Class<?> cls, String suffix) {
        final String suffixString = suffix != null ? suffix : "";
        final Pair<Class<?>, String> key = Pair.<Class<?>, String>of(cls, suffixString);
        if (!symbols.containsKey(key)) {
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

    public Symbol getSyntheticSymbol(String name, String suffix) {
        return getSyntheticSymbol(name + (suffix != null ? suffix : ""));
    }

    public Symbol addSuffixToSymbol(Symbol symbol, String suffix) {
        // try symbols
        for (Map.Entry<Pair<Class<?>, String>, Symbol> entry : symbols.entrySet()) {
            if (entry.getValue() == symbol) {
                return getSymbol(entry.getKey().getValue1(), entry.getKey().getValue2() + suffix);
            }
        }
        // syntheticSymbols
        return getSyntheticSymbol(symbol.name + suffix);
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
            throw new NameConflictException("Multiple classes are mapped to the same name. You can use 'customTypeNaming' or 'customTypeNamingFunction' settings to resolve conflicts or exclude conflicting class if it was added accidentally.");
        }
    }

    public String getMappedName(Class<?> cls) {
        if (cls == null) {
            return null;
        }
        final String customName = settings.customTypeNaming.get(cls.getName());
        if (customName != null) {
            return customName;
        }
        if (settings.customTypeNamingFunction != null) {
            try {
                final CustomTypeNamingFunction function = getCustomTypeNamingFunction();
                final Object getNameResult = function.getName(cls.getName(), cls.getSimpleName());
                if (getNameResult != null && !isUndefined(getNameResult)) {
                    return (String) getNameResult;
                }
            } catch (ScriptException e) {
                throw new RuntimeException("Evaluating 'customTypeNamingFunction' failed.", e);
            }
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

    private static boolean isUndefined(Object variable) {
        // Java 8
//        return ScriptObjectMirror.isUndefined(variable);

        // Hack for Java 7, it should match both:
        // org.mozilla.javascript.Undefined (Java 7)
        // jdk.nashorn.internal.runtime.Undefined (Java 8)
        return variable != null && variable.getClass().getSimpleName().equals("Undefined");
    }

    private CustomTypeNamingFunction getCustomTypeNamingFunction() throws ScriptException {
        if (customTypeNamingFunction == null) {
            final ScriptEngineManager manager = new ScriptEngineManager();
            final ScriptEngine engine = manager.getEngineByName("javascript");
            engine.eval("var getName = " + settings.customTypeNamingFunction);
            final Invocable invocable = (Invocable) engine;
            customTypeNamingFunction = invocable.getInterface(CustomTypeNamingFunction.class);
        }
        return customTypeNamingFunction;
    }

    public static interface CustomTypeNamingFunction {
        public Object getName(String className, String classSimpleName);
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
