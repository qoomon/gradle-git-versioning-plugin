package me.qoomon.gradle.gitversioning;

import me.qoomon.gitversioning.*;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtraPropertiesExtension;

import javax.annotation.Nonnull;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

public class GitVersioningPlugin implements Plugin<Project> {

    public void apply(@Nonnull Project project) {
        project.getTasks().create("version", VersionTask.class);

        GitVersioningPluginExtension config = project.getExtensions()
                .create("gitVersioning", GitVersioningPluginExtension.class, project);
        project.afterEvaluate(evaluatedProject -> {

            GitRepoSituation repoSituation = GitUtil.situation(project.getProjectDir());
            String providedBranch = getOption(project, "branch");
            if (providedBranch != null) {
                repoSituation.setHeadBranch(providedBranch.isEmpty() ? null : providedBranch);
            }
            String providedTag = getOption(project, "tag");
            if (providedTag != null) {
                repoSituation.setHeadTags(providedTag.isEmpty() ? emptyList() : singletonList(providedTag));
            }

            GitVersionDetails gitVersionDetails = GitVersioning.determineVersion(repoSituation,
                    ofNullable(config.commit)
                            .map(it -> new VersionDescription(null, it.versionFormat))
                            .orElse(new VersionDescription()),
                    config.branches.stream()
                            .map(it -> new VersionDescription(it.pattern, it.versionFormat))
                            .collect(toList()),
                    config.tags.stream()
                            .map(it -> new VersionDescription(it.pattern, it.versionFormat))
                            .collect(toList()),
                    project.getVersion().toString());


            project.getAllprojects().forEach(it -> {
                // TODO check for version is equals to root project

                it.getLogger().info(it.getDisplayName() + " - git versioning [" + it.getVersion() + " -> " + gitVersionDetails.getVersion() + "]"
                        + " (" + gitVersionDetails.getCommitRefType() + ":" + gitVersionDetails.getCommitRefName() + ")");
                it.setVersion(gitVersionDetails.getVersion());

                ExtraPropertiesExtension extraProperties = it.getExtensions().getExtraProperties();
                extraProperties.set("git.commit", gitVersionDetails.getCommit());
                extraProperties.set("git.ref", gitVersionDetails.getCommitRefName());
                extraProperties.set("git." + gitVersionDetails.getCommitRefType(), gitVersionDetails.getCommitRefName());
                gitVersionDetails.getMetaData().forEach((key, value) -> extraProperties.set("git.ref." + key, value)); // TODO write tests
            });
        });
    }

    private String getOption(final Project project, final String name) {
        String key = "git." + name;
        String value = (String) project.getProperties().get(key);
        if (value == null) {
            value = System.getenv(key.replaceAll("[A-Z]", "_$0").toUpperCase());
        }
        return value;
    }
}

