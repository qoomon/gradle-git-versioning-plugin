package me.qoomon.gradle.gitversioning;

import javax.annotation.Nonnull;

import org.gradle.api.NonNullApi;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class GitVersioningPlugin implements Plugin<Project> {

    public void apply(@Nonnull Project project) {
        project.getTasks().create("version", VersionTask.class);

        project.afterEvaluate(evaluatedProject -> {
            GitVersioningPluginExtension configuration = project.getExtensions().findByType(GitVersioningPluginExtension.class);

            project.getAllprojects().forEach(p -> {
                p.setVersion(p.getVersion() + "-GIT");
                p.getExtensions().getExtraProperties().set("branch", "master");
            });
        });
    }
}

