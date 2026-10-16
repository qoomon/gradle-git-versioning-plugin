package me.qoomon.gradle.gitversioning.git;

import me.qoomon.gitversioning.commons.JGitRepositoryReader;

import java.io.IOException;

public abstract class GitHeadValueSource extends GitRepositoryValueSource<GitHead, GitRepositoryValueSource.Parameters> {

    @Override
    protected GitHead read(JGitRepositoryReader reader) throws IOException {
        return new GitHead(reader.getRootDirectory(), reader.getRev(), reader.getBranch(), reader.getTimestamp());
    }

    @Override
    protected String describeValue() {
        return "git HEAD";
    }
}
