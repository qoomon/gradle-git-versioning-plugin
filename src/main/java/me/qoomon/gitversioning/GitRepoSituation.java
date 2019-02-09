package me.qoomon.gitversioning;

import java.util.List;

import static java.util.Collections.emptyList;
import static java.util.Objects.requireNonNull;
import static me.qoomon.gitversioning.GitConstants.NO_COMMIT;

public class GitRepoSituation {

    private boolean clean;
    private String commit;
    private String branch;
    private List<String> tags;

    public GitRepoSituation(){
        this(true, NO_COMMIT, null, emptyList());
    }

    public GitRepoSituation(boolean clean, String commit, String branch, List<String> tags) {
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
        if (commit.length() != 40){
            throw new IllegalArgumentException("commit sha-1 hash must contains of 40 hex characters");
        }
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
}