plugins {
    `kotlin-dsl`

    buildsrc.conventions.`kotlin-jvm`
    buildsrc.conventions.publishing
    buildsrc.conventions.`gradle-plugin`

    id("com.github.ben-manes.versions") version "0.43.0"
    id("com.adarshr.test-logger") version "3.2.0"

    idea
}

group = "me.qoomon"
version = "6.3.7"

dependencies {
    implementation("org.eclipse.jgit:org.eclipse.jgit:6.3.0.202209071007-r")
    implementation("org.apache.maven:maven-artifact:3.8.6")
    implementation("org.apache.commons:commons-configuration2:2.8.0")

    // test dependencies are declared in the test suites config
}

gradlePlugin {
    plugins.create("gitVersioning") {
        id = "me.qoomon.git-versioning"
        displayName = "Git Versioning Plugin"
        description = "This extension will adjust the project version, based on current git branch or tag."
        implementationClass = "me.qoomon.gradle.gitversioning.GitVersioningPlugin"
    }
}

@Suppress("UNUSED_VARIABLE", "UnstableApiUsage") // jvm test suites are incubating
testing.suites {

    // configure the existing unit-test suite
    val test by getting(JvmTestSuite::class) {
        dependencies {
            implementation(project.dependencies.platform("org.junit:junit-bom:5.9.1"))
            implementation("org.junit.jupiter:junit-jupiter-api")
            implementation("org.junit.jupiter:junit-jupiter-engine")

            implementation("org.assertj:assertj-core:3.23.1")
        }
    }

    // configure the functional-test suite (created in buildSrc convention plugin)
    val testFunctional by getting(JvmTestSuite::class) {
        dependencies {
            implementation(project)
            implementation(project.dependencies.gradleTestKit())

            implementation(project.dependencies.platform("org.junit:junit-bom:5.9.1"))
            implementation("org.junit.jupiter:junit-jupiter-api")
            implementation("org.junit.jupiter:junit-jupiter-engine")

            implementation("org.eclipse.jgit:org.eclipse.jgit:6.3.0.202209071007-r")

            implementation(project.dependencies.platform("io.kotest:kotest-bom:5.5.4"))
            implementation("io.kotest:kotest-assertions-core")
        }
    }
}

idea {
    module {
        isDownloadSources = true
        isDownloadJavadoc = false
    }
}
