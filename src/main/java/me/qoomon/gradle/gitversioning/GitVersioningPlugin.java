package me.qoomon.gradle.gitversioning;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import javax.annotation.Nonnull;

public class GitVersioningPlugin implements Plugin<Project> {

    /**
     * for main logic see {@link GitVersioningPluginExtension#apply}
     */
    public void apply(@Nonnull Project project) {

        project.getExtensions().create("gitVersioning", GitVersioningPluginExtension.class, project);

        // Only Register version task for current project so the plugin may be applied
        // on
        // sub projects. It would be nice to only define this once for all project in
        // a multi-module project, however there are a few other considerations that
        // need
        // to be made and this current change will make this plugin more composable.
        project.getTasks().register("version", VersionTask.class);
    }
}

