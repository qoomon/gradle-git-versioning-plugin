package buildsrc.conventions

/* Publishing configuration */

plugins {
    id("com.gradle.plugin-publish")
    `maven-publish` // for local testing only
}

pluginBundle {
    website = "https://github.com/qoomon/gradle-git-versioning-plugin"
    vcsUrl = "https://github.com/qoomon/gradle-git-versioning-plugin.git"
    tags = listOf("git", "versioning", "version", "commit", "branch", "tag", "generated")
}

publishing {
    repositories {
        // publish to a project-local directory, for testing.
        // ./gradlew publishAllPublicationsToProjectLocalRepository
        maven(rootProject.layout.buildDirectory.dir("maven-project-local")) {
            name = "ProjectLocal"
        }
    }
}
