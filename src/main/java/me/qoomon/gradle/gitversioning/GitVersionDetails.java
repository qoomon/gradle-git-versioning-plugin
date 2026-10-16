package me.qoomon.gradle.gitversioning;

import me.qoomon.gitversioning.commons.GitRefType;
import me.qoomon.gradle.gitversioning.GitVersioningPluginConfig.RefPatchDescription;

import java.util.Objects;

import static java.util.Objects.requireNonNull;

public final class GitVersionDetails {
    private final String commit;
    private final GitRefType refType;
    private final String refName;
    private final RefPatchDescription patchDescription;

    public GitVersionDetails(String commit, GitRefType refType, String refName, RefPatchDescription patchDescription) {

        this.commit = requireNonNull(commit);
        this.refType = requireNonNull(refType);
        this.refName = requireNonNull(refName);
        this.patchDescription = requireNonNull(patchDescription);
    }

    public String getCommit() {
        return commit;
    }

    public GitRefType getRefType() {
        return refType;
    }

    public String getRefName() {
        return refName;
    }

    public RefPatchDescription getPatchDescription() {
        return patchDescription;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) return true;
        if (obj == null || obj.getClass() != this.getClass()) return false;
        var that = (GitVersionDetails) obj;
        return Objects.equals(this.commit, that.commit) &&
                Objects.equals(this.refType, that.refType) &&
                Objects.equals(this.refName, that.refName) &&
                Objects.equals(this.patchDescription, that.patchDescription);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commit, refType, refName, patchDescription);
    }

    @Override
    public String toString() {
        return "GitVersionDetails[" +
                "commit=" + commit + ", " +
                "refType=" + refType + ", " +
                "refName=" + refName + ", " +
                "patchDescription=" + patchDescription + ']';
    }

}
