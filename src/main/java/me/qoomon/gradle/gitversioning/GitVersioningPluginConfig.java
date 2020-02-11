package me.qoomon.gradle.gitversioning;

import groovy.lang.Closure;

import java.util.ArrayList;
import java.util.List;

import static org.gradle.util.ConfigureUtil.configure;

public class GitVersioningPluginConfig {

    public CommitVersionDescription commitVersionDescription;
    public final List<VersionDescription> branchVersionDescriptions = new ArrayList<>();
    public final List<VersionDescription> tagVersionDescriptions = new ArrayList<>();

    public boolean preferTags = false;

    public void commit(Closure<?> closure) {
        CommitVersionDescription versionDescription = new CommitVersionDescription();
        configure(closure, versionDescription);
        this.commitVersionDescription = versionDescription;
    }

    public void branch(VersionDescription versionDescription) {
        this.branchVersionDescriptions.add(versionDescription);
    }

    public void branch(Closure<?> closure) {
        VersionDescription versionDescription = new VersionDescription();
        configure(closure, versionDescription);
        branch(versionDescription);
    }

    public void tag(VersionDescription versionDescription) {
        this.tagVersionDescriptions.add(versionDescription);
    }

    public void tag(Closure<?> closure) {
        VersionDescription versionDescription = new VersionDescription();
        configure(closure, versionDescription);
        tag(versionDescription);
    }

    public static class VersionDescription {

        public String pattern;
        public String versionFormat;
        public List<PropertyDescription> properties = new ArrayList<>();

        public void property(PropertyDescription propertyDescription) {
            this.properties.add(propertyDescription);
        }

        public void property(Closure<?> closure) {
            PropertyDescription propertyDescription = new PropertyDescription();
            configure(closure, propertyDescription);
            property(propertyDescription);
        }
    }

    public static class CommitVersionDescription {

        public String versionFormat;
        public List<PropertyDescription> properties = new ArrayList<>();

        public void property(PropertyDescription propertyDescription) {
            this.properties.add(propertyDescription);
        }

        public void property(Closure<?> closure) {
            PropertyDescription propertyDescription = new PropertyDescription();
            configure(closure, propertyDescription);
            property(propertyDescription);
        }
    }

    public static class PropertyDescription {

        public String pattern;
        public String valueFormat;
        public String valuePattern;
    }
}
