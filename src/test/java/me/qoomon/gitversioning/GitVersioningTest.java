package me.qoomon.gitversioning;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Path;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class GitVersioningTest {

    @TempDir
    Path tempDir;

    @Test
    void determineVersion_emptyRepo() throws GitAPIException {

        // given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();

        // when
        GitVersioning gitVersioning = GitVersioning.build(git.getRepository().getDirectory(),
                new VersionDescription(),
                emptyList(),
                emptyList());
        GitVersionDetails gitVersionDetails = gitVersioning.determineVersion("undefined");

        // then
        assertThat(gitVersionDetails).satisfies(it -> assertSoftly(softly -> {
            softly.assertThat(it.getDirectory()).isEqualTo(git.getRepository().getDirectory());
            softly.assertThat(it.isClean()).isTrue();
            softly.assertThat(it.getCommit()).isEqualTo("0000000000000000000000000000000000000000");
            softly.assertThat(it.getCommitRefType()).isEqualTo("commit");
            softly.assertThat(it.getCommitRefName()).isEqualTo("0000000000000000000000000000000000000000");
            softly.assertThat(it.getVersion()).isEqualTo("0000000000000000000000000000000000000000");
        }));
    }


    @Test
    void determineVersion_forBranch() throws GitAPIException, IOException {

        // given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();
        RevCommit givenCommit = git.commit().setMessage("initial commit").setAllowEmpty(true).call();
        String givenBranch = "develop";
        git.branchCreate().setName(givenBranch).setStartPoint(givenCommit).call();
        git.checkout().setName(givenBranch).call();

        // when
        GitVersioning gitVersioning = GitVersioning.build(git.getRepository().getDirectory(),
                new VersionDescription(),
                asList(new VersionDescription(null, null, "${branch}")),
                emptyList());
        GitVersionDetails gitVersionDetails = gitVersioning.determineVersion("undefined");

        // then
        assertThat(gitVersionDetails).satisfies(it -> assertSoftly(softly -> {
                    softly.assertThat(it.getDirectory()).isEqualTo(git.getRepository().getDirectory());
                    softly.assertThat(it.isClean()).isTrue();
                    softly.assertThat(it.getCommit()).isEqualTo(givenCommit.getName());
                    softly.assertThat(it.getCommitRefType()).isEqualTo("branch");
                    softly.assertThat(it.getCommitRefName()).isEqualTo(givenBranch);
                    softly.assertThat(it.getVersion()).isEqualTo(givenBranch);
                }));
    }

    @Test
    void determineVersion_forBranchAndTag() throws GitAPIException, IOException {

        // given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();
        RevCommit givenCommit = git.commit().setMessage("initial commit").setAllowEmpty(true).call();
        String givenBranch = "develop";
        git.branchCreate().setName(givenBranch).setStartPoint(givenCommit).call();
        git.checkout().setName(givenBranch).call();
        git.tag().setObjectId(givenCommit).setName("v1").call();

        // when
        GitVersioning gitVersioning = GitVersioning.build(git.getRepository().getDirectory(),
                new VersionDescription(),
                asList(new VersionDescription(null, null, "${branch}-branch")),
                emptyList());
        GitVersionDetails gitVersionDetails = gitVersioning.determineVersion("undefined");

        // then
        assertThat(gitVersionDetails).satisfies(it -> assertSoftly(softly -> {
                    softly.assertThat(it.getDirectory()).isEqualTo(git.getRepository().getDirectory());
                    softly.assertThat(it.isClean()).isTrue();
                    softly.assertThat(it.getCommit()).isEqualTo(givenCommit.getName());
                    softly.assertThat(it.getCommitRefType()).isEqualTo("branch");
                    softly.assertThat(it.getCommitRefName()).isEqualTo(givenBranch);
                    softly.assertThat(it.getVersion()).isEqualTo(givenBranch + "-branch");
                }));
    }

    @Test
    void determineVersion_detachedHead() throws GitAPIException {

        // given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();
        RevCommit givenCommit = git.commit().setMessage("initial commit").setAllowEmpty(true).call();
        git.checkout().setName(givenCommit.getName()).call();

        // when
        GitVersioning gitVersioning = GitVersioning.build(git.getRepository().getDirectory(),
                new VersionDescription(null, null, "${commit}-commit"),
                emptyList(),
                emptyList());
        GitVersionDetails gitVersionDetails = gitVersioning.determineVersion("undefined");

        // then
        assertThat(gitVersionDetails).satisfies(it -> assertSoftly(softly -> {
            softly.assertThat(it.getDirectory()).isEqualTo(git.getRepository().getDirectory());
            softly.assertThat(it.isClean()).isTrue();
            softly.assertThat(it.getCommit()).isEqualTo(givenCommit.getName());
            softly.assertThat(it.getCommitRefType()).isEqualTo("commit");
            softly.assertThat(it.getCommitRefName()).isEqualTo(givenCommit.getName());
            softly.assertThat(it.getVersion()).isEqualTo(givenCommit.getName() + "-commit");
        }));
    }

    @Test
    void determineVersion_detachedHeadAndTag() throws GitAPIException {

        // given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();
        RevCommit givenCommit = git.commit().setMessage("initial commit").setAllowEmpty(true).call();
        String givenTag = "v1";
        git.tag().setObjectId(givenCommit).setName(givenTag).call();
        git.checkout().setName(givenCommit.getName()).call();

        // when
        GitVersioning gitVersioning = GitVersioning.build(git.getRepository().getDirectory(),
                new VersionDescription(),
                emptyList(),
                asList(new VersionDescription("v.*", null, "${tag}-tag")));
        GitVersionDetails gitVersionDetails = gitVersioning.determineVersion("undefined");

        // then
        assertThat(gitVersionDetails).satisfies(it -> assertSoftly(softly -> {
            softly.assertThat(it.getDirectory()).isEqualTo(git.getRepository().getDirectory());
            softly.assertThat(it.isClean()).isTrue();
            softly.assertThat(it.getCommit()).isEqualTo(givenCommit.getName());
            softly.assertThat(it.getCommitRefType()).isEqualTo("tag");
            softly.assertThat(it.getCommitRefName()).isEqualTo(givenTag);
            softly.assertThat(it.getVersion()).isEqualTo(givenTag + "-tag");
        }));
    }

    @Test
    void determineVersion_normalizeVersionCharacters() throws GitAPIException {

        // given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();

        // when
        GitVersioning gitVersioning = GitVersioning.build(git.getRepository().getDirectory(),
                new VersionDescription(null, null, "x/y/z"),
                emptyList(),
                emptyList());
        GitVersionDetails gitVersionDetails = gitVersioning.determineVersion("undefined");

        // then
        assertThat(gitVersionDetails.getVersion()).isEqualTo("x-y-z");
    }

    @Test
    void determineVersion_detachedHeadWithTag() throws GitAPIException {

        // given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();
        RevCommit givenCommit = git.commit().setMessage("initial commit").setAllowEmpty(true).call();
        String givenTag = "v1";
        git.tag().setObjectId(givenCommit).setName(givenTag).call();
        git.checkout().setName(givenTag).call();

        // when
        GitVersioning gitVersioning = GitVersioning.build(git.getRepository().getDirectory(),
                new VersionDescription(),
                emptyList(),
                asList(new VersionDescription(null, null, "${tag}")));
        GitVersionDetails gitVersionDetails = gitVersioning.determineVersion("undefined");

        // then
        assertThat(gitVersionDetails).satisfies(it -> assertSoftly(softly -> {
                    softly.assertThat(it.getDirectory()).isEqualTo(git.getRepository().getDirectory());
                    softly.assertThat(it.isClean()).isTrue();
                    softly.assertThat(it.getCommit()).isEqualTo(givenCommit.getName());
                    softly.assertThat(it.getCommitRefType()).isEqualTo("tag");
                    softly.assertThat(it.getCommitRefName()).isEqualTo(givenTag);
                    softly.assertThat(it.getVersion()).isEqualTo(givenTag);
                }));
    }


}