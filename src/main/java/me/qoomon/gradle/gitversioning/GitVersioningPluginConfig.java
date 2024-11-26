package me.qoomon.gradle.gitversioning;

import me.qoomon.gitversioning.commons.GitRefType;
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static me.qoomon.gitversioning.commons.GitRefType.BRANCH;
import static me.qoomon.gitversioning.commons.GitRefType.TAG;

public class GitVersioningPluginConfig {

    private static final Pattern MATCH_ALL = Pattern.compile(".*");

    @Inject
    protected ObjectFactory getObjectFactory() {
        return null;
    }

    public Boolean disable = false;

    public String projectVersionPattern = null;

    public Pattern projectVersionPattern() {
        return projectVersionPattern != null
                ? Pattern.compile(projectVersionPattern)
                : null;
    }

    public String describeTagPattern = null;
    public Boolean describeTagFirstParent = true;

    public Boolean updateGradleProperties;

    public final RefPatchDescriptionList refs = getObjectFactory() != null
            ? getObjectFactory().newInstance(RefPatchDescriptionList.class)
            : new RefPatchDescriptionList();

    public PatchDescription rev;

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

        public String describeTagPattern;
        public Boolean describeTagFirstParent = null;

        public Pattern getDescribeTagPattern() {
            return Pattern.compile(describeTagPattern);
        }

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

    public String gitDir = null;
}
