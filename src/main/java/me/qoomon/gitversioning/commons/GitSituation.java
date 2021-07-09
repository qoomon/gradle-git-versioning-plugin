package me.qoomon.gitversioning.commons;

import org.eclipse.jgit.lib.ObjectId;
import org.eclipse.jgit.lib.Ref;
import org.eclipse.jgit.lib.Repository;

import java.io.File;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static java.time.Instant.EPOCH;
import static java.time.ZoneOffset.UTC;
import static java.util.Collections.emptyList;
import static me.qoomon.gitversioning.commons.GitUtil.NO_COMMIT;
import static org.eclipse.jgit.lib.Constants.HEAD;

//TODO rename to GitSituation
public class GitSituation {

    private final Repository repository;
    private final File rootDirectory;

    private final ObjectId head;
    private final String hash;
    private final Lazy<ZonedDateTime> timestamp = Lazy.of(this::timestamp);
    private Lazy<String> branch = Lazy.of(this::branch);

    private final Lazy<Map<ObjectId, List<Ref>>> reverseTagRefMap = Lazy.of(this::reverseTagRefMap);
    private Lazy<List<String>> tags = Lazy.of(this::tags);

    private final Lazy<Boolean> clean = Lazy.of(this::clean);

    private final Pattern describeTagPattern;
    private final Lazy<GitDescription> description = Lazy.of(this::describe);

    public GitSituation(Repository repository, Pattern describeTagPattern) throws IOException {
        this.repository = repository;
        this.rootDirectory = repository.getWorkTree();
        this.head = repository.resolve(HEAD);
        this.hash = head != null ? head.getName() : NO_COMMIT;
        this.describeTagPattern = describeTagPattern != null ? describeTagPattern : Pattern.compile(".*");
    }

    public GitSituation(Repository repository) throws IOException {
        this(repository, null);
    }

    public File getRootDirectory() {
        return rootDirectory;
    }

    public String getHash() {
        return hash;
    }

    public ZonedDateTime getTimestamp() {
        return timestamp.get();
    }

    public String getBranch() {
        return branch.get();
    }

    public void setBranch(String branch) {
        this.branch = Lazy.of(branch);
    }

    public boolean isDetached() {
        return branch.get() == null;
    }

    public List<String> getTags() {
        return tags.get();
    }

    public void setTags(List<String> tags) {
        this.tags = Lazy.of(tags);
    }

    public boolean isClean() {
        return clean.get();
    }

    public GitDescription getDescription() {
        return description.get();
    }

    // ----- initialization methods ------------------------------------------------------------------------------------

    private ZonedDateTime timestamp() throws IOException {
        return head != null
                ? GitUtil.revTimestamp(repository, head)
                : ZonedDateTime.ofInstant(EPOCH, UTC);
    }

    private String branch() throws IOException {
        return GitUtil.branch(repository);
    }

    private List<String> tags() {
        return head != null ? GitUtil.tagsPointAt(repository, head, reverseTagRefMap.get()) : emptyList();
    }

    private boolean clean() {
        return GitUtil.status(repository).isClean();
    }

    private GitDescription describe() throws IOException {
        return GitUtil.describe(repository, head, describeTagPattern, reverseTagRefMap.get());
    }

    private Map<ObjectId, List<Ref>> reverseTagRefMap() throws IOException {
        return GitUtil.reverseTagRefMap(repository);
    }
}