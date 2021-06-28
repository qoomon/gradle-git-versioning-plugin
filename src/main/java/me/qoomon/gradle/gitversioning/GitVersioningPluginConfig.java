package me.qoomon.gradle.gitversioning;

import groovy.lang.Closure;

import java.util.ArrayList;
import java.util.List;

import static org.gradle.util.ConfigureUtil.configure;

public class GitVersioningPluginConfig {

    public VersionDescription commit;
    public final List<VersionDescription> branches = new ArrayList<>();
    public final List<VersionDescription> tags = new ArrayList<>();

    public boolean disable = false;
    public boolean preferTags = false;
    public Boolean updateGradleProperties = false;

    public void commit(Closure<?> closure) {
        VersionDescription versionDescription = new VersionDescription();
        configure(closure, versionDescription);
        this.commit = versionDescription;
    }

    public void branch(VersionDescription versionDescription) {
        this.branches.add(versionDescription);
    }

    public void branch(Closure<?> closure) {
        VersionDescription versionDescription = new VersionDescription();
        configure(closure, versionDescription);
        branch(versionDescription);
    }

    public void tag(VersionDescription versionDescription) {
        this.tags.add(versionDescription);
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
        public Boolean updateGradleProperties;

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

        public String name;
        public String valueFormat;
    }
}
