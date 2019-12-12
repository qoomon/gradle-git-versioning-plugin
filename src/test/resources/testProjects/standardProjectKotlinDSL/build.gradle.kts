import me.qoomon.gradle.gitversioning.GitVersioningPluginConfig
import me.qoomon.gradle.gitversioning.GitVersioningPluginConfig.*

plugins {
    id("me.qoomon.git-versioning") version "2.1.1"
}

version = "0.0.0-SNAPSHOT"
gitVersioning.apply(closureOf<GitVersioningPluginConfig> {
    branch(closureOf<VersionDescription> {
        pattern = "master"
        versionFormat = "\${version}-matser-XXX"
    })
})