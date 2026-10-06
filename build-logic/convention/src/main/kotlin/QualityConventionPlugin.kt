import com.diffplug.gradle.spotless.SpotlessExtension
import com.mymagicalpet.buildlogic.JVM_TARGET
import com.mymagicalpet.buildlogic.libs
import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType

/**
 * Formatting (Spotless + ktlint) and static analysis (detekt) for every module.
 * Both are wired into `check`, so a red `./gradlew check` is the single signal.
 */
class QualityConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("com.diffplug.spotless")
            pluginManager.apply("io.gitlab.arturbosch.detekt")

            val ktlintVersion = libs.findVersion("ktlint").get().requiredVersion
            extensions.configure<SpotlessExtension> {
                kotlin {
                    target("src/**/*.kt")
                    ktlint(ktlintVersion).setEditorConfigPath(rootProject.file(".editorconfig"))
                }
                kotlinGradle {
                    target("*.gradle.kts")
                    ktlint(ktlintVersion).setEditorConfigPath(rootProject.file(".editorconfig"))
                }
            }

            extensions.configure<DetektExtension> {
                buildUponDefaultConfig = true
                parallel = true
                config.setFrom(rootProject.file("config/detekt/detekt.yml"))
                source.setFrom("src/main/kotlin", "src/test/kotlin")
            }
            tasks.withType<Detekt>().configureEach {
                jvmTarget = JVM_TARGET.toString()
                reports {
                    html.required.set(true)
                    xml.required.set(false)
                    txt.required.set(true)
                    sarif.required.set(false)
                    md.required.set(false)
                }
            }
        }
}
