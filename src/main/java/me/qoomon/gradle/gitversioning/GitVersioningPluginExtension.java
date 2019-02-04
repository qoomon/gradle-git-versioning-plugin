package me.qoomon.gradle.gitversioning;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.gradle.api.Project;

import groovy.lang.Closure;

public class GitVersioningPluginExtension {

    public Project project;

    public List<VersionFormatDescription> branches = new ArrayList<>();
    public List<VersionFormatDescription> tags = new ArrayList<>();

    @Inject
    public GitVersioningPluginExtension(Project project) {
        this.project = project;
    }

    public void branch(Closure closure) {
        VersionFormatDescription versionFormatDescription = new VersionFormatDescription();
        project.configure(versionFormatDescription, closure);
        this.branches.add(versionFormatDescription);
    }

    public void tag(Closure closure) {
        VersionFormatDescription versionFormatDescription = new VersionFormatDescription();
        project.configure(versionFormatDescription, closure);
        this.tags.add(versionFormatDescription);
    }


}
