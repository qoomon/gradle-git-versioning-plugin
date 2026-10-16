package me.qoomon.gitversioning.commons;

import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;
import java.util.regex.Pattern;

import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toList;

public class GitSituation {

    private final GitRepositoryReader reader;
    private final File rootDirectory;

    private final String rev;
    private final Supplier<ZonedDateTime> timestamp = Lazy.by(this::timestamp);
    private Supplier<String> branch = Lazy.by(this::branch);

    private Supplier<List<String>> tags = Lazy.by(this::tags);

    private final Supplier<Boolean> clean = Lazy.by(this::clean);

    private Pattern describeTagPattern = Pattern.compile(".*");

    private boolean firstParent = true;

    private Supplier<GitDescription> description = Lazy.by(this::describe);

    public GitSituation(Repository repository) throws IOException {
        this(new JGitRepositoryReader(repository));
    }

    public GitSituation(GitRepositoryReader reader) throws IOException {
        this.reader = reader;
        this.rootDirectory = reader.getRootDirectory();
        this.rev = reader.getRev();
    }

    public File getRootDirectory() {
        return rootDirectory;
    }

    public String getRev() {
        return rev;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp.get();
    }

    public String getBranch() {
        return branch.get();
    }

    protected void setBranch(String branch) {
        if (branch != null) {
            if (branch.startsWith("refs/tags/")) {
                throw new IllegalArgumentException("invalid branch ref" + branch);
            }
            branch = branch
                    // support default branches (heads)
                    .replaceFirst("^refs/heads/", "")
                    // support other refs e.g. GitHub pull requests refs/pull/1000/head
                    .replaceFirst("^refs/", "");
        }
        final String finalBranch = branch;
        this.branch = () -> finalBranch;
    }

    public boolean isDetached() {
        return branch.get() == null;
    }

    public List<String> getTags() {
        return tags.get();
    }

    protected void addTag(String tag) {
        requireNonNull(tag);

        if (tag.startsWith("refs/") && !tag.startsWith("refs/tags/")) {
            throw new IllegalArgumentException("invalid tag ref" + tag);
        }

        final String finalTag = tag.replaceFirst("^refs/tags/", "");

        final Supplier<List<String>> currentTags = this.tags;
        this.tags = Lazy.by(() -> {
            List<String> tags = new ArrayList<>(currentTags.get());
            tags.add(finalTag);
            return tags;
        });
    }

    protected void setTags(List<String> tags) {
        requireNonNull(tags);
        tags.forEach(tag -> {
            requireNonNull(tag);
            if (tag.startsWith("refs/") && !tag.startsWith("refs/tags/")) {
                throw new IllegalArgumentException("invalid tag ref" + tag);
            }
        });

        tags = tags.stream()
                .map(tag -> tag.replaceFirst("^refs/tags/", ""))
                .collect(toList());

        final List<String> finalTags = tags;
        this.tags = () -> finalTags;
    }

    public boolean isClean() {
        return clean.get();
    }

    public void setDescribeTagPattern(Pattern describeTagPattern) {
        this.describeTagPattern = requireNonNull(describeTagPattern);
        this.description = Lazy.by(this::describe);
    }

    public Pattern getDescribeTagPattern() {
        return describeTagPattern;
    }

    public boolean isFirstParent() {
        return firstParent;
    }

    public void setFirstParent(boolean firstParent) {
        this.firstParent = firstParent;
        this.description = Lazy.by(this::describe);
    }

    public GitDescription getDescription() {
        return description.get();
    }

    // ----- initialization methods ------------------------------------------------------------------------------------

    private ZonedDateTime timestamp() throws IOException {
        return reader.getTimestamp();
    }

    private String branch() throws IOException {
        return reader.getBranch();
    }

    private List<String> tags() throws IOException {
        return reader.getTags();
    }

    private boolean clean() throws IOException {
        return reader.isClean();
    }

    private GitDescription describe() throws IOException {
        return reader.describe(describeTagPattern, firstParent);
    }
}