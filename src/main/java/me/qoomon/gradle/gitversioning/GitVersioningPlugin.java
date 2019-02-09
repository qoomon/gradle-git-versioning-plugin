package me.qoomon.gradle.gitversioning;

import me.qoomon.gitversioning.GitRepoData;
import me.qoomon.gitversioning.GitVersionDetails;
import me.qoomon.gitversioning.GitVersioning;
import me.qoomon.gitversioning.VersionDescription;
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

            GitRepoData gitRepoData = GitRepoData.get(project.getProjectDir());
            if (providedClean != null) {
                gitRepoData.setClean(providedClean);
            }
            if (providedCommit != null) {
                gitRepoData.setCommit(providedCommit);
            }
            if (providedBranch != null) {
                gitRepoData.setBranch(providedBranch.equals("") ? null : providedBranch);
            }
            if (providedTag != null) {
                gitRepoData.setTags(providedTag.equals("") ? emptyList() : singletonList(providedTag));
            }

            GitVersioning gitVersioning = GitVersioning.build(gitRepoData,
                    ofNullable(config.commit).map(it -> new VersionDescription(null, null, it.versionFormat))
                            .orElse(new VersionDescription()),
                    config.branches.stream().map(it -> new VersionDescription(it.pattern, it.prefix, it.versionFormat))
                            .collect(toList()),
                    config.tags.stream()
                            .map(it -> new VersionDescription(it.pattern, it.prefix, it.versionFormat))
                            .collect(toList()));

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

