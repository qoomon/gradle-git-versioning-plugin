package me.qoomon.gradle.gitversioning.util

import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language
import java.io.File


// utils for testing using Gradle TestKit


abstract class GradleProjectTest {
    abstract val projectDir: File
    abstract val runner: GradleRunner

    /** util for quickly inserting `\$` into multiline strings */
    val slashDollar = "\\\$"

    fun createFile(filename: String, contents: String): File =
        projectDir.resolve(filename).apply {
            parentFile.mkdirs()
            createNewFile()
            writeText(contents)
        }
}


/** Builder for testing a Gradle project that uses Kotlin script DSL */
class GradleKtsProjectTest(
    override val projectDir: File
) : GradleProjectTest() {
    override val runner: GradleRunner = GradleRunner.create().withProjectDir(projectDir)

    @Language("kts")
    var settingsGradleKts: String = """
         rootProject.name = "functional-plugin-test"
    """.trimIndent()

    @Language("kts")
    var buildGradleKts: String = ""

    @Language("properties")
    var gradleProperties: String = """
        kotlin.mpp.stability.nowarn=true
    """.trimIndent()

    companion object {
        fun gradleKtsProjectTest(
            projectDir: File,
            build: GradleKtsProjectTest.() -> Unit,
        ): GradleKtsProjectTest {
            val gradleTest = GradleKtsProjectTest(projectDir).apply(build)

            projectDir.apply {
                gradleTest.createFile("build.gradle.kts", gradleTest.buildGradleKts)
                gradleTest.createFile("settings.gradle.kts", gradleTest.settingsGradleKts)
                gradleTest.createFile("gradle.properties", gradleTest.gradleProperties)
            }

            return gradleTest
        }
    }
}


/** Builder for testing a Gradle project that uses Groovy script*/
class GradleGroovyProjectTest(
    override val projectDir: File
) : GradleProjectTest() {
    override val runner: GradleRunner = GradleRunner.create().withProjectDir(projectDir)

    @Language("groovy")
    var settingsGradle: String = """
       rootProject.name = "functional-plugin-test"
    """.trimIndent()

    @Language("groovy")
    var buildGradle: String = ""

    @Language("properties")
    var gradleProperties: String = """
        kotlin.mpp.stability.nowarn=true
    """.trimIndent()

    companion object {
        fun gradleGroovyProjectTest(
            projectDir: File,
            build: GradleGroovyProjectTest.() -> Unit,
        ): GradleGroovyProjectTest {
            val gradleTest = GradleGroovyProjectTest(projectDir).apply(build)

            projectDir.apply {
                gradleTest.createFile("build.gradle", gradleTest.buildGradle)
                gradleTest.createFile("settings.gradle", gradleTest.settingsGradle)
                gradleTest.createFile("gradle.properties", gradleTest.gradleProperties)
            }

            return gradleTest
        }
    }
}
