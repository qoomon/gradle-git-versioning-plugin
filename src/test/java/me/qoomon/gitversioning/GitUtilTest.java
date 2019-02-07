package me.qoomon.gitversioning;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.Status;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.Constants;
import org.eclipse.jgit.revwalk.RevCommit;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class GitUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void status_clean() throws GitAPIException {
        // given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();

        // when
        Status status = GitUtil.status(git.getRepository());

        // then
        assertThat(status.isClean()).isTrue();
    }

    @Test
    void status_dirty() throws GitAPIException, IOException {
        // given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();

        boolean dummyFileCreated = new File(tempDir.toFile(), "README.md").createNewFile();
        assertThat(dummyFileCreated).isTrue();

        // when
        Status status = GitUtil.status(git.getRepository());

        // then
        assertThat(status.isClean()).isFalse();
    }

    @Test
    void branch_empty_repo() throws GitAPIException {
        // given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();

        // when
        String branch = GitUtil.branch(git.getRepository());

        // then
        assertThat(branch).isEqualTo("master");
    }

    @Test
    void branch_non_empty_repo() throws GitAPIException {
        // given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();
        RevCommit givenCommit = git.commit().setMessage("initial commit").setAllowEmpty(true).call();
        String givenBranchName = "feature";
        git.branchCreate().setName(givenBranchName).setStartPoint(givenCommit).call();
        git.checkout().setName(givenBranchName).call();

        // when
        String branch = GitUtil.branch(git.getRepository());

        // then
        assertThat(branch).isEqualTo(givenBranchName);
    }

    @Test
    void tag_pointsAt_empty_repo() throws GitAPIException {
        // given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();

        // when
        List<String> tags = GitUtil.tag_pointsAt(git.getRepository(), Constants.HEAD);

        // then
        assertThat(tags).isEmpty();
    }

    @Test
    void tag_pointsAt_no_tags() throws GitAPIException {
        // given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();

        git.commit().setMessage("initial commit").setAllowEmpty(true).call();

        // when
        List<String> tags = GitUtil.tag_pointsAt(git.getRepository(), Constants.HEAD);

        // then
        assertThat(tags).isEmpty();
    }

    @Test
    void tag_pointsAt_one_tags() throws GitAPIException {
        // given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();

        RevCommit givenCommit = git.commit().setMessage("initial commit").setAllowEmpty(true).call();
        String givenTagName = "v1.0.0";
        git.tag().setName(givenTagName).setObjectId(givenCommit).call();

        // when
        List<String> tags = GitUtil.tag_pointsAt(git.getRepository(), Constants.HEAD);

        // then
        assertThat(tags).containsExactly(givenTagName);
    }

    @Test
    void tag_pointsAt_multiple_tags() throws GitAPIException {
        // given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();

        RevCommit givenCommit = git.commit().setMessage("initial commit").setAllowEmpty(true).call();
        String givenTagName1 = "111";
        git.tag().setName(givenTagName1).setObjectId(givenCommit).call();
        String givenTagName2 = "222";
        git.tag().setName(givenTagName2).setObjectId(givenCommit).call();
        String givenTagName3 = "333";
        git.tag().setName(givenTagName3).setObjectId(givenCommit).call();

        // when
        List<String> tags = GitUtil.tag_pointsAt(git.getRepository(), Constants.HEAD);

        // then
        assertThat(tags).containsExactlyInAnyOrder(givenTagName1, givenTagName2, givenTagName3);
    }

    @Test
    void revParse_empty_repo() throws GitAPIException {
        // given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();

        // when
        String ref = GitUtil.revParse(git.getRepository(), Constants.HEAD);

        // then
        assertThat(ref).isEqualTo("0000000000000000000000000000000000000000");
    }

    @Test
    void revParse_non_empty_repo() throws GitAPIException {
        // given
        Git git = Git.init().setDirectory(tempDir.toFile()).call();

        RevCommit givenCommit = git.commit().setMessage("initial commit").setAllowEmpty(true).call();

        // when
        String ref = GitUtil.revParse(git.getRepository(), Constants.HEAD);

        // then
        assertThat(ref).isEqualTo(givenCommit.name());
    }
}