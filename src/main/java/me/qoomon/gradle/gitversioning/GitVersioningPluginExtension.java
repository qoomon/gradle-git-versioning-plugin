package me.qoomon.gradle.gitversioning;

import me.qoomon.gitversioning.commons.GitDescription;
import me.qoomon.gitversioning.commons.GitSituation;
import me.qoomon.gitversioning.commons.Lazy;
import me.qoomon.gradle.gitversioning.GitVersioningPluginConfig.PatchDescription;
import me.qoomon.gradle.gitversioning.GitVersioningPluginConfig.RefPatchDescription;
import org.apache.commons.configuration2.PropertiesConfiguration;
import org.apache.commons.configuration2.ex.ConfigurationException;
import org.apache.maven.artifact.versioning.DefaultArtifactVersion;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.gradle.api.Action;
import org.gradle.api.Project;
import org.gradle.api.model.ObjectFactory;
import org.gradle.api.plugins.ExtraPropertiesExtension;

import javax.inject.Inject;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.lang.Boolean.parseBoolean;
import static java.nio.file.Files.readAllBytes;
import static java.time.format.DateTimeFormatter.ISO_INSTANT;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.toList;
import static me.qoomon.gitversioning.commons.GitRefType.*;
import static me.qoomon.gitversioning.commons.StringUtil.*;
import static org.apache.commons.lang3.StringUtils.*;

@SuppressWarnings("StringConcatenationArgumentToLogCall")
public abstract class GitVersioningPluginExtension {

    private static final Pattern VERSION_PATTERN = Pattern.compile(".*?(?<version>(?<core>(?<major>\\d+)(?:\\.(?<minor>\\d+)(?:\\.(?<patch>\\d+))?)?)(?:-(?<label>.*))?)|");

    private static final String OPTION_NAME_GIT_REF = "git.ref";
    private static final String OPTION_NAME_GIT_TAG = "git.tag";
    private static final String OPTION_NAME_GIT_BRANCH = "git.branch";
    private static final String OPTION_NAME_DISABLE = "versioning.disable";
    private static final String OPTION_UPDATE_GRADLE_PROPERTIES = "versioning.updateGradleProperties";

    @Inject
    protected abstract ObjectFactory getObjectFactory();

    private final Project project;

    private GitVersioningPluginConfig config;

    public GitVersionDetails gitVersionDetails;

    public Map<String, Supplier<String>> globalFormatPlaceholderMap;

    public GitVersioningPluginExtension(Project project) {
        this.project = project;
    }

    public void apply(Action<GitVersioningPluginConfig> action) throws IOException {
        GitVersioningPluginConfig config = getObjectFactory().newInstance(GitVersioningPluginConfig.class);
        action.execute(config);
        apply(config);
    }

    public void apply(GitVersioningPluginConfig config) throws IOException {
        this.config = config;
        normalizeConfig(config);
        apply();
    }

