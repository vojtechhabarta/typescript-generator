
package cz.habarta.typescript.generator.maven;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;

import cz.habarta.typescript.generator.Settings;
import cz.habarta.typescript.generator.TypeScriptGenerator;

/**
 * Generates TypeScript declaration file from specified java classes.
 */
@Mojo(name = "generate", defaultPhase = LifecyclePhase.PROCESS_CLASSES, requiresDependencyResolution = ResolutionScope.COMPILE)
public class GenerateMojo extends AbstractMojo {

	private final Logger logger = Logger.getLogger(getClass().getName());

	/**
	 * Output TypeScript declaration file.
	 */
	@Parameter(required = true)
	private File outputFile;

	/**
	 * JSON classes to process.
	 */
	@Parameter(required = true)
	private List<String> classes;

	/**
	 * Name of generated TypeScript module.
	 */
	@Parameter
	private String moduleName;

	/**
	 * If true the module name will be generated in ambient style.
	 */
	@Parameter
	private boolean ambientModuleName;

	/**
	 * If true the enums will be generated.
	 */
	@Parameter
	private boolean declareEnums;

	/**
	 * If true declared properties will be optional.
	 */
	@Parameter
	private boolean declarePropertiesAsOptional;

	/**
	 * Suffix which will be removed from names of classes, interfaces,
	 * declareEnums. Type names which don't end with this suffix will not
	 * change.
	 */
	@Parameter
	private String removeTypeNameSuffix;

	@Parameter(defaultValue = "${project}", readonly = true, required = true)
	private MavenProject project;

	@Override
	public void execute() {
		try {
			logger.info("outputFile: " + outputFile);
			logger.info("classes: " + classes);

			// class loader
			final List<URL> urls = new ArrayList<>();
			for (String element : project.getCompileClasspathElements()) {
				urls.add(new File(element).toURI().toURL());
			}
			URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]),
					Thread.currentThread().getContextClassLoader());

			// classes
			final List<Class<?>> classList = new ArrayList<>();
			for (String className : classes) {
				classList.add(classLoader.loadClass(className));
			}

			final Settings settings = new Settings();
			settings.moduleName = moduleName;
			settings.ambientModuleName = ambientModuleName;
			settings.declarePropertiesAsOptional = declarePropertiesAsOptional;
			settings.removeTypeNameSuffix = removeTypeNameSuffix;
			settings.declareEnums = declareEnums;
			TypeScriptGenerator.generateTypeScript(classList, settings, outputFile);

		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

}
