
package cz.habarta.typescript.generator.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;


public class TypeScriptGeneratorPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        GenerateTask generateTsTask = project.getTasks().create("generateTypeScript", GenerateTask.class);
        generateTsTask.projectName = project.getName();
        for (Task task : project.getTasks()) {
            if (task.getName().startsWith("compile") && !task.getName().startsWith("compileTest")) {
                generateTsTask.dependsOn(task.getName());
            }
        }

    }
}