    private void apply() throws IOException {
        // check if the extension is disabled by command option
        final String commandOptionDisable = getCommandOption(OPTION_NAME_DISABLE);
        if (commandOptionDisable != null) {
            boolean disabled = parseBoolean(commandOptionDisable);
            if (disabled) {
                project.getLogger().warn("skip - versioning is disabled by command option");
                return;
            }
        } else {
            // check if the extension is disabled by config option
            if (config.disable) {
                project.getLogger().warn("skip - versioning is disabled by config option");
                return;
            }
        }

        File gitDir;
        if (config.gitDir != null) {
            gitDir = new File(config.gitDir);
            project.getLogger().lifecycle("git directory: " + gitDir);
        } else {
            gitDir = project.getProjectDir();
        }
        final GitSituation gitSituation = getGitSituation(gitDir);
        if (gitSituation == null) {
            project.getLogger().warn("skip - project is not part of a git repository");
            return;
        }

        if (project.getLogger().isDebugEnabled()) {
            project.getLogger().debug("git situation:");
            project.getLogger().debug("  root directory: " + gitSituation.getRootDirectory());
            project.getLogger().debug("  head commit: " + gitSituation.getRev());
            project.getLogger().debug("  head commit timestamp: " + gitSituation.getTimestamp());
            project.getLogger().debug("  head branch: " + gitSituation.getBranch());
            project.getLogger().debug("  head tags: " + gitSituation.getTags());
            project.getLogger().debug("  head description: " + gitSituation.getDescription());
        }

        // determine git version details
        gitVersionDetails = getGitVersionDetails(gitSituation, config);
        if (gitVersionDetails == null) {
            project.getLogger().warn("skip - no matching ref configuration and no rev configuration defined");
            project.getLogger().warn("git refs:");
            project.getLogger().warn("  branch: " + gitSituation.getBranch());
            project.getLogger().warn("  tags: " + gitSituation.getTags());
            project.getLogger().warn("defined ref configurations:");
            config.refs.list.forEach(ref -> project.getLogger().warn("  " + rightPad(ref.type.name(), 6) + " - pattern: " + ref.pattern));
            return;
        }

        project.getLogger().lifecycle("matching ref: " + gitVersionDetails.getRefType().name() + " - " + gitVersionDetails.getRefName());
        final RefPatchDescription patchDescription = gitVersionDetails.getPatchDescription();
        project.getLogger().lifecycle("  ref configuration: " + gitVersionDetails.getRefType().name() + " - pattern: " + patchDescription.pattern);
        if (patchDescription.version != null) {
            project.getLogger().lifecycle("    version: " + patchDescription.version);
        }
        if (!patchDescription.properties.isEmpty()) {
            project.getLogger().lifecycle("    properties:");
            patchDescription.properties.forEach((key, value) -> project.getLogger().lifecycle("    " + key + ": " + value));
        }
        if (patchDescription.describeTagPattern != null) {
            project.getLogger().lifecycle("    describeTagPattern: " + patchDescription.describeTagPattern);
            gitSituation.setDescribeTagPattern(patchDescription.getDescribeTagPattern());
        }
        if (patchDescription.describeTagFirstParent != null) {
            project.getLogger().lifecycle("    describeTagFirstParent: " + patchDescription.describeTagFirstParent);
            gitSituation.setFirstParent(patchDescription.describeTagFirstParent);
        }
        boolean updateGradleProperties = getUpdateGradlePropertiesOption(patchDescription);
        if (updateGradleProperties) {
            project.getLogger().lifecycle("    updateGradleProperties: true");
        }

        globalFormatPlaceholderMap = generateGlobalFormatPlaceholderMap(gitSituation, gitVersionDetails, project);
        Map<String, String> gitProjectProperties = generateGitProjectProperties(gitSituation, gitVersionDetails);

        project.getLogger().lifecycle("");
        project.getAllprojects().forEach(project -> {
            final String originalProjectVersion = project.getVersion().toString();

            final String versionFormat = patchDescription.version;
            if (versionFormat != null) {
                updateVersion(project, versionFormat);
                if(project == project.getRootProject()) {
                    project.getLogger().lifecycle("project version: " + project.getVersion());
                } else if (!project.getVersion().equals(project.getRootProject().getVersion())) {
                    project.getLogger().lifecycle(project.getName()+ " > project version: " + project.getVersion());
                }
            }

            final Map<String, String> propertyFormats = patchDescription.properties;
            if (propertyFormats != null && !propertyFormats.isEmpty()) {
                updatePropertyValues(project, propertyFormats, originalProjectVersion);
            }

            addGitProjectProperties(project, gitProjectProperties);

            if (updateGradleProperties) {
                File gradleProperties = project.file("gradle.properties");
                if (gradleProperties.exists()) {
                    updateGradlePropertiesFile(gradleProperties, project);
                }
            }
        });
    }


    // ---- project processing -----------------------------------------------------------------------------------------

    private void updateVersion(Project project, String versionFormat) {
        String gitProjectVersion = getGitVersion(versionFormat, project.getVersion().toString());
        project.getLogger().info("set version to  " + gitProjectVersion);
        project.setVersion(gitProjectVersion);
    }

