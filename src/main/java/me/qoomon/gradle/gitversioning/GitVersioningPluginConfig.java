package me.qoomon.gradle.gitversioning;

import static me.qoomon.gitversioning.commons.GitRefType.BRANCH;
import static me.qoomon.gitversioning.commons.GitRefType.TAG;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import javax.inject.Inject;
import me.qoomon.gitversioning.commons.GitRefType;
import me.qoomon.gitversioning.commons.GitSituation;
import me.qoomon.gitversioning.commons.Lazy;
import org.gradle.api.Action;
import org.gradle.api.model.ObjectFactory;
import org.jetbrains.annotations.Nullable;

public class GitVersioningPluginConfig {

    private static final Pattern MATCH_ALL = Pattern.compile(".*");

    @Inject
    protected ObjectFactory getObjectFactory() {
        return null;
    }

    public Boolean disable = false;

    public Pattern describeTagPattern = MATCH_ALL;

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
        @Nullable
        public final Pattern pattern;
        @Nullable
        private final RefPatchDescriptionProvider provider;

        public RefPatchDescription(
            GitRefType type,
            @Nullable
            Pattern pattern,
            @Nullable
            RefPatchDescriptionProvider provider
        ) {
            this.type = type;
            this.pattern = pattern;
            this.provider = provider;
        }

        public RefPatchDescription(
            GitRefType type,
            @Nullable
            Pattern pattern,
            PatchDescription patch,
            RefPatchDescriptionProvider provider
        ) {
            this(type, pattern, provider);
            this.describeTagPattern = patch.describeTagPattern;
            this.updateGradleProperties = patch.updateGradleProperties;
            this.version = patch.version;
            this.properties = patch.properties;
        }

        public RefPatchDescription evaluate(GitSituation gitSituation) {
            if (provider != null) {
                provider.action(this, gitSituation);
            }
            return this;
        }
    }

    public static class RefPatchDescriptionList {

        public boolean considerTagsOnBranches = false;

        private List<Lazy<RefPatchDescription>> descriptors = new ArrayList<>();

        private List<RefPatchDescription> evaluatedDescriptors = null;

        public List<RefPatchDescription> descriptors(GitSituation gitSituation) {
            if (evaluatedDescriptors == null) {
                evaluatedDescriptors = descriptors.stream()
                    .map(it -> it.get().evaluate(gitSituation))
                    .collect(Collectors.toList());
            }
            return evaluatedDescriptors;
        }

        public void branch(String pattern, RefPatchDescriptionProvider action) {
            var lazyDesc = new RefPatchDescription(
                BRANCH,
                Pattern.compile(pattern),
                action
            );
            this.descriptors.add(Lazy.by(() -> lazyDesc));
        }

        public void tag(String pattern, RefPatchDescriptionProvider action) {
            var lazyDesc = new RefPatchDescription(
                TAG,
                Pattern.compile(pattern),
                action
            );
            this.descriptors.add(Lazy.by(() -> lazyDesc));
        }
    }
}
