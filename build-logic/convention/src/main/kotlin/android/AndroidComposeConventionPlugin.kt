package android

import com.android.build.api.dsl.ApplicationExtension
import com.android.build.api.dsl.LibraryExtension
import com.worldoftamagochi.buildlogic.lib
import com.worldoftamagochi.buildlogic.libs
import io.github.takahirom.roborazzi.RoborazziExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType

/**
 * Jetpack Compose plus the screenshot pipeline: every Compose module can render
 * its screens on the JVM (Robolectric, native graphics) and compare them with
 * the reference PNGs kept in `src/test/screenshots` (Roborazzi).
 */
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
            pluginManager.apply("io.github.takahirom.roborazzi")

            pluginManager.withPlugin("com.android.application") {
                extensions.configure<ApplicationExtension> {
                    buildFeatures.compose = true
                    testOptions.unitTests.isIncludeAndroidResources = true
                }
            }
            pluginManager.withPlugin("com.android.library") {
                extensions.configure<LibraryExtension> {
                    buildFeatures.compose = true
                    testOptions.unitTests.isIncludeAndroidResources = true
                }
            }

            extensions.configure<RoborazziExtension> {
                outputDir.set(file("src/test/screenshots"))
            }
            tasks.withType<Test>().configureEach {
                systemProperty("robolectric.graphicsMode", "NATIVE")
                systemProperty("robolectric.pixelCopyRenderMode", "hardware")
            }

            dependencies {
                val bom = platform(libs.lib("compose-bom"))
                add("implementation", bom)
                add("implementation", libs.lib("compose-ui"))
                add("implementation", libs.lib("compose-ui-graphics"))
                add("implementation", libs.lib("compose-foundation"))
                add("implementation", libs.lib("compose-material3"))
                add("implementation", libs.lib("compose-ui-tooling-preview"))
                add("debugImplementation", libs.lib("compose-ui-tooling"))
                add("debugImplementation", libs.lib("compose-ui-test-manifest"))

                add("testImplementation", bom)
                add("testImplementation", libs.lib("junit4"))
                add("testImplementation", libs.lib("robolectric"))
                add("testImplementation", libs.lib("roborazzi"))
                add("testImplementation", libs.lib("roborazzi-compose"))
                add("testImplementation", libs.lib("compose-ui-test-junit4"))
                add("testImplementation", libs.lib("androidx-test-ext-junit"))
            }
        }
}
