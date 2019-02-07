package me.qoomon.gradle.gitversioning;

import static java.util.stream.Collectors.toList;

import java.util.Optional;

import javax.annotation.Nonnull;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import me.qoomon.gitversioning.GitVersionDetails;
import me.qoomon.gitversioning.GitVersioning;
import me.qoomon.gitversioning.VersionDescription;

public class GitVersioningPlugin implements Plugin<Project> {

    private static final Logger LOG = Logging.getLogger(GitVersioningPlugin.class);

    public void apply(@Nonnull Project project) {

        project.getTasks().create("version", VersionTask.class);
        GitVersioningPluginExtension config = project.getExtensions()
                .create("gitVersioning", GitVersioningPluginExtension.class, project);

        project.afterEvaluate(evaluatedProject -> {
            GitVersioning gitVersioning = GitVersioning.build(project.getProjectDir(),
                    Optional.ofNullable(config.commit).map(it -> new VersionDescription(null, null, it.versionFormat))
                            .orElse(new VersionDescription()),
                    config.branches.stream().map(it -> new VersionDescription(it.pattern, it.prefix, it.versionFormat))
                            .collect(toList()),
                    config.tags.stream()
                            .map(it -> new VersionDescription(it.pattern, it.prefix, it.versionFormat))
                            .collect(toList()));

            if (!config.enabled) {
                LOG.warn("Git Versioning Plugin disabled.");
                return;
            }

            project.getAllprojects().forEach(it -> {
                GitVersionDetails gitVersionDetails = gitVersioning.determineVersion(it.getVersion().toString());
                it.getLogger().info(it.getDisplayName() + " git versioning [" + it.getVersion() + " -> " + gitVersionDetails.getVersion() + "]"
                        + " (" + gitVersionDetails.getCommitRefType() + ":" + gitVersionDetails.getCommitRefName() + ")");

                it.setVersion(gitVersionDetails.getVersion());

                it.getExtensions().getExtraProperties().set("project.commit",
                        gitVersionDetails.getCommit());
                it.getExtensions().getExtraProperties().set("project.tag",
                        gitVersionDetails.getCommitRefType().equals("tag") ? gitVersionDetails.getCommitRefName() : "");
                it.getExtensions().getExtraProperties().set("project.branch",
                        gitVersionDetails.getCommitRefType().equals("branch") ? gitVersionDetails.getCommitRefName() : "");
            });
        });
    }
}

