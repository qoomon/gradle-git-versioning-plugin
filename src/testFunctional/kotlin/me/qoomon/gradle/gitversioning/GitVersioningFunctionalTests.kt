package me.qoomon.gradle.gitversioning

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import me.qoomon.gradle.gitversioning.util.GradleGroovyProjectTest.Companion.gradleGroovyProjectTest
import me.qoomon.gradle.gitversioning.util.GradleKtsProjectTest.Companion.gradleKtsProjectTest
import org.eclipse.jgit.api.Git
import org.gradle.testkit.runner.TaskOutcome
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

        result.output shouldContain "project is not part of a git repository"
        result.output shouldContain "0.0.0-SNAPSHOT"

        result.task(":version")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test
    fun `expect version is set when project is in a git repo (kts)`(@TempDir(cleanup = ON_SUCCESS) tempDir: File) {

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

        Git.init().setDirectory(project.projectDir).call()

        val result = project.runner
            .withArguments("version")
            .build()

        result.output shouldNotContain "project is not part of a git repository"
        result.output shouldContain "master-SNAPSHOT"

        result.task(":version")?.outcome shouldBe TaskOutcome.SUCCESS
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

        result.output shouldContain "project is not part of a git repository"
        result.output shouldContain "0.0.0-SNAPSHOT"

        result.task(":version")?.outcome shouldBe TaskOutcome.SUCCESS
    }

    @Test
    fun `expect version is set when project is in a git repo (groovy)`(@TempDir(cleanup = ON_SUCCESS) tempDir: File) {

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

        Git.init().setDirectory(project.projectDir).call()

        val result = project.runner
            .withArguments("version")
            .build()

        result.output shouldNotContain "project is not part of a git repository"
        result.output shouldContain "master-SNAPSHOT"

        result.task(":version")?.outcome shouldBe TaskOutcome.SUCCESS
    }
}
