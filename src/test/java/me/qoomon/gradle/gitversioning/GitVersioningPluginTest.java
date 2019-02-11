package me.qoomon.gradle.gitversioning;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.gradle.api.Project;
import org.gradle.api.internal.project.ProjectInternal;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.file.Path;

import static me.qoomon.gitversioning.GitConstants.NO_COMMIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.util.GFileUtils.writeFile;

class GitVersioningPluginTest {

    @TempDir
    Path projectDir;

    @Test
    void runVersionTask() throws GitAPIException {
        // given
        Git.init().setDirectory(projectDir.toFile()).call();

        File buildFile = projectDir.resolve("build.gradle").toFile();
        writeFile("plugins { id 'me.qoomon.git-versioning' }", buildFile);

        // when
        BuildResult buildresult = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("version", "-q")
                .build();

        // then
        assertThat(buildresult.task(":version").getOutcome()).isEqualTo(TaskOutcome.SUCCESS);
        assertThat(buildresult.getOutput()).isEqualTo(NO_COMMIT + "\n");
    }

    @Test
    void apply() throws GitAPIException {

        // given
        Git git = Git.init().setDirectory(projectDir.toFile()).call();
        RevCommit commit = git.commit().setMessage("initial commit").setAllowEmpty(true).call();

        Project project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build();

        project.getPluginManager().apply(GitVersioningPlugin.class);

        // when
        ((ProjectInternal) project).evaluate();

        // then
        assertThat(project.getVersion()).isEqualTo(commit.name());
    }

    @Test
    void apply_with_extension_commit_description() throws GitAPIException {

        // given
        Git git = Git.init().setDirectory(projectDir.toFile()).call();
        git.commit().setMessage("initial commit").setAllowEmpty(true).call();

        Project project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build();

        project.getPluginManager().apply(GitVersioningPlugin.class);

        GitVersioningPluginExtension extension = (GitVersioningPluginExtension) project.getExtensions()
                .getByName("gitVersioning");
        extension.commit = new GitVersioningPluginExtension.CommitVersionDescription();
        extension.commit.versionFormat = "commit-gitVersioning";

        // when
        ((ProjectInternal) project).evaluate();

        // then
        assertThat(project.getVersion()).isEqualTo("commit-gitVersioning");
    }

    @Test
    void apply_with_extension_branch_description() throws GitAPIException {

        // given
        Git git = Git.init().setDirectory(projectDir.toFile()).call();
        git.commit().setMessage("initial commit").setAllowEmpty(true).call();
        String givenBranch = "feature/sandbox";
        git.branchCreate().setName(givenBranch).call();
        git.checkout().setName(givenBranch).call();

        Project project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build();

        project.getPluginManager().apply(GitVersioningPlugin.class);

        GitVersioningPluginExtension extension = (GitVersioningPluginExtension) project.getExtensions()
                .getByName("gitVersioning");
        GitVersioningPluginExtension.VersionDescription branchVersionDescription = new GitVersioningPluginExtension.VersionDescription();
        branchVersionDescription.versionFormat = "${branch}-gitVersioning";
        extension.branches.add(branchVersionDescription);

        // when
        ((ProjectInternal) project).evaluate();

        // then
        assertThat(project.getVersion()).isEqualTo(givenBranch.replace("/", "-") + "-gitVersioning");
    }

    @Test
    void apply_with_extension_tag_description() throws GitAPIException {

        // given
        Git git = Git.init().setDirectory(projectDir.toFile()).call();
        git.commit().setMessage("initial commit").setAllowEmpty(true).call();
        String givenTag = "v1";
        git.tag().setName(givenTag).call();
        git.checkout().setName(givenTag).call();

        Project project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build();

        project.getPluginManager().apply(GitVersioningPlugin.class);

        GitVersioningPluginExtension extension = (GitVersioningPluginExtension) project.getExtensions()
                .getByName("gitVersioning");
        GitVersioningPluginExtension.VersionDescription tagVersionDescription = new GitVersioningPluginExtension.VersionDescription();
        tagVersionDescription.versionFormat = "${tag}-gitVersioning";
        extension.tags.add(tagVersionDescription);

        // when
        ((ProjectInternal) project).evaluate();

        // then
        assertThat(project.getVersion()).isEqualTo(givenTag + "-gitVersioning");
    }

    @Test
    void apply_normalizeVersion() throws GitAPIException {

        // given
        Git.init().setDirectory(projectDir.toFile()).call();

        Project project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build();

        project.getPluginManager().apply(GitVersioningPlugin.class);

        GitVersioningPluginExtension extension = (GitVersioningPluginExtension) project.getExtensions()
                .getByName("gitVersioning");
        GitVersioningPluginExtension.CommitVersionDescription commitVersionDescription = new GitVersioningPluginExtension.CommitVersionDescription();
        commitVersionDescription.versionFormat = "a/b/c";
        extension.commit = commitVersionDescription;

        // when
        ((ProjectInternal) project).evaluate();

        // then
        assertThat(project.getVersion()).isEqualTo("a-b-c");
    }
}