    private void updatePropertyValues(Project project, Map<String, String> propertyFormats, String originalProjectVersion) {
        boolean logHeader = true;
        // properties section
        for (Entry<String, ?> projectProperty : project.getProperties().entrySet()) {
            String projectPropertyName = projectProperty.getKey();
            Object projectPropertyValue = projectProperty.getValue();

            String propertyFormat = propertyFormats.get(projectPropertyName);
            if (propertyFormat != null) {
                if (projectPropertyValue == null || projectPropertyValue instanceof String) {
                    String gitPropertyValue = getGitPropertyValue(propertyFormat,
                            projectPropertyValue != null ? projectPropertyValue.toString() : null,
                            originalProjectVersion);
                    if (!gitPropertyValue.equals(projectPropertyValue)) {
                        if (logHeader) {
                            project.getLogger().lifecycle("properties:");
                            logHeader = false;
                        }
                        project.getLogger().lifecycle("  " + projectPropertyName + ": " + gitPropertyValue);
                        project.setProperty(projectPropertyName, gitPropertyValue);
                    }
                } else {
                    project.getLogger().warn("Can not update property " + projectPropertyName + "." +
                            " Expected value type is String, but was " + projectPropertyValue.getClass().getName());
                }
            }
        }
    }

    private void addGitProjectProperties(Project project, Map<String, String> gitProjectProperties) {
        ExtraPropertiesExtension extraProperties = project.getExtensions().getExtraProperties();
        gitProjectProperties.forEach(extraProperties::set);
    }

    private void updateGradlePropertiesFile(File gradleProperties, Project project) {
        // read existing gradle.properties
        PropertiesConfiguration gradlePropertiesConfig = new PropertiesConfiguration();
        try (FileReader reader = new FileReader(gradleProperties)) {
            gradlePropertiesConfig.read(reader);
        } catch (IOException | ConfigurationException e) {
            throw new RuntimeException(e);
        }

        // handle version
        if (gradlePropertiesConfig.containsKey("version")) {
            Object gradlePropertyVersion = gradlePropertiesConfig.getProperty("version");
            Object projectVersion = project.getVersion();
            if (!Objects.equals(projectVersion, gradlePropertyVersion)) {
                gradlePropertiesConfig.setProperty("version", projectVersion);
            }
        }

        // handle properties
        Map<String, ?> projectProperties = project.getProperties();
        gitVersionDetails.getPatchDescription().properties.forEach((key, value) -> {
            if (gradlePropertiesConfig.containsKey(key)) {
                Object gradlePropertyValue = gradlePropertiesConfig.getProperty(key);
                Object projectPropertyValue = projectProperties.get(key);
                if (!Objects.equals(projectPropertyValue, gradlePropertyValue)) {
                    gradlePropertiesConfig.setProperty(key, projectPropertyValue);
                }
            }
        });

        try (StringWriter writer = new StringWriter(512)) {
            gradlePropertiesConfig.write(writer);
            byte[] gitVersionedGradlePropertiesBytes = writer.toString().getBytes();
            byte[] existingGradlePropertiesBytes = readAllBytes(gradleProperties.toPath());
            // only write if there are changes
            if (!Arrays.equals(gitVersionedGradlePropertiesBytes, existingGradlePropertiesBytes)) {
                Files.write(gradleProperties.toPath(), gitVersionedGradlePropertiesBytes);
            }
        } catch (IOException | ConfigurationException e) {
            throw new RuntimeException(e);
        }
    }


    // ---- versioning -------------------------------------------------------------------------------------------------

