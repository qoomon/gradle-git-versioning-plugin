import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    kotlin("jvm") version embeddedKotlinVersion
}

dependencies {
    implementation(platform(kotlin("bom", embeddedKotlinVersion)))

    // define the Maven GAV coordinates of Gradle plugins that are used in the buildSrc convention plugins
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$embeddedKotlinVersion")
    implementation("com.gradle.publish:plugin-publish-plugin:1.0.0")
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

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = projectJvmTarget
    }
}
