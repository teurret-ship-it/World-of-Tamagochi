package android

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.Lint
import com.worldoftamagochi.buildlogic.JVM_TARGET
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

internal fun Project.configureKotlinAndroid() {
    tasks.withType<KotlinJvmCompile>().configureEach {
        compilerOptions.jvmTarget.set(JvmTarget.fromTarget(JVM_TARGET.toString()))
    }
}

internal fun ApplicationExtension.configureCommon(project: Project) {
    compileSdk = Sdk.COMPILE
    defaultConfig.minSdk = Sdk.MIN
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(JVM_TARGET)
        targetCompatibility = JavaVersion.toVersion(JVM_TARGET)
    }
    lint.configureLint(project)
}

internal fun LibraryExtension.configureCommon(project: Project) {
    compileSdk = Sdk.COMPILE
    defaultConfig.minSdk = Sdk.MIN
    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(JVM_TARGET)
        targetCompatibility = JavaVersion.toVersion(JVM_TARGET)
    }
    lint.configureLint(project)
}

/**
 * Lint is a hard gate: any warning fails the build. Dependency freshness checks
 * are disabled here because Dependabot owns version bumps (ADR-001).
 */
private fun Lint.configureLint(project: Project) {
    warningsAsErrors = true
    abortOnError = true
    checkDependencies = true
    disable +=
        setOf(
            "GradleDependency",
            "NewerVersionAvailable",
            "AndroidGradlePluginVersion",
            "OldTargetApi",
        )
    htmlReport = true
    htmlOutput =
        project.layout.buildDirectory
            .file("reports/lint/lint.html")
            .get()
            .asFile
}
