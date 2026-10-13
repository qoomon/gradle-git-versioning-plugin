package me.qoomon.gradle.gitversioning.git;

import org.jspecify.annotations.Nullable;

import java.io.File;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.Objects;

import static java.time.ZoneOffset.UTC;

/**
 * The HEAD commit and branch of a git repository.
 */
public final class GitHead {

    private final File rootDirectory;
    private final String rev;
    private final @Nullable String branch;
    private final long timestamp;

    public GitHead(File rootDirectory, String rev, @Nullable String branch, ZonedDateTime timestamp) {
        this.rootDirectory = rootDirectory;
        this.rev = rev;
        this.branch = branch;
        this.timestamp = timestamp.toEpochSecond();
    }

    public File getRootDirectory() {
        return rootDirectory;
    }

    public String getRev() {
        return rev;
    }

    /**
     * @return current branch or {@code null} if HEAD is detached
     */
    public @Nullable String getBranch() {
        return branch;
    }

    public ZonedDateTime getTimestamp() {
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(timestamp), UTC);
    }

    @Override
    public boolean equals(@Nullable Object o) {
        if (this == o) return true;
        if (!(o instanceof GitHead)) return false;
        GitHead that = (GitHead) o;
        return timestamp == that.timestamp
                && rootDirectory.equals(that.rootDirectory)
                && rev.equals(that.rev)
                && Objects.equals(branch, that.branch);
    }

    @Override
    public int hashCode() {
        return Objects.hash(rootDirectory, rev, branch, timestamp);
    }
}
