package me.qoomon.gradle.gitversioning;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.gradle.util.GradleVersion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.jgit.lib.Constants.MASTER;

/**
 * Runs real builds with the configuration cache enabled and deprecation warnings turned into failures.
 */
class GitVersioningPluginConfigurationCacheTest {

    private static final String CACHE_STORED = "Configuration cache entry stored.";
    private static final String CACHE_REUSED = "Reusing configuration cache.";
    private static final String CACHE_INVALIDATED = "configuration cache cannot be reused";

    /**
     * oldest Gradle version the plugin is tested with
     */
    private static final String MINIMUM_TESTED_GRADLE_VERSION = "8.14.5";

    @TempDir
    Path projectDir;

    Git git;

    @BeforeEach
    void setUp() throws GitAPIException, IOException {
        git = Git.init().setInitialBranch(MASTER).setDirectory(projectDir.toFile()).call();
        write(".gitignore", ".gradle/\nbuild/\n");
        write("settings.gradle", "rootProject.name = 'test'\n");
    }

    static Stream<String> gradleVersions() {
        return Stream.of(GradleVersion.current().getVersion(), MINIMUM_TESTED_GRADLE_VERSION);
    }

    @ParameterizedTest(name = "Gradle {0}")
    @MethodSource("gradleVersions")
    void versionIsReusedFromCacheUntilHeadChanges(String gradleVersion) throws Exception {
        // given
        writeBuildFile("refs { branch('.+') { version = '${ref}-${commit.short}' } }");
        RevCommit firstCommit = commitAll("initial commit");

        // when / then
        BuildResult stored = runner("version").withGradleVersion(gradleVersion).build();
        assertThat(stored.getOutput()).contains(CACHE_STORED);
        assertThat(versionOf(stored)).isEqualTo("master-" + firstCommit.abbreviate(7).name());

        BuildResult reused = runner("version").withGradleVersion(gradleVersion).build();
        assertThat(reused.getOutput()).contains(CACHE_REUSED);
        assertThat(versionOf(reused)).isEqualTo("master-" + firstCommit.abbreviate(7).name());

        RevCommit secondCommit = git.commit().setMessage("second commit").setAllowEmpty(true).call();
        BuildResult invalidated = runner("version").withGradleVersion(gradleVersion).build();
        assertThat(invalidated.getOutput()).contains(CACHE_INVALIDATED);
        assertThat(versionOf(invalidated)).isEqualTo("master-" + secondCommit.abbreviate(7).name());
    }

    @Test
    void versionIsRecalculatedWhenTagIsCreated() throws Exception {
        // given
        writeBuildFile("refs { branch('.+') { version = '${describe.tag}-${describe.distance}' } }");
        commitAll("initial commit");

        // when / then
        assertThat(versionOf(runVersionTask())).isEqualTo("root-1");
        assertThat(runVersionTask().getOutput()).contains(CACHE_REUSED);

        git.tag().setName("v1.0.0").call();
        BuildResult invalidated = runVersionTask();
        assertThat(invalidated.getOutput()).contains(CACHE_INVALIDATED);
        assertThat(versionOf(invalidated)).isEqualTo("v1.0.0-0");
    }

    @Test
    void versionIsRecalculatedWhenWorkingTreeGetsDirty() throws Exception {
        // given
        writeBuildFile("refs { branch('.+') { version = '1.0.0${dirty}' } }");
        commitAll("initial commit");

        // when / then
        assertThat(versionOf(runVersionTask())).isEqualTo("1.0.0");
        assertThat(runVersionTask().getOutput()).contains(CACHE_REUSED);

        write("settings.gradle", "rootProject.name = 'test'\n// changed\n");
        BuildResult invalidated = runVersionTask();
        assertThat(invalidated.getOutput()).contains(CACHE_INVALIDATED);
        assertThat(versionOf(invalidated)).isEqualTo("1.0.0-DIRTY");
    }

