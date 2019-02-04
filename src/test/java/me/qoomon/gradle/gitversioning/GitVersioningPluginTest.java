package me.qoomon.gradle.gitversioning;

import static org.gradle.util.GFileUtils.writeFile;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.nio.file.Path;

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
    void test() {
        // given
        File buildFile = tempDir.resolve("build.gradle").toFile();
        writeFile("plugins { id 'me.qoomon.git-versioning' }", buildFile);

        // when
        BuildResult result = GradleRunner.create()
                .withProjectDir(tempDir.toFile())
                .withPluginClasspath()
                .withArguments("version")
                .build();

        // then
        assertEquals(result.task(":version").getOutcome(), TaskOutcome.SUCCESS);
    }

    @Test
    void apply() {
        // given
        Project project = ProjectBuilder.builder().withProjectDir(tempDir.toFile()).build();
        project.setVersion("foo");
        project.getExtensions().add("me.qoomon.git-versioning", new GitVersioningPluginExtension(project));
        project.getPluginManager().apply(GitVersioningPlugin.class);

        // when
        ((ProjectInternal) project).evaluate();

        // then
        assertEquals("foo-GIT", project.getVersion());

    }
}