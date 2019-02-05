package me.qoomon.gradle.gitversioning;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.logging.Logger;
import org.gradle.api.logging.Logging;

import me.qoomon.gitversioning.GitUtil;
import me.qoomon.gitversioning.GitVersionDetails;
import me.qoomon.gitversioning.GitVersioning;
import me.qoomon.gitversioning.StringUtil;

public class GitVersioningPlugin implements Plugin<Project> {

    private static final Logger LOG = Logging.getLogger(GitVersioningPlugin.class);

    public void apply(@Nonnull Project project) {

        project.getTasks().create("version", VersionTask.class);

        GitVersioningPluginExtension configuration = project.getExtensions().findByType(GitVersioningPluginExtension.class);
        //        if (!configuration.isEnabled()) { // TODO
        //            project.getLogger().info("disabled");
        //            return;
        //        }

        GitVersioning gitVersioning = GitVersioning.build(project.getProjectDir());

        project.afterEvaluate(evaluatedProject -> {
            project.getAllprojects().forEach(p -> {
                //                p.getLogger()   .info("--- " + BuildProperties.projectArtifactId() + ":" + BuildProperties.projectVersion() + " ---");
                GitVersionDetails gitVersionDetails = gitVersioning.determineVersion(p.getVersion().toString());
                //                p.getLogger().info(projectGav.getArtifactId() + ":" + projectGav.getVersion()
                //                        + " - " + projectGitBasedVersion.getCommitRefType() + ": " + projectGitBasedVersion.getCommitRefName()
                //                        + " -> version: " + projectGitBasedVersion.getVersion());

                p.setVersion(gitVersionDetails.getVersion());

                p.getExtensions().getExtraProperties().set("project.commit",
                        gitVersionDetails.getCommit());
                p.getExtensions().getExtraProperties().set("project.tag",
                        gitVersionDetails.getCommitRefType().equals("tag") ? gitVersionDetails.getCommitRefName() : "");
                p.getExtensions().getExtraProperties().set("project.branch",
                        gitVersionDetails.getCommitRefType().equals("branch") ? gitVersionDetails.getCommitRefName() : "");
            });
        });
    }
}

