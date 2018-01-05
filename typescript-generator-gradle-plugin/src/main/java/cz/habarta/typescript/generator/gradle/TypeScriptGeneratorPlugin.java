
package cz.habarta.typescript.generator.gradle;

import java.util.*;
import org.gradle.api.*;


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
