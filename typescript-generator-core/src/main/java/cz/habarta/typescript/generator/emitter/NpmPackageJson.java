
package cz.habarta.typescript.generator.emitter;

import cz.habarta.typescript.generator.util.Utils;
import java.util.Map;
import org.jspecify.annotations.Nullable;


public class NpmPackageJson {

    public @Nullable String name; // required
    public @Nullable String version; // required
    public @Nullable String types;
    public @Nullable String main;
    public @Nullable Map<String, String> dependencies;
    public @Nullable Map<String, String> devDependencies;
    public @Nullable Map<String, String> peerDependencies;
    public @Nullable Map<String, String> scripts;

    public NpmPackageJson(
        @Nullable String name,
        @Nullable String version,
        @Nullable String types,
        @Nullable String main,
        @Nullable Map<String, String> dependencies,
        @Nullable Map<String, String> devDependencies,
        @Nullable Map<String, String> peerDependencies,
        @Nullable Map<String, String> scripts
    ) {
        this.name = name;
        this.version = version;
        this.types = types;
        this.main = main;
        this.dependencies = Utils.nullIfEmpty(dependencies);
        this.devDependencies = Utils.nullIfEmpty(devDependencies);
        this.peerDependencies = Utils.nullIfEmpty(peerDependencies);
        this.scripts = Utils.nullIfEmpty(scripts);
    }

}
