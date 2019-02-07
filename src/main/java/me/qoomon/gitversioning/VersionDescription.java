package me.qoomon.gitversioning;

/**
 * Created by qoomon on 26/11/2016.
 */
public class VersionDescription {

    private String pattern;

    private String prefix;

    private String versionFormat;

    public VersionDescription() {
        this(null, null, null);
    }

    public VersionDescription(String pattern, String prefix, String versionFormat) {
        setPattern(pattern);
        setPrefix(prefix);
        setVersionFormat(versionFormat);
    }

    public String getPattern() {
        return pattern;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getVersionFormat() {
        return versionFormat;
    }

    public void setPattern(final String pattern) {
        this.pattern = pattern != null ? pattern : ".*";
    }

    public void setPrefix(final String prefix) {
        this.prefix = prefix != null ? prefix : "";
    }

    public void setVersionFormat(final String versionFormat) {
        this.versionFormat = versionFormat != null ? versionFormat : "${commit}";
    }
}

