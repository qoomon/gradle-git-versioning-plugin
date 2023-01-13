package me.qoomon.gradle.gitversioning;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.tasks.TaskContainer;

import javax.annotation.Nonnull;

public class GitVersioningPlugin implements Plugin<Project> {

    /**
     * for main logic see {@link GitVersioningPluginExtension#apply(GitVersioningPluginConfig)}
     */
    public void apply(@Nonnull Project project) {

        project.getExtensions().create("gitVersioning", GitVersioningPluginExtension.class, project);

        TaskContainer tasks = project.getTasks();
        if(tasks.findByName("version") == null) {
            tasks.register("version", VersionTask.class);
        }
    }
}
