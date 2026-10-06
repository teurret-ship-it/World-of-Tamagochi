import com.mymagicalpet.buildlogic.JVM_TARGET
import com.mymagicalpet.buildlogic.lib
import com.mymagicalpet.buildlogic.libs
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/** Pure Kotlin/JVM module: no Android dependency, testable anywhere. */
class JvmLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.jvm")
            pluginManager.apply("wot.quality")

            extensions.configure<JavaPluginExtension> {
                sourceCompatibility = JavaVersion.toVersion(JVM_TARGET)
                targetCompatibility = JavaVersion.toVersion(JVM_TARGET)
            }
            extensions.configure<KotlinJvmProjectExtension> {
                compilerOptions {
                    jvmTarget.set(JvmTarget.fromTarget(JVM_TARGET.toString()))
                    allWarningsAsErrors.set(true)
                }
            }
            configureJvmTests()
        }
}

fun Project.configureJvmTests() {
    dependencies {
        add("testImplementation", platform(libs.lib("junit-bom")))
        add("testImplementation", libs.lib("junit-jupiter"))
        add("testImplementation", libs.lib("kotest-assertions"))
        add("testImplementation", libs.lib("kotest-property"))
        add("testRuntimeOnly", libs.lib("junit-platform-launcher"))
    }
    tasks.withType<Test>().configureEach {
        useJUnitPlatform()
        testLogging {
            events("failed")
            exceptionFormat = org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL
        }
    }
}
