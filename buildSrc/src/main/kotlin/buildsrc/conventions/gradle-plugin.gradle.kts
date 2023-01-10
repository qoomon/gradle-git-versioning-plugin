package buildsrc.conventions

import org.gradle.kotlin.dsl.*

/* Gradle plugin configuration */

plugins {
    `java-gradle-plugin`
    `jvm-test-suite`
}

@Suppress("UnstableApiUsage") // jvm test suites are incubating
testing.suites {

    // configure all test suites
    withType<JvmTestSuite>().configureEach {
        useJUnitJupiter()
    }

    // get the existing unit-test suite
    val test by getting(JvmTestSuite::class)

    // register a new functional-test suite
    val testFunctional by registering(JvmTestSuite::class) {

        // functional tests should run after unit tests
        targets.configureEach {
            testTask.configure { shouldRunAfter(test) }
        }

        sources {
            java {
                resources {
                    // fix issue with missing Gradle property file
                    srcDir(tasks.pluginUnderTestMetadata.map { it.outputDirectory })
                }
            }
        }

        gradlePlugin.testSourceSet(sources)
    }

    tasks.check { dependsOn(testFunctional) }
}
