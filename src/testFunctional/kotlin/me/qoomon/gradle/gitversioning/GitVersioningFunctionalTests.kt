package me.qoomon.gradle.gitversioning

import me.qoomon.gradle.gitversioning.util.GradleGroovyProjectTest.Companion.gradleGroovyProjectTest
import me.qoomon.gradle.gitversioning.util.GradleKtsProjectTest.Companion.gradleKtsProjectTest
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.CleanupMode.ON_SUCCESS
import org.junit.jupiter.api.io.TempDir
import java.io.File

class GitVersioningFunctionalTests {

    @Test
    fun `expect version is not set when not in git repo (kts)`(@TempDir(cleanup = ON_SUCCESS) tempDir: File) {
        val project = gradleKtsProjectTest(projectDir = tempDir) {

            buildGradleKts = """
                plugins {
                    id("me.qoomon.git-versioning") version "6.3.5"
                }
                
                version = "0.0.0-SNAPSHOT"
                gitVersioning.apply {
                    refs {
                        branch(".+") { 
                            version = "$slashDollar{ref}-SNAPSHOT" 
                        }
                        tag("v(?<version>.*)") { 
                            version = "$slashDollar{ref.version}" 
                        }
                    }
                
                    // optional fallback configuration in case of no matching ref configuration
                    rev { version = "$slashDollar{commit}" }
                }
            """.trimIndent()
        }

        val result = project.runner
            .withArguments("version")
            .build()

        assertTrue("project is not part of a git repository" in result.output)
        assertTrue("0.0.0-SNAPSHOT" in result.output)
    }

    @Test
    fun `expect version is not set when not in git repo (groovy)`(@TempDir(cleanup = ON_SUCCESS) tempDir: File) {

        val project = gradleGroovyProjectTest(projectDir = tempDir) {
            buildGradle = """
                plugins {
                    id("me.qoomon.git-versioning") version "6.3.5"
                }
                
                version = '0.0.0-SNAPSHOT'
                gitVersioning.apply {
                    refs {
                        branch('.+') {
                            version = '$slashDollar{ref}-SNAPSHOT'
                        }
                        tag('v(?<version>.*)') {
                            version = '$slashDollar{ref.version}'
                        }
                    }
                
                    // optional fallback configuration in case of no matching ref configuration
                    rev {
                        version = '$slashDollar{commit}'
                    }
                }
            """.trimIndent()
        }

        val result = project.runner
            .withArguments("version")
            .build()

        assertTrue("project is not part of a git repository" in result.output)
        assertTrue("0.0.0-SNAPSHOT" in result.output)
    }

}
