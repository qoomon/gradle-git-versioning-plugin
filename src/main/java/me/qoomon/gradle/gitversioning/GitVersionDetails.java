package me.qoomon.gradle.gitversioning;

import me.qoomon.gitversioning.commons.GitRefType;
import me.qoomon.gradle.gitversioning.GitVersioningPluginConfig.RefPatchDescription;

import static java.util.Objects.requireNonNull;

public record GitVersionDetails(String commit, GitRefType refType, String refName,
                                RefPatchDescription patchDescription) {
    public GitVersionDetails(String commit, GitRefType refType, String refName, RefPatchDescription patchDescription) {

        this.commit = requireNonNull(commit);
        this.refType = requireNonNull(refType);
        this.refName = requireNonNull(refName);
        this.patchDescription = requireNonNull(patchDescription);
    }
}
