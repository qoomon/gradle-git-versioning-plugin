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

    public void commit(Closure closure) {
        CommitVersionDescription versionDescription = new CommitVersionDescription();
        configure(closure, versionDescription);
        this.commitVersionDescription = versionDescription;
    }

    public void branchVersionDescription(Closure closure) {
        VersionDescription versionDescription = new VersionDescription();
        configure(closure, versionDescription);
        addBranchVersionDescription(versionDescription);
    }

    public void addBranchVersionDescription(VersionDescription versionDescription) {
        this.branchVersionDescriptions.add(versionDescription);
    }

    public void tag(Closure closure) {
        VersionDescription versionDescription = new VersionDescription();
        configure(closure, versionDescription);
        addTagVersionDescription(versionDescription);
    }

    public void addTagVersionDescription(VersionDescription versionDescription) {
        this.tagVersionDescriptions.add(versionDescription);
    }

    public static class VersionDescription {

        public String pattern;
        public String versionFormat;
        public List<PropertyDescription> properties = new ArrayList<>();

        public void property(Closure closure) {
            PropertyDescription propertyDescription = new PropertyDescription();
            configure(closure, propertyDescription);
            this.properties.add(propertyDescription);
        }
    }

    public static class CommitVersionDescription {

        public String versionFormat;
        public List<PropertyDescription> properties = new ArrayList<>();

        public void property(Closure closure) {
            PropertyDescription propertyDescription = new PropertyDescription();
            configure(closure, propertyDescription);
            this.properties.add(propertyDescription);
        }
    }

    public static class PropertyDescription {

        public String pattern;
        public ValueDescription value;

        public void value(Closure closure) {
            ValueDescription valueDescription = new ValueDescription();
            configure(closure, valueDescription);
            this.value = valueDescription;
        }
    }

    public static class ValueDescription {

        public String pattern;
        public String format;

    }
}