    private GitSituation getGitSituation(File executionRootDirectory) throws IOException {
        final FileRepositoryBuilder repositoryBuilder = new FileRepositoryBuilder().findGitDir(executionRootDirectory);
        if (repositoryBuilder.getGitDir() == null) {
            return null;
        }

        final Repository repository = repositoryBuilder.build();
        return new GitSituation(repository) {
            {
                handleEnvironment(repository);
            }

            private void handleEnvironment(Repository repository) throws IOException {
                // --- commandline arguments and environment variables
                {
                    {
                        String overrideBranch = getCommandOption(OPTION_NAME_GIT_BRANCH);
                        String overrideTag = getCommandOption(OPTION_NAME_GIT_TAG);
                        if (overrideBranch != null || overrideTag != null) {
                            overrideBranch = overrideBranch == null || overrideBranch.trim().isEmpty() ? null : overrideBranch.trim();
                            setBranch(overrideBranch);

                            overrideTag = overrideTag == null || overrideTag.trim().isEmpty() ? null : overrideTag.trim();
                            setTags(overrideTag == null ? emptyList() : singletonList(overrideTag));
                            return;
                        }
                    }

                    {
                        final String providedRef = getCommandOption(OPTION_NAME_GIT_REF);
                        if (providedRef != null) {
                            if (!providedRef.startsWith("refs/")) {
                                throw new IllegalArgumentException("invalid provided ref " + providedRef + " -  needs to start with refs/");
                            }

                            if (providedRef.startsWith("refs/tags/")) {
                                setBranch(null);
                                setTags(singletonList(providedRef));
                            } else {
                                setBranch(providedRef);
                                setTags(emptyList());
                            }
                            return;
                        }
                    }
                }

                // --- try getting the branch and tag situation from environment ---
                // skip if we are on a branch
                if (repository.getBranch() == null) {
                    return;
                }

                // GitHub Actions support
                if ("true".equalsIgnoreCase(System.getenv("GITHUB_ACTIONS"))) {
                    if (!this.getRev().equals(System.getenv("GITHUB_SHA"))) {
                        return;
                    }

                    project.getLogger().lifecycle("gather git situation from GitHub Actions environment variable: GITHUB_REF");
                    String githubRef = System.getenv("GITHUB_REF");
                    project.getLogger().debug("  GITHUB_REF: " + githubRef);

                    if (githubRef.startsWith("refs/tags/")) {
                        addTag(githubRef);
                    } else {
                        setBranch(githubRef);
                    }
                    return;
                }

                // GitLab CI support
                if ("true".equalsIgnoreCase(System.getenv("GITLAB_CI"))) {
                    if (!this.getRev().equals(System.getenv("CI_COMMIT_SHA"))) {
                        return;
                    }

                    project.getLogger().lifecycle("gather git situation from GitLab CI environment variables: CI_COMMIT_BRANCH, CI_MERGE_REQUEST_SOURCE_BRANCH_NAME and CI_COMMIT_TAG");
                    String commitBranch = System.getenv("CI_COMMIT_BRANCH");
                    String commitTag = System.getenv("CI_COMMIT_TAG");
                    String mrSourceBranch = System.getenv("CI_MERGE_REQUEST_SOURCE_BRANCH_NAME");
                    project.getLogger().debug("  CI_COMMIT_BRANCH: " + commitBranch);
                    project.getLogger().debug("  CI_COMMIT_TAG: " + commitTag);
                    project.getLogger().debug("  CI_MERGE_REQUEST_SOURCE_BRANCH_NAME: " + mrSourceBranch);

                    if (!isBlank(commitBranch)) {
                        setBranch(commitBranch);
                    } else if (!isBlank(mrSourceBranch)) {
                        setBranch(mrSourceBranch);
                    } else if (!isBlank(commitTag)) {
                        addTag(commitTag);
                    }
                    return;
                }

                // Circle CI support
                if ("true".equalsIgnoreCase(System.getenv("CIRCLECI"))) {
                    if (!this.getRev().equals(System.getenv("CIRCLE_SHA1"))) {
                        return;
                    }

                    project.getLogger().lifecycle("gather git situation from Circle CI environment variables: CIRCLE_BRANCH and CIRCLE_TAG");
                    String commitBranch = System.getenv("CIRCLE_BRANCH");
                    String commitTag = System.getenv("CIRCLE_TAG");
                    project.getLogger().debug("  CIRCLE_BRANCH: " + commitBranch);
                    project.getLogger().debug("  CIRCLE_TAG: " + commitTag);

                    if (!isBlank(commitBranch)) {
                        setBranch(commitBranch.trim());
                    } else if (!isBlank(commitTag)) {
                        addTag(commitTag.trim());
                    }
                    return;
                }

                // Jenkins support
                if (System.getenv("JENKINS_HOME") != null && !System.getenv("JENKINS_HOME").trim().isEmpty()) {
                    if (!this.getRev().equals(System.getenv("GIT_COMMIT"))) {
                        return;
                    }
                    project.getLogger().lifecycle("gather git situation from jenkins environment variables: BRANCH_NAME and TAG_NAME");
                    String commitBranch = System.getenv("BRANCH_NAME");
                    String commitTag = System.getenv("TAG_NAME");
                    project.getLogger().debug("  BRANCH_NAME: " + commitBranch);
                    project.getLogger().debug("  TAG_NAME: " + commitTag);

                    if (!isBlank(commitBranch)) {
                        if (commitBranch.equals(commitTag)) {
                            addTag(commitBranch);
                        } else {
                            setBranch(commitBranch);
                        }
                    } else if (!isBlank(commitTag)) {
                        addTag(commitTag);
                    }
                    //noinspection UnnecessaryReturnStatement
                    return;
                }
            }


            protected void setBranch(String branch) {
                project.getLogger().debug("override git branch with " + branch);
                super.setBranch(branch);
            }

            protected void setTags(List<String> tags) {
                project.getLogger().debug("override git tags with single tag " + tags);
                super.setTags(tags);
            }

            protected void addTag(String tag) {
                project.getLogger().debug("add git tag " + tag);
                super.addTag(tag);
            }
        };
    }

