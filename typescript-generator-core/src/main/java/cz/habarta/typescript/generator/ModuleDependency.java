
package cz.habarta.typescript.generator;

import cz.habarta.typescript.generator.util.Utils;
import java.io.File;


public class ModuleDependency {

    public String importFrom;
    public String importAs;
    public File infoJson;
    public String npmPackageName;
    public String npmVersionRange;

    public ModuleDependency() {
    }

    public ModuleDependency(String importFrom, String importAs, File infoJson, String npmPackageName, String npmVersionRange) {
        this.importFrom = importFrom;
        this.importAs = importAs;
        this.infoJson = infoJson;
        this.npmPackageName = npmPackageName;
        this.npmVersionRange = npmVersionRange;
    }

    @Override
    public String toString() {
        return Utils.objectToString(this);
    }

}
