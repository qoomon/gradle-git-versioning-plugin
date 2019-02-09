package me.qoomon.gitversioning;

import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;

import java.io.File;
import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static me.qoomon.UncheckedExceptions.unchecked;
import static me.qoomon.gitversioning.GitUtil.NO_COMMIT;
import static org.eclipse.jgit.lib.Constants.HEAD;

public class GitRepoData {

    private boolean clean;
    private String commit;
    private String branch;
    private List<String> tags;

    public GitRepoData(){
        this(true, NO_COMMIT, null, emptyList());
    }

    public GitRepoData(boolean clean, String commit, String branch, List<String> tags) {
        setClean(clean);
        setCommit(commit);
        setBranch(branch);
        setTags(tags);
    }

    public boolean isClean() {
        return clean;
    }

    public void setClean(boolean clean) {
        this.clean = clean;
    }

    public String getCommit() {
        return commit;
    }

    public void setCommit(String commit) {
        this.commit = requireNonNull(commit);
    }

    public String getBranch() {
        return branch;
    }

    public void setBranch(String branch) {
        this.branch = branch;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = requireNonNull(tags);
    }

    public static GitRepoData get(File directory) {
        FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder().findGitDir(directory);
        if (repositoryBuilder.getGitDir() == null) {
            throw new IllegalArgumentException(
                    directory + " directory is not a git repository (or any of the parent directories)");
        }
        try (Repository repository = unchecked(repositoryBuilder::build)) {
            boolean headClean = GitUtil.status(repository).isClean();
            String headCommit = GitUtil.revParse(repository, HEAD);
            String headBranch = GitUtil.branch(repository);
            List<String> headTags = GitUtil.tag_pointsAt(repository, HEAD);
            return new GitRepoData(headClean, headCommit, headBranch, headTags);
        }
    }
}