    private static GitVersionDetails getGitVersionDetails(GitSituation gitSituation, GitVersioningPluginConfig config) {
        final Lazy<List<String>> sortedTags = Lazy.by(() -> gitSituation.getTags().stream()
                .sorted(comparing(DefaultArtifactVersion::new)).collect(toList()));
        for (RefPatchDescription refConfig : config.refs.list) {
            switch (refConfig.type) {
                case TAG: {
                    if (gitSituation.isDetached() || config.refs.considerTagsOnBranches) {
                        for (String tag : sortedTags.get()) {
                            if (refConfig.pattern == null || refConfig.pattern.matcher(tag).matches()) {
                                return new GitVersionDetails(gitSituation.getRev(), TAG, tag, refConfig);
                            }
                        }
                    }
                }
                break;
                case BRANCH: {
                    if (!gitSituation.isDetached()) {
                        String branch = gitSituation.getBranch();
                        if (refConfig.pattern == null || refConfig.pattern.matcher(branch).matches()) {
                            return new GitVersionDetails(gitSituation.getRev(), BRANCH, branch, refConfig);
                        }
                    }
                }
                break;
                default:
                    throw new IllegalArgumentException("Unexpected ref type: " + refConfig.type);
            }
        }

        if (config.rev != null) {
            return new GitVersionDetails(gitSituation.getRev(), COMMIT, gitSituation.getRev(),
                    new RefPatchDescription(COMMIT, null, config.rev));
        }


        return null;
    }

    private String getGitVersion(String versionFormat, String projectVersion) {
        final Map<String, Supplier<String>> placeholderMap = generateFormatPlaceholderMap(projectVersion);

        return slugify(substituteText(versionFormat, placeholderMap));
    }

    private String getGitPropertyValue(String propertyFormat, String originalValue, String projectVersion) {
        final Map<String, Supplier<String>> placeholderMap = generateFormatPlaceholderMap(projectVersion);
        placeholderMap.put("value", () -> originalValue);
        return substituteText(propertyFormat, placeholderMap);
    }

