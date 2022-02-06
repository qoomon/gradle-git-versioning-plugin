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

        project.getAllprojects().forEach(it -> it.getTasks().create("version", VersionTask.class));
    }
}

