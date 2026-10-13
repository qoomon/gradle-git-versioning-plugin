package me.qoomon.gradle.gitversioning;

import me.qoomon.gitversioning.commons.GitRefType;
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.jspecify.annotations.Nullable;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static me.qoomon.gitversioning.commons.GitRefType.BRANCH;
import static me.qoomon.gitversioning.commons.GitRefType.TAG;

public class GitVersioningPluginConfig {

    @Inject
    protected @Nullable ObjectFactory getObjectFactory() {
        return null;
    }

    public Boolean disable = false;

    public @Nullable String projectVersionPattern = null;

    public @Nullable Pattern projectVersionPattern() {
        return projectVersionPattern != null
                ? Pattern.compile(projectVersionPattern)
                : null;
    }

    public @Nullable String describeTagPattern = null;
    public Boolean describeTagFirstParent = true;

    public @Nullable Boolean updateGradleProperties;

    public final RefPatchDescriptionList refs = getObjectFactory() != null
            ? getObjectFactory().newInstance(RefPatchDescriptionList.class)
            : new RefPatchDescriptionList();

    public @Nullable PatchDescription rev;

    public void refs(Action<RefPatchDescriptionList> action) {
        action.execute(this.refs);
    }

    public void rev(Action<PatchDescription> action) {
        this.rev = getObjectFactory() != null
                ? getObjectFactory().newInstance(PatchDescription.class)
                : new PatchDescription();
        action.execute(this.rev);
    }

    public static class PatchDescription {

        public @Nullable String describeTagPattern;
        public @Nullable Boolean describeTagFirstParent = null;

        public @Nullable Pattern getDescribeTagPattern() {
            return describeTagPattern != null
                    ? Pattern.compile(describeTagPattern)
                    : null;
        }

        public @Nullable Boolean updateGradleProperties;

        public @Nullable String version;

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
        /**
         * {@code null} matches any ref
         */
        public final @Nullable Pattern pattern;

        public RefPatchDescription(GitRefType type, @Nullable Pattern pattern) {
            this.type = type;
            this.pattern = pattern;
        }

        public RefPatchDescription(GitRefType type, @Nullable Pattern pattern, PatchDescription patch) {
            this(type, pattern);
            this.describeTagPattern = patch.describeTagPattern;
            this.describeTagFirstParent = patch.describeTagFirstParent;
            this.updateGradleProperties = patch.updateGradleProperties;
            this.version = patch.version;
            this.properties = patch.properties;
        }
    }

    public static class RefPatchDescriptionList {

        public boolean considerTagsOnBranches = false;

        public List<RefPatchDescription> list = new ArrayList<>();

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
