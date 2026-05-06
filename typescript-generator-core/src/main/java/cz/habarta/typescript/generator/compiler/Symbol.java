
package cz.habarta.typescript.generator.compiler;

import org.jspecify.annotations.Nullable;


public class Symbol {

    private @Nullable String module;
    private @Nullable String namespace;
    private String simpleName;
    private boolean isResolved = false;

    public Symbol(String temporaryName) {
        this.simpleName = temporaryName;
    }

    public @Nullable String getModule() {
        return module;
    }

    public @Nullable String getNamespace() {
        return namespace;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public boolean isResolved() {
        return isResolved;
    }

    public String getFullName() {
        String fullName = simpleName;
        if (namespace != null) {
            fullName = namespace + "." + fullName;
        }
        if (module != null) {
            fullName = module + "." + fullName;
        }
        return fullName;
    }

    public void setFullName(@Nullable String module, String namespacedName) {
        this.module = module;
        final int index = namespacedName.lastIndexOf('.');
        if (index == -1) {
            namespace = null;
            simpleName = namespacedName;
        } else {
            namespace = namespacedName.substring(0, index);
            simpleName = namespacedName.substring(index + 1);
        }
        this.isResolved = true;
    }

    void addSuffix(String suffix) {
        simpleName = simpleName + suffix;
    }

}
