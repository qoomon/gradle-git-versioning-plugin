package me.qoomon.gradle.gitversioning;

import groovy.lang.Closure;
import me.qoomon.gitversioning.commons.GitRefType;
import org.gradle.api.Action;

import java.util.*;
import java.util.regex.Pattern;

import static me.qoomon.gitversioning.commons.GitRefType.*;
import static org.gradle.util.internal.ConfigureUtil.configure;

public class GitVersioningPluginConfig {

    private static final Pattern MATCH_ALL = Pattern.compile(".*");

    public Boolean disable = false;

    public Pattern describeTagPattern = MATCH_ALL;

    public Boolean updateGradleProperties;

    public RefPatchDescriptionList refs = new RefPatchDescriptionList();

    public PatchDescription rev;

    // groovy support
    public void refs(Closure<RefPatchDescriptionList> closure) {
        configure(closure, this.refs);
    }

    public void rev(Closure<PatchDescription> closure) {
        this.rev = new PatchDescription();
        configure(closure, this.rev);
    }

    // kotlin support
    public void refs(Action<RefPatchDescriptionList> action) {
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

        // WORKAROUND Groovy MetaClass API properties field name conflict
        public Map<String, String> getProperties_() {
            return properties;
        }
        // WORKAROUND Groovy MetaClass properties API field name conflict
        public void setProperties_(Map<String, String> properties) {
            this.properties = properties;
        }
    }

    public static class RefPatchDescription extends PatchDescription {

        public final GitRefType type;
        public final Pattern pattern;

        public RefPatchDescription(GitRefType type, Pattern pattern) {
            this.type = type;
            this.pattern = pattern;
        }

        public RefPatchDescription(GitRefType type, Pattern pattern, PatchDescription patch) {
            this(type, pattern);
            this.describeTagPattern = patch.describeTagPattern;
            this.updateGradleProperties = patch.updateGradleProperties;
            this.version = patch.version;
            this.properties = patch.properties;
        }
    }

    public static class RefPatchDescriptionList {

        public boolean considerTagsOnBranches = false;

        public List<RefPatchDescription> list = new ArrayList<>();

        // groovy support
        public void branch(String pattern, Closure<RefPatchDescription> closure) {
            RefPatchDescription ref = new RefPatchDescription(BRANCH, Pattern.compile(pattern));
            configure(closure, ref);
            this.list.add(ref);
        }

        public void tag(String pattern, Closure<RefPatchDescription> closure) {
            RefPatchDescription ref = new RefPatchDescription(TAG, Pattern.compile(pattern));
            configure(closure, ref);
            this.list.add(ref);
        }

        // kotlin support
        public void branch(String pattern, Action<RefPatchDescription> action) {
            RefPatchDescription ref = new RefPatchDescription(BRANCH, Pattern.compile(pattern));
            action.execute(ref);
            this.list.add(ref);
        }

        public void tag(String pattern, Action<RefPatchDescription> action) {
            RefPatchDescription ref = new RefPatchDescription(TAG, Pattern.compile(pattern));
            action.execute(ref);
            this.list.add(ref);
        }
    }
}
