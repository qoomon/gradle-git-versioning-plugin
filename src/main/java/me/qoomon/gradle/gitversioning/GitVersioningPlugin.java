package me.qoomon.gradle.gitversioning;

import org.gradle.api.Plugin;
import org.gradle.api.Project;

public class GitVersioningPlugin implements Plugin<Project> {

    public void apply(Project project) {
        project.getTasks().create("version", VersionTask.class);

        project.afterEvaluate(evaluatedProject -> {
            GitVersioningPluginExtension configuration = project.getExtensions().findByType(GitVersioningPluginExtension.class);

            System.out.println(project.getRootDir());

            project.getAllprojects().forEach(p -> p.setVersion("GIT-SNAPSHOT"));

            project.getAllprojects().forEach(p -> p.getExtensions().getExtraProperties().set("branch", "master"));
        });
    }
}

