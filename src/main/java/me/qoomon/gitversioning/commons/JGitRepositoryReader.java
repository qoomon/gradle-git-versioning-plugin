package me.qoomon.gitversioning.commons;

import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.regex.Pattern;

import static java.time.Instant.EPOCH;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyList;
import static me.qoomon.gitversioning.commons.GitUtil.*;

public class JGitRepositoryReader implements GitRepositoryReader {

    private final Repository repository;
    private final File rootDirectory;
    private final ObjectId head;

    public JGitRepositoryReader(Repository repository) throws IOException {
        this.repository = repository;
        this.rootDirectory = worktreesFix_getWorkTree(repository);
        this.head = worktreesFix_resolveHead(repository);
    }

    @Override
    public File getRootDirectory() {
        return rootDirectory;
    }

    @Override
    public String getRev() {
        return head != null ? head.getName() : NO_COMMIT;
    }

    @Override
    public ZonedDateTime getTimestamp() throws IOException {
        return head != null
                ? GitUtil.revTimestamp(repository, head)
                : ZonedDateTime.ofInstant(EPOCH, UTC);
    }

    @Override
    public String getBranch() throws IOException {
        return GitUtil.branch(repository);
    }

    @Override
    public List<String> getTags() throws IOException {
        return head != null ? GitUtil.tagsPointAt(head, repository) : emptyList();
    }

    @Override
    public boolean isClean() throws IOException {
        try {
            return GitUtil.status(repository).isClean();
        } catch (GitAPIException e) {
            throw new IOException(e);
        }
    }

    @Override
    public GitDescription describe(Pattern tagPattern, boolean firstParent) throws IOException {
        return GitUtil.describe(head, tagPattern, repository, firstParent);
    }
}
