package me.qoomon.gradle.gitversioning;

import me.qoomon.gitversioning.*;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.ExtraPropertiesExtension;

import javax.annotation.Nonnull;

import java.util.*;
import java.util.Map.Entry;
import java.util.stream.Collectors;

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
            String providedTag = getOption(rootProject, "git.tag");
            if (providedTag != null) {
                repoSituation.setHeadBranch(null);
                repoSituation.setHeadTags(providedTag.isEmpty() ? emptyList() : singletonList(providedTag));
            }
            String providedBranch = getOption(rootProject, "git.branch");
            if (providedBranch != null) {
                repoSituation.setHeadBranch(providedBranch.isEmpty() ? null : providedBranch);
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
                            .collect(toList()));


            final Map<String, Object> originVersionMap = rootProject.getAllprojects().stream().collect(Collectors.toMap(Project::getPath, p -> p.getVersion()));

            rootProject.getAllprojects().forEach(project -> {
                // TODO check for version is equals to root project

                // update version
                String gitProjectVersion = gitVersionDetails.getVersionTransformer().apply(resolveOriginVersion(project, originVersionMap));

                project.getLogger().info(project.getDisplayName() + " - git versioning [" + project.getVersion() + " -> " + gitProjectVersion + "]"
                        + " (" + gitVersionDetails.getCommitRefType() + ":" + gitVersionDetails.getCommitRefName() + ")");

                project.setVersion(gitProjectVersion);


                // update properties
                Map<String, String> gitProjectProperties = gitVersionDetails.getPropertiesTransformer().apply(getProjectStringProperties(project), project.getVersion().toString());

                gitProjectProperties.forEach((key, value) -> {
                    if (!Objects.equals(project.property(key), value)) {
                        project.getLogger().info(project.getDisplayName() + " - set property " + key + ": " + value);
                        project.setProperty(key, value);
                    }
                });

                ExtraPropertiesExtension extraProperties = project.getExtensions().getExtraProperties();
                extraProperties.set("git.commit", gitVersionDetails.getCommit());
                extraProperties.set("git.ref", gitVersionDetails.getCommitRefName());
                extraProperties.set("git." + gitVersionDetails.getCommitRefType(), gitVersionDetails.getCommitRefName());
            });
        });
    }

    private String resolveOriginVersion(Project project, Map<String,Object> originVersionMap) {

        String projectVersion = originVersionMap.get(project.getPath()).toString();
        if (!projectVersion.equals("unspecified")) {
            return projectVersion;
        }

        if(project.getParent() == null){
            return "unspecified";
        }

        return resolveOriginVersion(project.getParent(), originVersionMap);

    }

    private Map<String, String> getProjectStringProperties(Project project) {
        Map<String, String> result = new HashMap<>();
        for (Entry<String, ?> entry : project.getProperties().entrySet()) {
            if (entry.getValue() instanceof String) {
                result.put(entry.getKey(), (String) entry.getValue());
            }
        }
        return result;
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