    private Map<String, Supplier<String>> generateFormatPlaceholderMap(String projectVersion) {
        final Map<String, Supplier<String>> placeholderMap = new HashMap<>(globalFormatPlaceholderMap);

        placeholderMap.put("version", Lazy.of(projectVersion));

        final Lazy<Matcher> projectVersionMatcher = Lazy.by(() -> matchVersion(projectVersion));

        placeholderMap.put("version.core", Lazy.by(() -> notNullOrDefault(projectVersionMatcher.get().group("core"), "0.0.0")));

        placeholderMap.put("version.major", Lazy.by(() -> notNullOrDefault(projectVersionMatcher.get().group("major"), "0")));
        placeholderMap.put("version.major.next", Lazy.by(() -> increase(placeholderMap.get("version.major").get(), 1)));

        placeholderMap.put("version.minor", Lazy.by(() -> notNullOrDefault(projectVersionMatcher.get().group("minor"), "0")));
        placeholderMap.put("version.minor.next", Lazy.by(() -> increase(placeholderMap.get("version.minor").get(), 1)));

        placeholderMap.put("version.patch", Lazy.by(() -> notNullOrDefault(projectVersionMatcher.get().group("patch"), "0")));
        placeholderMap.put("version.patch.next", Lazy.by(() -> increase(placeholderMap.get("version.patch").get(), 1)));

        placeholderMap.put("version.label", Lazy.by(() -> notNullOrDefault(projectVersionMatcher.get().group("label"), "")));
        placeholderMap.put("version.label.prefixed", Lazy.by(() -> {
            String label = placeholderMap.get("version.label").get();
            return !label.isEmpty() ? "-" + label : "";
        }));

        // deprecated
        placeholderMap.put("version.release", Lazy.by(() -> projectVersion.replaceFirst("-.*$", "")));

        final Pattern projectVersionPattern = config.projectVersionPattern();
        if (projectVersionPattern != null) {
            // ref pattern groups
            for (Entry<String, String> patternGroup : patternGroupValues(projectVersionPattern, projectVersion).entrySet()) {
                final String groupName = patternGroup.getKey();
                final String value = patternGroup.getValue() != null ? patternGroup.getValue() : "";
                placeholderMap.put("version." + groupName, () -> value);
            }
        }

        return placeholderMap;
    }

