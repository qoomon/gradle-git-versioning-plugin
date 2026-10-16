package me.qoomon.gitversioning.commons;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Read access to the git facts {@link GitSituation} is based on.
 */
public interface GitRepositoryReader {

    File getRootDirectory() throws IOException;

    String getRev() throws IOException;

    ZonedDateTime getTimestamp() throws IOException;

    /**
     * @return current branch or {@code null} if HEAD is detached
     */
    String getBranch() throws IOException;

    /**
     * @return tags pointing at HEAD
     */
    List<String> getTags() throws IOException;

    boolean isClean() throws IOException;

    GitDescription describe(Pattern tagPattern, boolean firstParent) throws IOException;
}
