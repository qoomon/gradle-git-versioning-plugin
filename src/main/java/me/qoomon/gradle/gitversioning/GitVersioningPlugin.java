package me.qoomon.gradle.gitversioning;

import me.qoomon.gitversioning.*;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import javax.annotation.Nonnull;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

public class GitVersioningPlugin implements Plugin<Project> {

    private static final Logger LOG = Logging.getLogger(GitVersioningPlugin.class);

    public void apply(@Nonnull Project project) {
        project.getTasks().create("version", VersionTask.class);

        GitVersioningPluginExtension config = project.getExtensions()
                .create("gitVersioning", GitVersioningPluginExtension.class, project);
        project.afterEvaluate(evaluatedProject -> {

            if (!config.enabled) {
                LOG.warn("Git Versioning Plugin disabled.");
                return;
            }

            Boolean providedClean = null; // TODO
            String providedCommit = null; // TODO
            String providedBranch = null; // TODO
            String providedTag = null; // TODO

            GitRepoSituation repoSituation = GitUtil.situation(project.getProjectDir());
            if (providedClean != null) {
                repoSituation.setClean(providedClean);
            }
            if (providedCommit != null) {
                repoSituation.setHeadCommit(providedCommit);
            }
            if (providedBranch != null) {
                repoSituation.setHeadBranch(providedBranch.equals("") ? null : providedBranch);
            }
            if (providedTag != null) {
                repoSituation.setHeadTags(providedTag.equals("") ? emptyList() : singletonList(providedTag));
            }

            GitVersioning gitVersioning = GitVersioning.build(repoSituation,
                    ofNullable(config.commit).map(it -> new VersionDescription(null, it.versionFormat))
                            .orElse(new VersionDescription()),
                    config.branches.stream().map(it -> new VersionDescription(it.pattern, it.versionFormat))
                            .collect(toList()),
                    config.tags.stream()
                            .map(it -> new VersionDescription(it.pattern, it.versionFormat))
                            .collect(toList()));

            GitVersionDetails gitVersionDetails = gitVersioning.determineVersion(project.getVersion().toString());

            project.getAllprojects().forEach(it -> {
                // TODO check for version is equals to root project

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

