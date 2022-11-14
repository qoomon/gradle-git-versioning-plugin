package me.qoomon.gradle.gitversioning

import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import me.qoomon.gradle.gitversioning.util.GradleGroovyProjectTest
import me.qoomon.gradle.gitversioning.util.GradleGroovyProjectTest.Companion.gradleGroovyProjectTest
import me.qoomon.gradle.gitversioning.util.GradleKtsProjectTest
import me.qoomon.gradle.gitversioning.util.GradleKtsProjectTest.Companion.gradleKtsProjectTest
import org.eclipse.jgit.api.Git
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.*
import org.junit.jupiter.api.io.CleanupMode.ON_SUCCESS
import org.junit.jupiter.api.io.TempDir
import java.io.File


@TestMethodOrder(MethodOrderer.OrderAnnotation::class)
class GitVersioningFunctionalTests {

    @Nested
    inner class KtsProject {

        @field:TempDir(cleanup = ON_SUCCESS)
        lateinit var tempDir: File

        private val project: GradleKtsProjectTest by lazy {
            gradleKtsProjectTest(projectDir = tempDir) {
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
        }

        @Test
        @Order(1)
        fun `expect version is not set when not in git repo`() {

            val result = project.runner
                .withArguments("version")
                .build()

            result.output shouldContain "project is not part of a git repository"
            result.output shouldContain "0.0.0-SNAPSHOT"

            result.task(":version")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        @Test
        @Order(2)
        fun `expect version is set when project is in a git repo`() {

            Git.init().setDirectory(project.projectDir).call()

            val result = project.runner
                .withArguments("version")
                .build()

            result.output shouldNotContain "project is not part of a git repository"
            result.output shouldContain "master-SNAPSHOT"

            result.task(":version")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        @Test
        @Order(3)
        fun `tagged version`() {

            // I'm not sure why init is needed again. It should have already been initialized in the previous test.
            Git.init().setDirectory(project.projectDir).call()

            Git.open(project.projectDir).apply {
                add().addFilepattern("build.gradle.kts").call()
                commit().setMessage("testin'").call()
                tag().setName("v1.2.3").call()

                println(status().call())
            }

            val result = project.runner
                .withArguments("version")
                .build()

            result.output shouldNotContain "project is not part of a git repository"
            result.output shouldContain "master-SNAPSHOT"

            result.task(":version")?.outcome shouldBe TaskOutcome.SUCCESS
        }
    }

    @Nested
    inner class GroovyProject {

        @field:TempDir(cleanup = ON_SUCCESS)
        lateinit var tempDir: File

        private val project: GradleGroovyProjectTest by lazy {
            gradleGroovyProjectTest(projectDir = tempDir) {
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
        }

        @Test
        @Order(1)
        fun `expect version is not set when not in git repo`() {

            val result = project.runner
                .withArguments("version")
                .build()

            result.output shouldContain "project is not part of a git repository"
            result.output shouldContain "0.0.0-SNAPSHOT"

            result.task(":version")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        @Test
        @Order(2)
        fun `expect version is set to -SNAPSHOT when project is in a git repo without a tag`() {

            Git.init().setDirectory(project.projectDir).call()

            val result = project.runner
                .withArguments("version")
                .build()

            result.output shouldNotContain "project is not part of a git repository"
            result.output shouldContain "master-SNAPSHOT"

            result.task(":version")?.outcome shouldBe TaskOutcome.SUCCESS
        }

        @Test
        @Order(3)
        fun `tagged version`() {

            Git.init().setDirectory(project.projectDir).call()

            Git.open(project.projectDir).apply {
                add().addFilepattern("build.gradle.kts").call()
                commit().setMessage("testin'").call()
                tag().setName("v1.2.3").call()

                println(status().call())
            }

            val result = project.runner
                .withArguments("version")
                .build()

            result.output shouldNotContain "project is not part of a git repository"
            result.output shouldContain "master-SNAPSHOT"

            result.task(":version")?.outcome shouldBe TaskOutcome.SUCCESS
        }
    }
}
