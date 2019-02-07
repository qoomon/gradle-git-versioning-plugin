package me.qoomon.gradle.gitversioning;

import static org.assertj.core.api.Assertions.assertThat;
import static org.gradle.util.GFileUtils.writeFile;

import java.io.File;
import java.nio.file.Path;

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

class GitVersioningPluginTest {

    @TempDir
    Path tempDir;

    @Test
    void test() throws GitAPIException {
        // given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();

        File buildFile = tempDir.resolve("build.gradle").toFile();
        writeFile("plugins { id 'me.qoomon.git-versioning' }", buildFile);

        // when
        BuildResult buildresult = GradleRunner.create()
                .withProjectDir(tempDir.toFile())
                .withPluginClasspath()
                .withArguments("version")
                .build();
        TaskOutcome taskOutcome = buildresult.task(":version").getOutcome();

        // then
        assertThat(taskOutcome).isEqualTo(TaskOutcome.SUCCESS);
    }

    @Test
    void apply() throws GitAPIException {
        // given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();
        RevCommit commit = git.commit().setMessage("initial commit").setAllowEmpty(true).call();

        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        project.setVersion("1.0.0");
        project.getExtensions().add("me.qoomon.git-versioning", new GitVersioningPluginExtension(project));
        project.getPluginManager().apply(GitVersioningPlugin.class);

        // when
        ((ProjectInternal) project).evaluate();

        // then
        assertThat(project.getVersion()).isEqualTo(commit.name());
    }
}