package me.qoomon.gradle.gitversioning;

import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class VersionTask extends DefaultTask {

    @TaskAction
    void printVersion() {
        System.out.println(getProject().getVersion());



    }
}