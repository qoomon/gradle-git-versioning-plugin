package me.qoomon.gitversioning;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;

import static me.qoomon.gitversioning.GitUtil.NO_COMMIT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.SoftAssertions.assertSoftly;
import static org.eclipse.jgit.lib.Constants.MASTER;

class GitRepoDataTest {

    @TempDir
    Path tempDir;


    @Test
    void get_emptyRepo() throws GitAPIException {

        // Given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();

        // When
        GitRepoData gitRepoData = GitRepoData.get(git.getRepository().getDirectory());

        // Then
        assertThat(gitRepoData).satisfies(it -> assertSoftly(softly -> {
            softly.assertThat(it.isClean()).isTrue();
            softly.assertThat(it.getCommit()).isEqualTo(NO_COMMIT);
            softly.assertThat(it.getBranch()).isEqualTo(MASTER);
            softly.assertThat(it.getTags()).isEmpty();
        }));
    }

    @Test
    void get_onBranch() throws GitAPIException {

        // Given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();
        RevCommit givenCommit = git.commit().setMessage("init").setAllowEmpty(true).call();

        // When
        GitRepoData gitRepoData = GitRepoData.get(git.getRepository().getDirectory());

        // Then
        assertThat(gitRepoData).satisfies(it -> assertSoftly(softly -> {
            softly.assertThat(it.isClean()).isTrue();
            softly.assertThat(it.getCommit()).isEqualTo(givenCommit.getName());
            softly.assertThat(it.getBranch()).isEqualTo(MASTER);
            softly.assertThat(it.getTags()).isEmpty();
        }));
    }

    @Test
    void get_onBranchWithTag() throws GitAPIException {

        // Given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();
        RevCommit givenCommit = git.commit().setMessage("init").setAllowEmpty(true).call();
        String givenTag = "v1";
        git.tag().setName(givenTag).setObjectId(givenCommit).call();

        // When
        GitRepoData gitRepoData = GitRepoData.get(git.getRepository().getDirectory());

        // Then
        assertThat(gitRepoData).satisfies(it -> assertSoftly(softly -> {
            softly.assertThat(it.isClean()).isTrue();
            softly.assertThat(it.getCommit()).isEqualTo(givenCommit.getName());
            softly.assertThat(it.getBranch()).isEqualTo(MASTER);
            softly.assertThat(it.getTags()).containsExactly(givenTag);
        }));
    }

    @Test
    void get_detachedHead() throws GitAPIException {

        // Given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();
        RevCommit givenCommit = git.commit().setMessage("init").setAllowEmpty(true).call();
        git.checkout().setName(givenCommit.getName()).call();

        // When
        GitRepoData gitRepoData = GitRepoData.get(git.getRepository().getDirectory());

        // Then
        assertThat(gitRepoData).satisfies(it -> assertSoftly(softly -> {
            softly.assertThat(it.isClean()).isTrue();
            softly.assertThat(it.getCommit()).isEqualTo(givenCommit.getName());
            softly.assertThat(it.getBranch()).isNull();
            softly.assertThat(it.getTags()).isEmpty();
        }));
    }

    @Test
    void get_detachedHeadWithTag() throws GitAPIException {

        // Given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();
        RevCommit givenCommit = git.commit().setMessage("init").setAllowEmpty(true).call();
        String givenTag = "v1";
        git.tag().setName(givenTag).setObjectId(givenCommit).call();
        git.checkout().setName(givenTag).call();

        // When
        GitRepoData gitRepoData = GitRepoData.get(git.getRepository().getDirectory());

        // Then
        assertThat(gitRepoData).satisfies(it -> assertSoftly(softly -> {
            softly.assertThat(it.isClean()).isTrue();
            softly.assertThat(it.getCommit()).isEqualTo(givenCommit.getName());
            softly.assertThat(it.getBranch()).isNull();
            softly.assertThat(it.getTags()).containsExactly(givenTag);
        }));
    }
}