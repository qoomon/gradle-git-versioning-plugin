package me.qoomon.gradle.gitversioning;

import groovy.lang.Closure;
import org.gradle.api.Project;

import java.util.ArrayList;
import java.util.List;

public class GitVersioningPluginExtension {

    public final Project project;

    public boolean enabled = true;

    public CommitVersionDescription commit;
    public final List<VersionDescription> branches = new ArrayList<>();
    public final List<VersionDescription> tags = new ArrayList<>();

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
        public String versionFormat;
    }

    public static class CommitVersionDescription {

        public String versionFormat;
    }
}
