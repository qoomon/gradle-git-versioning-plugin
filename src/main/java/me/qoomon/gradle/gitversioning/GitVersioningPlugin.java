package me.qoomon.gradle.gitversioning;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

import javax.annotation.Nonnull;

public class GitVersioningPlugin implements Plugin<Project> {

    /**
     * for main logic see {@link GitVersioningPluginExtension#apply}
     */
    public void apply(@Nonnull Project rootProject) {
        rootProject.getExtensions().create("gitVersioning", GitVersioningPluginExtension.class, rootProject);

        rootProject.getAllprojects().forEach(it -> it.getTasks().create("version", VersionTask.class));
    }
}

