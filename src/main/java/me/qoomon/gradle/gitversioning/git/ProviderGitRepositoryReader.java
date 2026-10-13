package me.qoomon.gradle.gitversioning.git;

import me.qoomon.gitversioning.commons.GitDescription;
import me.qoomon.gitversioning.commons.GitRepositoryReader;
import org.gradle.api.provider.Provider;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.provider.ValueSource;
import org.jspecify.annotations.Nullable;

import java.io.File;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Reads git facts through configuration cache compatible {@link ValueSource}s.
 * <p>
 * Each fact is obtained from its own value source, so only the facts actually used
 * become configuration cache inputs, e.g. the working tree status is only checked if a format uses it.
 */
public class ProviderGitRepositoryReader implements GitRepositoryReader {

    private final ProviderFactory providers;
    private final File projectDirectory;
    private final Provider<GitHead> head;

    public ProviderGitRepositoryReader(ProviderFactory providers, File projectDirectory) {
        this.providers = providers;
        this.projectDirectory = projectDirectory;
        this.head = providers.of(GitHeadValueSource.class, spec ->
                spec.getParameters().getProjectDirectory().set(projectDirectory));
    }

    public boolean isGitRepository() {
        return head.isPresent();
    }

    @Override
    public File getRootDirectory() {
        return head.get().getRootDirectory();
    }

    @Override
    public String getRev() {
        return head.get().getRev();
    }

    @Override
    public ZonedDateTime getTimestamp() {
        return head.get().getTimestamp();
    }

    @Override
    public @Nullable String getBranch() {
        return head.get().getBranch();
    }

    @Override
    public List<String> getTags() {
        return providers.of(GitTagsValueSource.class, spec ->
                spec.getParameters().getProjectDirectory().set(projectDirectory)).get();
    }

    @Override
    public boolean isClean() {
        return providers.of(GitStatusValueSource.class, spec ->
                spec.getParameters().getProjectDirectory().set(projectDirectory)).get();
    }

    @Override
    public GitDescription describe(Pattern tagPattern, boolean firstParent) {
        return providers.of(GitDescribeValueSource.class, spec -> {
            spec.getParameters().getProjectDirectory().set(projectDirectory);
            spec.getParameters().getTagPattern().set(tagPattern.pattern());
            spec.getParameters().getFirstParent().set(firstParent);
        }).get();
    }
}
