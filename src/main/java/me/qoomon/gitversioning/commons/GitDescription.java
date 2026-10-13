package me.qoomon.gitversioning.commons;

import java.util.Objects;

public class GitDescription {
    private final String commit;
    private final String tag;
    private final int distance;

    public GitDescription(String commit, String tag, int distance) {
        this.commit = commit;
        this.tag = tag;
        this.distance = distance;
    }

    public String getCommit() {
        return commit;
    }

    public String getTag() {
        return tag;
    }

    public int getDistance() {
        return distance;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GitDescription that)) return false;
        return distance == that.distance && commit.equals(that.commit) && tag.equals(that.tag);
    }

    @Override
    public int hashCode() {
        return Objects.hash(commit, tag, distance);
    }

    @Override
    public String toString() {
        return tag + "-" + distance + "-g" + commit.substring(0,7);
    }
}
