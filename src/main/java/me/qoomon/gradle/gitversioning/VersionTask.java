package me.qoomon.gradle.gitversioning;

import org.gradle.api.DefaultTask;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.TaskAction;

public class VersionTask extends DefaultTask {
    private final Provider<String> projectVersion = getProject().provider(() -> getProject().getVersion().toString());

    @TaskAction
    void printProjectVersion() {
        System.out.println(projectVersion.get());
    }
}
