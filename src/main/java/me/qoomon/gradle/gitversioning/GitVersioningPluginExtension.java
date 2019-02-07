package me.qoomon.gradle.gitversioning;

import java.util.ArrayList;
import java.util.List;

import org.gradle.api.Project;

import groovy.lang.Closure;

public class GitVersioningPluginExtension {

    public final Project project;

    public boolean enabled = true;

    public CommitVersionDescription commit;
    public List<VersionDescription> branches = new ArrayList<>();
    public List<VersionDescription> tags = new ArrayList<>();

    public GitVersioningPluginExtension(Project project) {
        this.project = project;
    }

    public void branch(Closure closure) {
        VersionDescription versionDescription = new VersionDescription();
        project.configure(versionDescription, closure);
        this.branches.add(versionDescription);
    }

    public void tag(Closure closure) {
        VersionDescription versionDescription = new VersionDescription();
        project.configure(versionDescription, closure);
        this.tags.add(versionDescription);
    }

    public void commit(Closure closure) {
        CommitVersionDescription versionDescription = new CommitVersionDescription();
        project.configure(versionDescription, closure);
        this.commit = versionDescription;
    }

    public static class VersionDescription {

        public String pattern;
        public String prefix;
        public String versionFormat;
    }

    public static class CommitVersionDescription {

        public String versionFormat;
    }
}
