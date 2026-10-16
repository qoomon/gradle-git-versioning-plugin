package me.qoomon.gradle.gitversioning.git;

import me.qoomon.gitversioning.commons.JGitRepositoryReader;

import java.io.IOException;

/**
 * Whether the working tree is clean.
 */
public abstract class GitStatusValueSource extends GitRepositoryValueSource<Boolean, GitRepositoryValueSource.Parameters> {

    @Override
    protected Boolean read(JGitRepositoryReader reader) throws IOException {
        return reader.isClean();
    }

    @Override
    protected String describeValue() {
        return "git working tree status";
    }
}
