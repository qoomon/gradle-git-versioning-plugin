package me.qoomon.gitversioning;

import java.io.File;

public class GitVersionDetails {

    private File directory;
    private boolean clean;
    private final String commit;
    private final String commitRefType;
    private final String commitRefName;
    private final String version;

    public GitVersionDetails(final File directory, final boolean clean,
                             final String commit, final String commitRefType, final String commitRefName,
                             final String version) {
        this.directory = directory;
        this.clean = clean;
        this.version = version;
        this.commit = commit;
        this.commitRefType = commitRefType;
        this.commitRefName = commitRefName;
    }

    public File getDirectory() {
        return directory;
    }

    public boolean isClean() {
        return clean;
    }

    public String getVersion() {
        return version;
    }

    public String getCommit() {
        return commit;
    }

    public String getCommitRefType() {
        return commitRefType;
    }

    public String getCommitRefName() {
        return commitRefName;
    }
}
