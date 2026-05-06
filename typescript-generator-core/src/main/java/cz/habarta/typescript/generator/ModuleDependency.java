
package cz.habarta.typescript.generator;

import com.fasterxml.jackson.annotation.JsonInclude;
import cz.habarta.typescript.generator.util.Utils;
import java.io.File;
import org.jspecify.annotations.Nullable;


@JsonInclude(JsonInclude.Include.NON_DEFAULT)
public class ModuleDependency {

    public boolean global;
    public @Nullable String importFrom;
    public @Nullable String importAs;
    public File infoJson;
    public @Nullable String npmPackageName;
    public @Nullable String npmVersionRange;
    public boolean peerDependency;

    public ModuleDependency(
        boolean global,
        @Nullable String importFrom,
        @Nullable String importAs,
        File infoJson,
        @Nullable String npmPackageName,
        @Nullable String npmVersionRange,
        boolean peerDependency
    ) {
        this.global = global;
        this.importFrom = importFrom;
        this.importAs = importAs;
        this.infoJson = infoJson;
        this.npmPackageName = npmPackageName;
        this.npmVersionRange = npmVersionRange;
        this.peerDependency = peerDependency;
    }

    public static ModuleDependency module(String importFrom, String importAs, File infoJson, String npmPackageName, String npmVersionRange) {
        return new ModuleDependency(false, importFrom, importAs, infoJson, npmPackageName, npmVersionRange, false);
    }

    public static ModuleDependency global(File infoJson) {
        return new ModuleDependency(true, null, null, infoJson, null, null, false);
    }

    @Override
    public String toString() {
        return Utils.objectToString(this);
    }

    public String toShortString() {
        return global ? "global" : "'" + importFrom + "'";
    }

}
