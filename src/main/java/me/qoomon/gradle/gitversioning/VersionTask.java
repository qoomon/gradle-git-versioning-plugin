package me.qoomon.gradle.gitversioning;

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.TaskAction;
import org.gradle.work.DisableCachingByDefault;

@DisableCachingByDefault(because = "Only prints the project version")
public abstract class VersionTask extends DefaultTask {

    @Input
    public abstract Property<String> getProjectVersion();

    @TaskAction
    void printProjectVersion() {
        System.out.println(getProjectVersion().get());
    }
}