    private Map<String, Supplier<String>> generateGlobalFormatPlaceholderMap(GitSituation gitSituation, GitVersionDetails gitVersionDetails, Project rootProject) {

        final Map<String, Supplier<String>> placeholderMap = new HashMap<>();

        final Lazy<String> hash = Lazy.by(gitSituation::getRev);
        placeholderMap.put("commit", hash);
        placeholderMap.put("commit.short", Lazy.by(() -> hash.get().substring(0, 7)));

        final Lazy<ZonedDateTime> headCommitDateTime = Lazy.by(gitSituation::getTimestamp);
        placeholderMap.put("commit.timestamp", Lazy.by(() -> String.valueOf(headCommitDateTime.get().toEpochSecond())));
        placeholderMap.put("commit.timestamp.year", Lazy.by(() -> String.valueOf(headCommitDateTime.get().getYear())));
        placeholderMap.put("commit.timestamp.year.2digit", Lazy.by(() -> String.valueOf(headCommitDateTime.get().getYear() % 100)));
        placeholderMap.put("commit.timestamp.month", Lazy.by(() -> leftPad(String.valueOf(headCommitDateTime.get().getMonthValue()), 2, "0")));
        placeholderMap.put("commit.timestamp.day", Lazy.by(() -> leftPad(String.valueOf(headCommitDateTime.get().getDayOfMonth()), 2, "0")));
        placeholderMap.put("commit.timestamp.hour", Lazy.by(() -> leftPad(String.valueOf(headCommitDateTime.get().getHour()), 2, "0")));
        placeholderMap.put("commit.timestamp.minute", Lazy.by(() -> leftPad(String.valueOf(headCommitDateTime.get().getMinute()), 2, "0")));
        placeholderMap.put("commit.timestamp.second", Lazy.by(() -> leftPad(String.valueOf(headCommitDateTime.get().getSecond()), 2, "0")));
        placeholderMap.put("commit.timestamp.datetime", Lazy.by(() -> headCommitDateTime.get().toEpochSecond() > 0
                ? headCommitDateTime.get().format(DateTimeFormatter.ofPattern("yyyyMMdd.HHmmss")) : "00000000.000000"));

        final String refName = gitVersionDetails.getRefName();
        final Lazy<String> refNameSlug = Lazy.by(() -> slugify(refName));
        placeholderMap.put("ref", () -> refName);
        placeholderMap.put("ref" + ".slug", refNameSlug);

        final Pattern refPattern = gitVersionDetails.getPatchDescription().pattern;
        if (refPattern != null) {
            // ref pattern groups
            for (Entry<String, String> patternGroup : patternGroupValues(refPattern, refName).entrySet()) {
                final String groupName = patternGroup.getKey();
                final String value = patternGroup.getValue() != null ? patternGroup.getValue() : "";
                placeholderMap.put("ref." + groupName, () -> value);
                placeholderMap.put("ref." + groupName + ".slug", Lazy.by(() -> slugify(value)));
            }
        }

        // dirty
        final Lazy<Boolean> dirty = Lazy.by(() -> !gitSituation.isClean());
        placeholderMap.put("dirty", Lazy.by(() -> dirty.get() ? "-DIRTY" : ""));
        placeholderMap.put("dirty.snapshot", Lazy.by(() -> dirty.get() ? "-SNAPSHOT" : ""));

        // describe
        final Lazy<GitDescription> description = Lazy.by(gitSituation::getDescription);
        placeholderMap.put("describe", Lazy.by(() -> description.get().toString()));
        final Lazy<String> descriptionTag = Lazy.by(() -> description.get().getTag());
        placeholderMap.put("describe.tag", descriptionTag);
        // describe tag pattern groups
        final Lazy<Map<String, String>> describeTagPatternValues = Lazy.by(
                () -> patternGroupValues(gitSituation.getDescribeTagPattern(), descriptionTag.get()));
        for (String groupName : patternGroups(gitSituation.getDescribeTagPattern())) {
            Lazy<String> groupValue = Lazy.by(() -> describeTagPatternValues.get().get(groupName));
            placeholderMap.put("describe.tag." + groupName, groupValue);
            placeholderMap.put("describe.tag." + groupName + ".slug", Lazy.by(() -> slugify(groupValue.get())));
        }

        final Lazy<Matcher> descriptionTagVersionMatcher = Lazy.by(() -> matchVersion(descriptionTag.get()));

        placeholderMap.put("describe.tag.version", Lazy.by(() -> notNullOrDefault(descriptionTagVersionMatcher.get().group("version"), "0.0.0")));

        placeholderMap.put("describe.tag.version.core", Lazy.by(() -> notNullOrDefault(descriptionTagVersionMatcher.get().group("core"), "0")));

        placeholderMap.put("describe.tag.version.major", Lazy.by(() -> notNullOrDefault(descriptionTagVersionMatcher.get().group("major"), "0")));
        placeholderMap.put("describe.tag.version.major.next", Lazy.by(() -> increase(placeholderMap.get("describe.tag.version.major").get(), 1)));

        placeholderMap.put("describe.tag.version.minor", Lazy.by(() -> notNullOrDefault(descriptionTagVersionMatcher.get().group("minor"), "0")));
        placeholderMap.put("describe.tag.version.minor.next", Lazy.by(() -> increase(placeholderMap.get("describe.tag.version.minor").get(), 1)));

        placeholderMap.put("describe.tag.version.patch", Lazy.by(() -> notNullOrDefault(descriptionTagVersionMatcher.get().group("patch"), "0")));
        placeholderMap.put("describe.tag.version.patch.next", Lazy.by(() -> increase(placeholderMap.get("describe.tag.version.patch").get(), 1)));

        placeholderMap.put("describe.tag.version.label", Lazy.by(() -> notNullOrDefault(descriptionTagVersionMatcher.get().group("label"), "")));
        placeholderMap.put("describe.tag.version.label.next", Lazy.by(() -> increase(placeholderMap.get("describe.tag.version.label").get(), 1)));

        final Lazy<Integer> descriptionDistance = Lazy.by(() -> description.get().getDistance());
        placeholderMap.put("describe.distance", Lazy.by(() -> String.valueOf(descriptionDistance.get())));

        placeholderMap.put("describe.tag.version.patch.plus.describe.distance", Lazy.by(() -> increase(placeholderMap.get("describe.tag.version.patch").get(), descriptionDistance.get())));
        placeholderMap.put("describe.tag.version.patch.next.plus.describe.distance", Lazy.by(() -> increase(placeholderMap.get("describe.tag.version.patch.next").get(), descriptionDistance.get())));

        placeholderMap.put("describe.tag.version.label.plus.describe.distance", Lazy.by(() -> increase(placeholderMap.get("describe.tag.version.label").get(), descriptionDistance.get())));
        placeholderMap.put("describe.tag.version.label.next.plus.describe.distance", Lazy.by(() -> increase(placeholderMap.get("describe.tag.version.label.next").get(), descriptionDistance.get())));

        // command parameters e.g. gradle -Pfoo=123 will be available as ${property.foo}
        for (Entry<String, ?> property : rootProject.getProperties().entrySet()) {
            if (property.getValue() != null) {
                // filter complex properties
                if (property.getValue() instanceof String || property.getValue() instanceof Number) {
                    placeholderMap.put("property." + property.getKey(), () -> property.getValue().toString());
                }
            }
        }

        // environment variables e.g. BUILD_NUMBER=123 will be available as ${env.BUILD_NUMBER}
        System.getenv().forEach((key, value) -> placeholderMap.put("env." + key, () -> value));

        return placeholderMap;
    }

