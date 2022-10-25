package me.qoomon.gradle.gitversioning;

import org.gradle.api.Action;
import org.gradle.api.DefaultTask;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;

public class VersionTask extends DefaultTask {
    private final Object projectVersion = getProject().getVersion();

    @TaskAction
    void printProjectVersion() {
        System.out.println(projectVersion);
    }
}