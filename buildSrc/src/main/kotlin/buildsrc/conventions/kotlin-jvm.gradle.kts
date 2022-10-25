package buildsrc.conventions

import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/* Kotlin JVM configuration */

plugins {
    kotlin("jvm")
}

val projectJvmTarget = "11"

kotlin {
    jvmToolchain {
        (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(projectJvmTarget))
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = projectJvmTarget
    }
}
