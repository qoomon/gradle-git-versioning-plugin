package me.qoomon.gitversioning;

import org.junit.jupiter.api.Test;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class GitVersioningTest {

    @Test
    void determineVersion_forBranch() {

        // given
        GitRepoSituation repoSituation = new GitRepoSituation();
        repoSituation.setHeadBranch("develop");

        GitVersioning gitVersioning = GitVersioning.build(repoSituation,
                new VersionDescription(),
                asList(new VersionDescription(null, "${branch}-branch")),
                emptyList());

        // when
        GitVersionDetails gitVersionDetails = gitVersioning.determineVersion("undefined");

        // then
        assertThat(gitVersionDetails).satisfies(it -> assertSoftly(softly -> {
            softly.assertThat(it.isClean()).isTrue();
            softly.assertThat(it.getCommit()).isEqualTo(repoSituation.getHeadCommit());
            softly.assertThat(it.getCommitRefType()).isEqualTo("branch");
            softly.assertThat(it.getCommitRefName()).isEqualTo(repoSituation.getHeadBranch());
            softly.assertThat(it.getVersion()).isEqualTo(repoSituation.getHeadBranch() + "-branch");
        }));
    }

    @Test
    void determineVersion_forBranchWithTag() {

        // given
        GitRepoSituation repoSituation = new GitRepoSituation();
        repoSituation.setHeadBranch("develop");
        repoSituation.setHeadTags(asList("v1"));

        GitVersioning gitVersioning = GitVersioning.build(repoSituation,
                new VersionDescription(),
                asList(new VersionDescription(null, "${branch}-branch")),
                emptyList());

        // when
        GitVersionDetails gitVersionDetails = gitVersioning.determineVersion("undefined");

        // then
        assertThat(gitVersionDetails).satisfies(it -> assertSoftly(softly -> {
            softly.assertThat(it.isClean()).isTrue();
            softly.assertThat(it.getCommit()).isEqualTo(repoSituation.getHeadCommit());
            softly.assertThat(it.getCommitRefType()).isEqualTo("branch");
            softly.assertThat(it.getCommitRefName()).isEqualTo(repoSituation.getHeadBranch());
            softly.assertThat(it.getVersion()).isEqualTo(repoSituation.getHeadBranch() + "-branch");
        }));
    }

    @Test
    void determineVersion_detachedHead() {

        // given
        GitRepoSituation repoSituation = new GitRepoSituation();

        GitVersioning gitVersioning = GitVersioning.build(repoSituation,
                new VersionDescription(null, "${commit}-commit"),
                emptyList(),
                emptyList());

        // when
        GitVersionDetails gitVersionDetails = gitVersioning.determineVersion("undefined");

        // then
        assertThat(gitVersionDetails).satisfies(it -> assertSoftly(softly -> {
            softly.assertThat(it.isClean()).isTrue();
            softly.assertThat(it.getCommit()).isEqualTo(repoSituation.getHeadCommit());
            softly.assertThat(it.getCommitRefType()).isEqualTo("commit");
            softly.assertThat(it.getCommitRefName()).isEqualTo(repoSituation.getHeadCommit());
            softly.assertThat(it.getVersion()).isEqualTo(repoSituation.getHeadCommit() + "-commit");
        }));
    }

    @Test
    void determineVersion_detachedHeadWithTag() {

        // given
        GitRepoSituation repoSituation = new GitRepoSituation();
        repoSituation.setHeadTags(asList("v1"));

        GitVersioning gitVersioning = GitVersioning.build(repoSituation,
                new VersionDescription(),
                emptyList(),
                asList(new VersionDescription("v.*", "${tag}-tag")));

        // when
        GitVersionDetails gitVersionDetails = gitVersioning.determineVersion("undefined");

        // then
        assertThat(gitVersionDetails).satisfies(it -> assertSoftly(softly -> {
            softly.assertThat(it.isClean()).isTrue();
            softly.assertThat(it.getCommit()).isEqualTo(repoSituation.getHeadCommit());
            softly.assertThat(it.getCommitRefType()).isEqualTo("tag");
            softly.assertThat(it.getCommitRefName()).isEqualTo(repoSituation.getHeadTags().get(0));
            softly.assertThat(it.getVersion()).isEqualTo(repoSituation.getHeadTags().get(0) + "-tag");
        }));
    }

    @Test
    void determineVersion_normalizeVersionCharacters() {

        // given
        String versionFormat = "x/y/z";

        GitVersioning gitVersioning = GitVersioning.build(new GitRepoSituation(),
                new VersionDescription(null, versionFormat),
                emptyList(),
                emptyList());

        // when
        GitVersionDetails gitVersionDetails = gitVersioning.determineVersion("undefined");

        // then
        assertThat(gitVersionDetails.getVersion()).isEqualTo("x-y-z");
    }
}