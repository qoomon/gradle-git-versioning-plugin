package me.qoomon.gitversioning;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;

class GitVersioningTest {

    @TempDir
    Path tempDir;


    @Test
    void determineVersion_empty_repo() throws GitAPIException {

        // given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();

        // when
        GitVersioning gitVersioning = new GitVersioning(git.getRepository().getDirectory());
        GitVersionDetails gitVersionDetails = gitVersioning.determineVersion("undefined");

        // then
        assertThat(gitVersionDetails).isNotNull()
                .satisfies(it -> assertSoftly(softly -> {
                    softly.assertThat(it.getDirectory()).isEqualTo(git.getRepository().getDirectory());
                    softly.assertThat(it.getCommit()).isEqualTo("0000000000000000000000000000000000000000");
                    softly.assertThat(it.getCommitRefType()).isEqualTo("commit");
                    softly.assertThat(it.getCommitRefName()).isEqualTo("0000000000000000000000000000000000000000");
                    softly.assertThat(it.getVersion()).isEqualTo("0000000000000000000000000000000000000000");
                }));


    }

    @Test
    void determineVersion_non_empty_repo() throws GitAPIException {

        // given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();
        RevCommit givenCommit = git.commit().setMessage("initial commit").setAllowEmpty(true).call();

        // when
        GitVersioning gitVersioning = new GitVersioning(git.getRepository().getDirectory());
        GitVersionDetails gitVersionDetails = gitVersioning.determineVersion("undefined");

        // then
        assertThat(gitVersionDetails).isNotNull()
                .satisfies(it -> assertSoftly(softly -> {
                    softly.assertThat(it.getDirectory()).isEqualTo(git.getRepository().getDirectory());
                    softly.assertThat(it.getCommit()).isEqualTo(givenCommit.getName());
                    softly.assertThat(it.getCommitRefType()).isEqualTo("commit");
                    softly.assertThat(it.getCommitRefName()).isEqualTo(givenCommit.getName());
                    softly.assertThat(it.getVersion()).isEqualTo(givenCommit.getName());
                }));


    }
}