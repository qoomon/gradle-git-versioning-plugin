plugins {
    `kotlin-dsl`
    `java-gradle-plugin`

    kotlin("jvm") version embeddedKotlinVersion

    id("com.gradle.plugin-publish") version "1.0.0"
    `maven-publish` // for local testing only

    id "com.github.ben-manes.versions" version "0.43.0"
    id("com.adarshr.test-logger") version "3.2.0"

    idea
}

group = "me.qoomon"
version = "6.3.5"

dependencies {
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.3.0.202209071007-r")
    implementation("org.apache.maven:maven-artifact:3.8.6")
    implementation("org.apache.commons:commons-configuration2:2.8.0")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.9.1")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.9.1")
    testImplementation("org.assertj:assertj-core:3.23.1")
}

tasks.withType<Test>().configureEach {
    useJUnitPlatform()
}

gradlePlugin {
    plugins.create("gitVersioning") {
        id = "me.qoomon.git-versioning"
        displayName = "Git Versioning Plugin"
        description =
            "This extension will adjust the project version, based on current git branch or tag."
        implementationClass = "me.qoomon.gradle.gitversioning.GitVersioningPlugin"
    }
}

pluginBundle {
    website = "https://github.com/qoomon/gradle-git-versioning-plugin"
    vcsUrl = "https://github.com/qoomon/gradle-git-versioning-plugin.git"
    tags = listOf("git", "versioning", "version", "commit", "branch", "tag", "generated")
}

val projectJvmTarget = "11"

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(projectJvmTarget))
    }
}

kotlinDslPluginOptions {
    jvmTarget.set(projectJvmTarget)
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = projectJvmTarget
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = false
    }
}
