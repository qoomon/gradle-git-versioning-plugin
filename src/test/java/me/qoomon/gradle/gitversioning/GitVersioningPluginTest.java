package me.qoomon.gradle.gitversioning;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.testkit.runner.TaskOutcome;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

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
        String givenVersion = "1.2.3";
        writeFile("plugins { id 'me.qoomon.git-versioning' }\nversion = '" + givenVersion + "'", buildFile);

        // when
        BuildResult buildresult = GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments("version", "-q")
                .build();

        // then
        assertThat(buildresult.task(":version")).satisfies(it ->
                assertThat(it.getOutcome()).isEqualTo(TaskOutcome.SUCCESS)
        );
        assertThat(buildresult.getOutput()).isEqualTo(givenVersion + "\n");
    }

    @Test
    void apply() throws GitAPIException, IOException {

        // given
        Git git = Git.init().setDirectory(projectDir.toFile()).call();
        RevCommit commit = git.commit().setMessage("initial commit").setAllowEmpty(true).call();

        Project project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build();

        project.getPluginManager().apply(GitVersioningPlugin.class);

        GitVersioningPluginExtension extension = (GitVersioningPluginExtension) project.getExtensions()
                .getByName("gitVersioning");

        GitVersioningPluginConfig config = new GitVersioningPluginConfig();

        // when
        extension.apply(config);

        // then
        assertThat(project.getVersion()).isEqualTo("master-SNAPSHOT");
    }

    @Test
    void apply_with_extension_commit_description() throws GitAPIException, IOException {

        // given
        Git git = Git.init().setDirectory(projectDir.toFile()).call();
        git.commit().setMessage("initial commit").setAllowEmpty(true).call();

        Project project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build();

        project.getPluginManager().apply(GitVersioningPlugin.class);

        GitVersioningPluginExtension extension = (GitVersioningPluginExtension) project.getExtensions()
                .getByName("gitVersioning");

        GitVersioningPluginConfig config = new GitVersioningPluginConfig() {{
            branches.add(new VersionDescription() {{
                versionFormat = "branch-gitVersioning";
            }});
        }};

        // when
        extension.apply(config);

        // then
        assertThat(project.getVersion()).isEqualTo("branch-gitVersioning");
    }

    @Test
    void apply_with_extension_branch_description() throws GitAPIException, IOException {

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

        GitVersioningPluginConfig config = new GitVersioningPluginConfig() {{
            branch(new VersionDescription() {{
                versionFormat = "${branch}-gitVersioning";
            }});
        }};

        // when
        extension.apply(config);

        // then
        assertThat(project.getVersion()).isEqualTo(givenBranch.replace("/", "-") + "-gitVersioning");
    }

    @Test
    void apply_with_extension_tag_description() throws GitAPIException, IOException {

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

        GitVersioningPluginConfig config = new GitVersioningPluginConfig() {{
            tag(new VersionDescription() {{
                versionFormat = "${tag}-gitVersioning";
            }});
        }};

        // when
        extension.apply(config);

        // then
        assertThat(project.getVersion()).isEqualTo(givenTag + "-gitVersioning");
    }

    @Test
    void apply_normalizeVersion() throws GitAPIException, IOException {

        // given
        Git.init().setDirectory(projectDir.toFile()).call();

        Project project = ProjectBuilder.builder().withProjectDir(projectDir.toFile()).build();

        project.getPluginManager().apply(GitVersioningPlugin.class);

        GitVersioningPluginExtension extension = (GitVersioningPluginExtension) project.getExtensions()
                .getByName("gitVersioning");

        GitVersioningPluginConfig config = new GitVersioningPluginConfig() {{
            branches.add(new VersionDescription() {{
                versionFormat = "a/b/c";
            }});
        }};

        // when
        extension.apply(config);

        // then
        assertThat(project.getVersion()).isEqualTo("a-b-c");
    }
}