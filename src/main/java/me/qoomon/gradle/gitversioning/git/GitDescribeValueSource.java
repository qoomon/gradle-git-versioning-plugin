package me.qoomon.gradle.gitversioning.git;

import me.qoomon.gitversioning.commons.GitDescription;
import me.qoomon.gitversioning.commons.JGitRepositoryReader;
import org.gradle.api.provider.Property;

import java.io.IOException;
import java.util.regex.Pattern;

public abstract class GitDescribeValueSource extends GitRepositoryValueSource<GitDescription, GitDescribeValueSource.Parameters> {

    public interface Parameters extends GitRepositoryValueSource.Parameters {
        Property<String> getTagPattern();

        Property<Boolean> getFirstParent();
    }

    @Override
    protected GitDescription read(JGitRepositoryReader reader) throws IOException {
        return reader.describe(Pattern.compile(getParameters().getTagPattern().get()), getParameters().getFirstParent().get());
    }

    @Override
    protected String describeValue() {
        return "git describe (tag pattern: " + getParameters().getTagPattern().get() + ")";
    }
}
