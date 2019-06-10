
package cz.habarta.typescript.generator.gradle;

import java.util.Collections;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;


public class TypeScriptGeneratorPlugin implements Plugin<Project> {

    @Override
    public void apply(Project project) {
        final Task generateTsTask = project.task(Collections.singletonMap(Task.TASK_TYPE, GenerateTask.class), "generateTypeScript");

        for (Task task : project.getTasks()) {
            if (task.getName().startsWith("compile")) {
                generateTsTask.dependsOn(task.getName());
            }
        }
    }

}