    @Test
    void onlyUsedEnvironmentVariablesInvalidateCache() throws Exception {
        // given
        writeBuildFile("refs { branch('.+') { version = '1.0.${env.BUILD_NUMBER}' } }");
        commitAll("initial commit");

        // when / then
        assertThat(versionOf(runVersionTask(Map.of("BUILD_NUMBER", "1")))).isEqualTo("1.0.1");

        BuildResult reused = runVersionTask(Map.of("BUILD_NUMBER", "1", "UNRELATED", "changed"));
        assertThat(reused.getOutput()).contains(CACHE_REUSED);

        BuildResult invalidated = runVersionTask(Map.of("BUILD_NUMBER", "2"));
        assertThat(invalidated.getOutput()).contains(CACHE_INVALIDATED);
        assertThat(versionOf(invalidated)).isEqualTo("1.0.2");
    }

    @Test
    void multiProjectBuildWithoutImplicitParentLookup() throws Exception {
        // given
        write("settings.gradle", "rootProject.name = 'test'\n"
                + "include 'sub'\n"
                // opt into the Gradle 10 behaviour
                + "enableFeaturePreview('NO_IMPLICIT_LOOKUP_IN_PARENT_PROJECTS')\n");
        write("gradle.properties", "foo=foo@properties\n");
        writeBuildFile("refs { branch('.+') {\n"
                + "    version = '${ref}-SNAPSHOT'\n"
                + "    properties = ['foo': '${value}-${ref}', 'commandLine': '${property.commandLine}']\n"
                + "} }");
        write("sub/build.gradle", "def projectVersion = project.version.toString()\n"
                + "def foo = project.property('foo')\n"
                + "def commandLine = project.property('commandLine')\n"
                + "def gitRef = project.property('git.ref')\n"
                + "tasks.register('printProperties') {\n"
                + "    doLast { println(\"sub: $projectVersion $foo $commandLine $gitRef\") }\n"
                + "}\n");
        commitAll("initial commit");

        // when
        BuildResult result = runner("version", ":sub:printProperties", "-PcommandLine=cli").build();

        // then
        assertThat(versionOf(result)).isEqualTo("master-SNAPSHOT");
        assertThat(result.getOutput()).contains("sub: master-SNAPSHOT foo@properties-master cli master");
    }

    // ---- helpers ----------------------------------------------------------------------------------------------------

    private void writeBuildFile(String gitVersioningConfig) throws IOException {
        write("build.gradle", "plugins { id 'me.qoomon.git-versioning' }\n"
                + "version = '0.0.0'\n"
                + "gitVersioning.apply {\n"
                + "    " + gitVersioningConfig + "\n"
                + "}\n");
    }

    private void write(String path, String content) throws IOException {
        Path file = projectDir.resolve(path);
        Files.createDirectories(file.getParent());
        Files.writeString(file, content);
    }

    private RevCommit commitAll(String message) throws GitAPIException {
        git.add().addFilepattern(".").call();
        return git.commit().setMessage(message).call();
    }

    private BuildResult runVersionTask() {
        return runner("version").build();
    }

    private BuildResult runVersionTask(Map<String, String> environment) {
        Map<String, String> fullEnvironment = new HashMap<>(System.getenv());
        // don't pick up the git situation of the CI system running these tests
        fullEnvironment.keySet().removeAll(asList("GITHUB_ACTIONS", "GITLAB_CI", "CIRCLECI", "JENKINS_HOME"));
        fullEnvironment.putAll(environment);
        return runner("version").withEnvironment(fullEnvironment).build();
    }

    private GradleRunner runner(String... tasks) {
        List<String> arguments = new ArrayList<>(asList(tasks));
        arguments.addAll(asList("--configuration-cache", "--warning-mode=fail", "--stacktrace"));
        return GradleRunner.create()
                .withProjectDir(projectDir.toFile())
                .withPluginClasspath()
                .withArguments(arguments);
    }

    /**
     * @return the line printed by the version task
     */
    private static String versionOf(BuildResult result) {
        List<String> lines = result.getOutput().lines().toList();
        return lines.get(lines.indexOf("> Task :version") + 1);
    }
}
