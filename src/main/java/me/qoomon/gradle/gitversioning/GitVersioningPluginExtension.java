package me.qoomon.gradle.gitversioning;

import groovy.lang.Closure;
import org.gradle.api.Project;

import java.util.ArrayList;
import java.util.List;

import static org.gradle.util.ConfigureUtil.configure;

public class GitVersioningPluginExtension {

    public CommitVersionDescription commit;
    public final List<VersionDescription> branches = new ArrayList<>();
    public final List<VersionDescription> tags = new ArrayList<>();

    public boolean preferTags = false;

    public GitVersioningPluginExtension(Project project) {
    }

    public void branch(Closure closure) {
        VersionDescription versionDescription = new VersionDescription();
        configure(closure,versionDescription);
        this.branches.add(versionDescription);
    }

    public void tag(Closure closure) {
        VersionDescription versionDescription = new VersionDescription();
        configure(closure,versionDescription);
        this.tags.add(versionDescription);
    }

    public void commit(Closure closure) {
        CommitVersionDescription versionDescription = new CommitVersionDescription();
        configure(closure,versionDescription);
        this.commit = versionDescription;
    }

    public static class VersionDescription {

        public String pattern;
        public String versionFormat;
        public List<PropertyDescription> properties = new ArrayList<>();

        public void property(Closure closure) {
            PropertyDescription propertyDescription = new PropertyDescription();
            configure(closure,propertyDescription);
            this.properties.add(propertyDescription);
        }
    }

    public static class CommitVersionDescription {

        public String versionFormat;
        public List<PropertyDescription> properties = new ArrayList<>();

        public void property(Closure closure) {
            PropertyDescription propertyDescription = new PropertyDescription();
            configure(closure,propertyDescription);
            this.properties.add(propertyDescription);
        }
    }

    public static class PropertyDescription {

        public String pattern;
        public ValueDescription value;

        public void value(Closure closure) {
            ValueDescription valueDescription = new ValueDescription();
            configure(closure,valueDescription);
            this.value = valueDescription;
        }
    }

    public static class ValueDescription {

        public String pattern;
        public String format;

    }
}
