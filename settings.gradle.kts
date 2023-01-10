rootProject.name = "gradle-git-versioning-plugin"

enableFeaturePreview("STABLE_CONFIGURATION_CACHE") // https://docs.gradle.org/7.5.1/userguide/configuration_cache.html#config_cache:stable

@Suppress("UnstableApiUsage") // Central declaration of repositories is an incubating feature
dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }

    pluginManagement {
        repositories {
            gradlePluginPortal()
            mavenCentral()
        }
    }
}