    private Matcher matchVersion(String input) {
        Matcher matcher = VERSION_PATTERN.matcher(input);
        //noinspection ResultOfMethodCallIgnored
        matcher.find();

        return matcher;
    }

    private static Map<String, String> generateGitProjectProperties(GitSituation gitSituation, GitVersionDetails gitVersionDetails) {
        final Map<String, String> properties = new HashMap<>();

        properties.put("git.commit", gitVersionDetails.getCommit());
        properties.put("git.commit.short", gitVersionDetails.getCommit().substring(0, 7));

        final ZonedDateTime headCommitDateTime = gitSituation.getTimestamp();
        properties.put("git.commit.timestamp", String.valueOf(headCommitDateTime.toEpochSecond()));
        properties.put("git.commit.timestamp.datetime", headCommitDateTime.toEpochSecond() > 0
                ? headCommitDateTime.format(ISO_INSTANT) : "0000-00-00T00:00:00Z");

        final String refName = gitVersionDetails.getRefName();
        final String refNameSlug = slugify(refName);
        properties.put("git.ref", refName);
        properties.put("git.ref" + ".slug", refNameSlug);

        return properties;
    }


    // ---- configuration ----------------------------------------------------------------------------------------------

    private void normalizeConfig(GitVersioningPluginConfig config) {
        // consider global config
        List<PatchDescription> patchDescriptions = new ArrayList<>(config.refs.list);
        if (config.rev != null) {
            patchDescriptions.add(config.rev);
        }
        for (PatchDescription patchDescription : patchDescriptions) {
            if (patchDescription.describeTagPattern == null) {
                patchDescription.describeTagPattern = config.describeTagPattern;
            }
            if (patchDescription.describeTagFirstParent == null) {
                patchDescription.describeTagFirstParent = config.describeTagFirstParent;
            }
            if (patchDescription.updateGradleProperties == null) {
                patchDescription.updateGradleProperties = config.updateGradleProperties;
            }
        }
    }

    private String getCommandOption(final String name) {
        String value = System.getProperty(name);
        if (value == null) {
            String plainName = name.replaceFirst("^versioning\\.", "");
            String environmentVariableName = "VERSIONING_"
                    + String.join("_", plainName.split("(?=\\p{Lu})"))
                    .replaceAll("\\.", "_")
                    .toUpperCase();
            value = System.getenv(environmentVariableName);
        }
        return value;
    }

    private boolean getUpdateGradlePropertiesOption(final RefPatchDescription gitRefConfig) {
        final String updateGradlePropertiesOption = getCommandOption(OPTION_UPDATE_GRADLE_PROPERTIES);
        if (updateGradlePropertiesOption != null) {
            return parseBoolean(updateGradlePropertiesOption);
        }

        return Objects.requireNonNullElse(gitRefConfig.updateGradleProperties, false);
    }


    // ---- misc -------------------------------------------------------------------------------------------------------

    private static String slugify(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("/", "-");
    }

    private static String increase(String number, long increment) {
        String sanitized = number.isEmpty() ? "0" : number;
        return String.format("%0" + sanitized.length() + "d", Long.parseLong(number.isEmpty() ? "0" : number) + increment);
    }

    public static <T> T notNullOrDefault(T obj, T defaultObj) {
        return (obj != null) ? obj : defaultObj;
    }
}
