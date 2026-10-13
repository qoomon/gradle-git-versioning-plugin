package me.qoomon.gradle.gitversioning.git;

import me.qoomon.gitversioning.commons.GitRepositoryReader;

import java.io.IOException;
import java.util.List;

public abstract class GitTagsValueSource extends GitRepositoryValueSource<List<String>, GitRepositoryValueSource.Parameters> {

    @Override
    protected List<String> read(GitRepositoryReader reader) throws IOException {
        return List.copyOf(reader.getTags());
    }

    @Override
    protected String describeValue() {
        return "git tags pointing at HEAD";
    }
}
