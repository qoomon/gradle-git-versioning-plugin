package me.qoomon.gradle.gitversioning;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import me.qoomon.gitversioning.*;
import me.qoomon.gradle.gitversioning.GitVersioningPluginExtension.ValueDescription;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtraPropertiesExtension;

import javax.annotation.Nonnull;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.Optional.ofNullable;
import static java.util.stream.Collectors.toList;

public class GitVersioningPlugin implements Plugin<Project> {

    public void apply(@Nonnull Project rootProject) {
        rootProject.getAllprojects().forEach(it -> it.getTasks().create("version", VersionTask.class));

        GitVersioningPluginExtension config = rootProject.getExtensions()
                .create("gitVersioning", GitVersioningPluginExtension.class, rootProject);
        rootProject.afterEvaluate(evaluatedProject -> {

            GitRepoSituation repoSituation = GitUtil.situation(rootProject.getProjectDir());
            String providedBranch = getOption(rootProject, "git.branch");
            if (providedBranch != null) {
                repoSituation.setHeadBranch(providedBranch.isEmpty() ? null : providedBranch);
            }
            String providedTag = getOption(rootProject, "git.tag");
            if (providedTag != null) {
                repoSituation.setHeadTags(providedTag.isEmpty() ? emptyList() : singletonList(providedTag));
            }

            rootProject.getAllprojects().forEach(project -> {
                // TODO check for version is equals to root project

                Map<String, String> projectStringProperties = new HashMap<>();
                for (Entry<String, ?> entry : project.getProperties().entrySet()) {
                    if (entry.getValue() instanceof String) {
                        projectStringProperties.put(entry.getKey(), (String) entry.getValue());
                    }
                }

                GitVersionDetails gitVersionDetails = GitVersioning.determineVersion(repoSituation,
                        ofNullable(config.commit)
                                .map(it -> new VersionDescription(null, it.versionFormat, mapPropertyDescription(it.properties)))
                                .orElse(new VersionDescription()),
                        config.branches.stream()
                                .map(it -> new VersionDescription(it.pattern, it.versionFormat, mapPropertyDescription(it.properties)))
                                .collect(toList()),
                        config.tags.stream()
                                .map(it -> new VersionDescription(it.pattern, it.versionFormat, mapPropertyDescription(it.properties)))
                                .collect(toList()),
                        project.getVersion().toString(),
                        projectStringProperties);

                project.getLogger().info(project.getDisplayName() + " - git versioning [" + project.getVersion() + " -> " + gitVersionDetails.getVersion() + "]"
                        + " (" + gitVersionDetails.getCommitRefType() + ":" + gitVersionDetails.getCommitRefName() + ")");


                // update properties
                gitVersionDetails.getProperties().forEach((key, value) -> {
                    if (!project.property(key).equals(value)) {
                        project.setProperty(key, value);

                    }
                });

                // update version
                project.setVersion(gitVersionDetails.getVersion());

                ExtraPropertiesExtension extraProperties = project.getExtensions().getExtraProperties();
                extraProperties.set("git.commit", gitVersionDetails.getCommit());
                extraProperties.set("git.ref", gitVersionDetails.getCommitRefName());
                extraProperties.set("git." + gitVersionDetails.getCommitRefType(), gitVersionDetails.getCommitRefName());
                gitVersionDetails.getMetaData().forEach((key, value) -> extraProperties.set("git.ref." + key, value)); // TODO write tests
            });
        });
    }

    private List<PropertyDescription> mapPropertyDescription(List<GitVersioningPluginExtension.PropertyDescription> properties) {
        return properties.stream()
                .map(it -> new PropertyDescription(it.pattern, mapPropertyValueDescription(it.value))
                ).collect(toList());
    }

    private PropertyValueDescription mapPropertyValueDescription(GitVersioningPluginExtension.ValueDescription value) {
        return Optional.of(value)
                .map(it -> new PropertyValueDescription(it.pattern, it.format)).get();
    }

    private String getOption(final Project project, final String name) {
        String value = (String) project.getProperties().get(name);
        if (value == null) {
            value = System.getenv("VERSIONING_" + name.replaceAll("\\.", "_").toUpperCase());
        }
        return value;
    }
}

