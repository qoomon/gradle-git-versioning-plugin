package me.qoomon.gradle.gitversioning;

import groovy.lang.Closure;
import me.qoomon.gitversioning.commons.GitRefType;
import org.gradle.api.Action;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static me.qoomon.gitversioning.commons.GitRefType.*;
import static org.gradle.util.ConfigureUtil.configure;

public class GitVersioningPluginConfig {

    private static final Pattern MATCH_ALL = Pattern.compile(".*");

    public Boolean disable = false;

    public Pattern describeTagPattern = MATCH_ALL;

    public Boolean updateGradleProperties;

    public RefPatchDescriptionList refs = new RefPatchDescriptionList();

    public PatchDescription rev;

    // groovy support
    public void refs(Closure<RefPatchDescriptionList> closure) {
        this.refs = configure(closure, new RefPatchDescriptionList());
    }

    public void rev(Closure<PatchDescription> closure) {
        this.rev = configure(closure, new PatchDescription());
    }

    // kotlin support
    public void refs(Action<RefPatchDescriptionList> action) {
        this.refs = new RefPatchDescriptionList();
        action.execute(this.refs);
    }

    public void rev(Action<PatchDescription> action) {
        this.rev = new PatchDescription();
        action.execute(this.rev);
    }


    public static class PatchDescription {

        public Pattern describeTagPattern;
        public Boolean updateGradleProperties;

        public String version;

        public Map<String, String> properties = new HashMap<>();
    }

    public static class RefPatchDescription extends PatchDescription {

        public final GitRefType type;
        public final Pattern pattern;

        public RefPatchDescription(GitRefType type, Pattern pattern) {
            this.type = type;
            this.pattern = pattern;
        }

        public RefPatchDescription(GitRefType type, Pattern pattern, PatchDescription description) {
            this(type, pattern);
            this.describeTagPattern = description.describeTagPattern;
            this.updateGradleProperties = description.updateGradleProperties;
            this.version = description.version;
            this.properties = new HashMap<>(description.properties);
        }
    }

    public static class RefPatchDescriptionList {

        public boolean considerTagsOnBranches = false;

        public List<RefPatchDescription> list = new ArrayList<>();

        // groovy support
        public void branch(String pattern, Closure<RefPatchDescription> closure) {
            this.list.add(configure(closure, new RefPatchDescription(BRANCH, Pattern.compile(pattern))));
        }

        public void tag(String pattern, Closure<RefPatchDescription> closure) {
            this.list.add(configure(closure, new RefPatchDescription(TAG, Pattern.compile(pattern))));
        }

        // kotlin support
        public void branch(String pattern, Action<RefPatchDescription> action) {
            RefPatchDescription ref = new RefPatchDescription(BRANCH, Pattern.compile(pattern));
            this.list.add(ref);
            action.execute(ref);
        }

        public void tag(String pattern, Action<RefPatchDescription> action) {
            RefPatchDescription ref = new RefPatchDescription(TAG, Pattern.compile(pattern));
            this.list.add(ref);
            action.execute(ref);
        }
    }
}
