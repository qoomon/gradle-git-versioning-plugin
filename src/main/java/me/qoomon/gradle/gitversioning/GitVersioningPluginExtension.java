package me.qoomon.gradle.gitversioning;

import groovy.lang.Closure;
import me.qoomon.gitversioning.*;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtraPropertiesExtension;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static java.lang.Boolean.parseBoolean;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;
import static org.gradle.util.ConfigureUtil.configure;

public class GitVersioningPluginExtension {
    private static final String OPTION_NAME_GIT_TAG = "git.tag";
    private static final String OPTION_NAME_GIT_BRANCH = "git.branch";
    private static final String OPTION_NAME_DISABLE = "versioning.disable";
    private static final String OPTION_PREFER_TAGS = "versioning.preferTags";

    public final Project rootProject;

    public GitVersioningPluginExtension(Project project) {
        this.rootProject = project;
    }

    public void apply(Closure closure) {
        GitVersioningPluginConfig config = new GitVersioningPluginConfig();
        configure(closure, config);
        apply(config);
    }

    public void apply(GitVersioningPluginConfig config) {

        if (parseBoolean(getCommandOption(rootProject, OPTION_NAME_DISABLE))) {
            rootProject.getLogger().warn("skip - versioning is disabled");
            return;
        }

        GitRepoSituation repoSituation = GitUtil.situation(rootProject.getProjectDir());
        String providedTag = getCommandOption(rootProject, OPTION_NAME_GIT_TAG);
        if (providedTag != null) {
            repoSituation.setHeadBranch(null);
            repoSituation.setHeadTags(providedTag.isEmpty() ? emptyList() : singletonList(providedTag));
        }
        String providedBranch = getCommandOption(rootProject, OPTION_NAME_GIT_BRANCH);
        if (providedBranch != null) {
            repoSituation.setHeadBranch(providedBranch.isEmpty() ? null : providedBranch);
        }

        final boolean preferTagsOption = getPreferTagsOption(rootProject, config);
        GitVersionDetails gitVersionDetails = GitVersioning.determineVersion(repoSituation,
                ofNullable(config.commitVersionDescription)
                        .map(it -> new me.qoomon.gitversioning.VersionDescription(null, it.versionFormat, mapPropertyDescription(it.properties)))
                        .orElse(new me.qoomon.gitversioning.VersionDescription()),
                config.branchVersionDescriptions.stream()
                        .map(it -> new me.qoomon.gitversioning.VersionDescription(it.pattern, it.versionFormat, mapPropertyDescription(it.properties)))
                        .collect(toList()),
                config.tagVersionDescriptions.stream()
                        .map(it -> new me.qoomon.gitversioning.VersionDescription(it.pattern, it.versionFormat, mapPropertyDescription(it.properties)))
                        .collect(toList()),
                preferTagsOption);

        String gitProjectVersion = gitVersionDetails.getVersionTransformer().apply(rootProject.getVersion().toString());
        rootProject.getLogger().info(rootProject.getDisplayName()
                + " - git versioning [" + rootProject.getVersion() + " -> " + gitProjectVersion + "]"
                + " (" + gitVersionDetails.getCommitRefType() + ":" + gitVersionDetails.getCommitRefName() + ")");

        rootProject.getAllprojects().forEach(project -> {
            // update version
            project.setVersion(gitProjectVersion);

            // update properties
            gitVersionDetails.getPropertiesTransformer()
                    .apply(getProjectStringProperties(project), project.getVersion().toString())
                    .forEach((key, value) -> {
                        if (!Objects.equals(project.property(key), value)) {
                            project.getLogger().info(project.getDisplayName() + " - set property " + key + ": " + value);
                            project.setProperty(key, value);
                        }
                    });

            // provide extra properties
            ExtraPropertiesExtension extraProperties = project.getExtensions().getExtraProperties();
            extraProperties.set("git.commit", gitVersionDetails.getCommit());
            extraProperties.set("git.commit.timestamp", Long.toString(gitVersionDetails.getCommitTimestamp()));
            extraProperties.set("git.commit.timestamp.datetime", toTimestampDateTime(gitVersionDetails.getCommitTimestamp()));
            extraProperties.set("git.ref", gitVersionDetails.getCommitRefName());
            extraProperties.set("git." + gitVersionDetails.getCommitRefType(), gitVersionDetails.getCommitRefName());
            extraProperties.set("git.dirty", Boolean.toString(!gitVersionDetails.isClean()));
        });
    }

    private String resolveOriginVersion(Project project, Map<String, Object> originVersionMap) {
        String projectVersion = originVersionMap.get(project.getPath()).toString();
        if (!projectVersion.equals("unspecified")) {
            return projectVersion;
        }

        if (project.getParent() == null) {
            return "unspecified";
        }

        return resolveOriginVersion(project.getParent(), originVersionMap);

    }

    private Map<String, String> getProjectStringProperties(Project project) {
        Map<String, String> result = new HashMap<>();
        for (Map.Entry<String, ?> entry : project.getProperties().entrySet()) {
            if (entry.getValue() instanceof String) {
                result.put(entry.getKey(), (String) entry.getValue());
            }
        }
        return result;
    }

    private List<me.qoomon.gitversioning.PropertyDescription> mapPropertyDescription(List<GitVersioningPluginConfig.PropertyDescription> properties) {
        return properties.stream()
                .map(it -> new me.qoomon.gitversioning.PropertyDescription(it.pattern, mapPropertyValueDescription(it.value))
                ).collect(toList());
    }

    private PropertyValueDescription mapPropertyValueDescription(GitVersioningPluginConfig.ValueDescription value) {
        return Optional.of(value)
                .map(it -> new PropertyValueDescription(it.pattern, it.format)).get();
    }

    private static String getCommandOption(final Project project, final String name) {
        String value = (String) project.getProperties().get(name);
        if (value == null) {
            String plainName = name.replaceFirst("^versioning\\.", "");
            String environmentVariableName = "VERSIONING_"
                    + String.join("_", plainName.split("(?=\\p{Lu})"))
                    .replaceAll("\\.", "_")
                    .toUpperCase();
            value = System.getenv(environmentVariableName);
        }
        if (value == null) {
            value = System.getProperty(name);
        }
        return value;
    }

    private static boolean getPreferTagsOption(final Project project, GitVersioningPluginConfig config) {
        final boolean preferTagsOption;
        final String preferTagsCommandOption = getCommandOption(project, OPTION_PREFER_TAGS);
        if (preferTagsCommandOption != null) {
            preferTagsOption = parseBoolean(preferTagsCommandOption);
        } else {
            preferTagsOption = config.preferTags;
        }
        return preferTagsOption;
    }

    private static String toTimestampDateTime(long timestamp) {
        if (timestamp == 0) {
            return "0000-00-00T00:00:00Z";
        }

        return DateTimeFormatter.ISO_DATE_TIME
                .withZone(ZoneOffset.UTC)
                .format(Instant.ofEpochSecond(timestamp));
    }
}
