package me.qoomon.gradle.gitversioning;

/**
 * Created by qoomon on 26/11/2016.
 */
public class VersionFormatDescription {

    public String pattern = ".*";

    public String prefix = "";

    public String versionFormat = "${commit}";

    public VersionFormatDescription() {
    }

    public VersionFormatDescription(String pattern, String prefix, String versionFormat) {
        this.pattern = pattern;
        this.prefix = prefix;
        this.versionFormat = versionFormat;
    }
}

