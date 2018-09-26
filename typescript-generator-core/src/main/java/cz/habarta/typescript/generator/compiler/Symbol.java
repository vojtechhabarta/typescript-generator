
package cz.habarta.typescript.generator.compiler;


public class Symbol {

    private String module;
    private String namespace;
    private String simpleName;

    public Symbol(String temporaryName) {
        this.simpleName = temporaryName;
    }

    public String getModule() {
        return module;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getSimpleName() {
        return simpleName;
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

    public void setFullName(String module, String namespacedName) {
        this.module = module;
        final int index = namespacedName.lastIndexOf('.');
        if (index == -1) {
            namespace = null;
            simpleName = namespacedName;
        } else {
            namespace = namespacedName.substring(0, index);
            simpleName = namespacedName.substring(index + 1);
        }
    }

    void addSuffix(String suffix) {
        simpleName = simpleName + suffix;
    }

}
