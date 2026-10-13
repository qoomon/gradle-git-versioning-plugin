import org.gradle.api.tasks.testing.logging.TestExceptionFormat

plugins {
    `java-gradle-plugin`
    alias(libs.plugins.plugin.publish)
    alias(libs.plugins.versions)
}

group = "me.qoomon"
version = "6.4.5"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(libs.versions.java.get())
    }
}

tasks.withType<JavaCompile>().configureEach {
    options.compilerArgs.add("-Xlint:deprecation")
}

dependencies {
    // nullness annotations of the public API, e.g. understood by the Kotlin compiler
    api(libs.jspecify)

    implementation(libs.jgit)
    implementation(libs.maven.artifact)
    implementation(libs.commons.configuration2)

    testImplementation(libs.assertj.core)
}

testing {
    suites {
        named<JvmTestSuite>("test") {
            useJUnitJupiter(libs.versions.junit)
        }
    }
}

tasks.test {
    testLogging {
        events("passed", "skipped", "failed")
        exceptionFormat = TestExceptionFormat.FULL
    }
}

tasks.validatePlugins {
    enableStricterValidation = true
}

gradlePlugin {
    website = "https://github.com/qoomon/gradle-git-versioning-plugin"
    vcsUrl = "https://github.com/qoomon/gradle-git-versioning-plugin.git"

    plugins {
        register("gitVersioning") {
            id = "me.qoomon.git-versioning"
            displayName = "Git Versioning Plugin"
            description = "This extension will adjust the project version, based on current git branch or tag."
            tags = listOf("git", "versioning", "version", "commit", "branch", "tag", "generated")
            implementationClass = "me.qoomon.gradle.gitversioning.GitVersioningPlugin"
        }
    }
}
