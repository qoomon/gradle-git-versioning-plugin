pluginManagement {
    repositories {
        gradlePluginPortal()
    }
}

plugins {
    // provisions the Java toolchain if it is not installed locally
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        mavenCentral()
    }
}

rootProject.name = "gradle-git-versioning-plugin"
