import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Line coverage gate. Applied to modules holding game rules (`:core:sim`),
 * where an untested branch is a balance bug waiting to ship.
 */
class CoverageConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlinx.kover")
            extensions.configure<KoverProjectExtension> {
                reports {
                    verify {
                        rule("Game rules are covered") {
                            minBound(MIN_LINE_COVERAGE)
                        }
                    }
                }
            }
            tasks.named("check").configure { dependsOn("koverVerify") }
        }

    private companion object {
        const val MIN_LINE_COVERAGE = 90
    }
}
