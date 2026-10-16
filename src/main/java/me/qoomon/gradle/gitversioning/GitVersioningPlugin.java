package me.qoomon.gradle.gitversioning;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskContainer;

public class GitVersioningPlugin implements Plugin<Project> {

    /**
     * for main logic see {@link GitVersioningPluginExtension#apply(GitVersioningPluginConfig)}
     */
    public void apply(Project project) {

        project.getExtensions().create("gitVersioning", GitVersioningPluginExtension.class, project);

        TaskContainer tasks = project.getTasks();
        // check names only to not realize an already registered task
        if (!tasks.getNames().contains("version")) {
            tasks.register("version", VersionTask.class, task -> {
                task.setGroup("help");
                task.setDescription("Prints the project version.");
                task.getProjectVersion().convention(project.provider(() -> project.getVersion().toString()));
            });
        }
    }
}
