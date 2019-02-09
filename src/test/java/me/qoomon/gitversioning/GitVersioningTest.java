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
        GitRepoData gitRepoData = new GitRepoData();
        gitRepoData.setBranch("develop");

        GitVersioning gitVersioning = GitVersioning.build(gitRepoData,
                new VersionDescription(),
                asList(new VersionDescription(null, null, "${branch}-branch")),
                emptyList());

        // when
        GitVersionDetails gitVersionDetails = gitVersioning.determineVersion("undefined");

        // then
        assertThat(gitVersionDetails).satisfies(it -> assertSoftly(softly -> {
            softly.assertThat(it.isClean()).isTrue();
            softly.assertThat(it.getCommit()).isEqualTo(gitRepoData.getCommit());
            softly.assertThat(it.getCommitRefType()).isEqualTo("branch");
            softly.assertThat(it.getCommitRefName()).isEqualTo(gitRepoData.getBranch());
            softly.assertThat(it.getVersion()).isEqualTo(gitRepoData.getBranch() + "-branch");
        }));
    }

    @Test
    void determineVersion_forBranchWithTag() {

        // given
        GitRepoData gitRepoData = new GitRepoData();
        gitRepoData.setBranch("develop");
        gitRepoData.setTags(asList("v1"));

        GitVersioning gitVersioning = GitVersioning.build(gitRepoData,
                new VersionDescription(),
                asList(new VersionDescription(null, null, "${branch}-branch")),
                emptyList());

        // when
        GitVersionDetails gitVersionDetails = gitVersioning.determineVersion("undefined");

        // then
        assertThat(gitVersionDetails).satisfies(it -> assertSoftly(softly -> {
            softly.assertThat(it.isClean()).isTrue();
            softly.assertThat(it.getCommit()).isEqualTo(gitRepoData.getCommit());
            softly.assertThat(it.getCommitRefType()).isEqualTo("branch");
            softly.assertThat(it.getCommitRefName()).isEqualTo(gitRepoData.getBranch());
            softly.assertThat(it.getVersion()).isEqualTo(gitRepoData.getBranch() + "-branch");
        }));
    }

    @Test
    void determineVersion_detachedHead() {

        // given
        GitRepoData gitRepoData = new GitRepoData();

        GitVersioning gitVersioning = GitVersioning.build(gitRepoData,
                new VersionDescription(null, null, "${commit}-commit"),
                emptyList(),
                emptyList());

        // when
        GitVersionDetails gitVersionDetails = gitVersioning.determineVersion("undefined");

        // then
        assertThat(gitVersionDetails).satisfies(it -> assertSoftly(softly -> {
            softly.assertThat(it.isClean()).isTrue();
            softly.assertThat(it.getCommit()).isEqualTo(gitRepoData.getCommit());
            softly.assertThat(it.getCommitRefType()).isEqualTo("commit");
            softly.assertThat(it.getCommitRefName()).isEqualTo(gitRepoData.getCommit());
            softly.assertThat(it.getVersion()).isEqualTo(gitRepoData.getCommit() + "-commit");
        }));
    }

    @Test
    void determineVersion_detachedHeadWithTag() {

        // given
        GitRepoData gitRepoData = new GitRepoData();
        gitRepoData.setTags(asList("v1"));

        GitVersioning gitVersioning = GitVersioning.build(gitRepoData,
                new VersionDescription(),
                emptyList(),
                asList(new VersionDescription("v.*", null, "${tag}-tag")));

        // when
        GitVersionDetails gitVersionDetails = gitVersioning.determineVersion("undefined");

        // then
        assertThat(gitVersionDetails).satisfies(it -> assertSoftly(softly -> {
            softly.assertThat(it.isClean()).isTrue();
            softly.assertThat(it.getCommit()).isEqualTo(gitRepoData.getCommit());
            softly.assertThat(it.getCommitRefType()).isEqualTo("tag");
            softly.assertThat(it.getCommitRefName()).isEqualTo(gitRepoData.getTags().get(0));
            softly.assertThat(it.getVersion()).isEqualTo(gitRepoData.getTags().get(0) + "-tag");
        }));
    }

    @Test
    void determineVersion_normalizeVersionCharacters() {

        // given
        String versionFormat = "x/y/z";

        GitVersioning gitVersioning = GitVersioning.build(new GitRepoData(),
                new VersionDescription(null, null, versionFormat),
                emptyList(),
                emptyList());

        // when
        GitVersionDetails gitVersionDetails = gitVersioning.determineVersion("undefined");

        // then
        assertThat(gitVersionDetails.getVersion()).isEqualTo("x-y-z");
    }
}