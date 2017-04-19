
package cz.habarta.typescript.generator.compiler;


public class Symbol {

    private String namespace;
    private String simpleName;

    public Symbol(String temporaryName) {
        this.simpleName = temporaryName;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getSimpleName() {
        return simpleName;
    }

    public String getFullName() {
        return namespace != null ? namespace + "." + simpleName : simpleName;
    }

    public void setFullName(String fullName) {
        final int index = fullName.lastIndexOf('.');
        if (index == -1) {
            namespace = null;
            simpleName = fullName;
        } else {
            namespace = fullName.substring(0, index);
            simpleName = fullName.substring(index + 1);
        }
    }

}
