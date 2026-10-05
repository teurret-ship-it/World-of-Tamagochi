import org.gradle.api.Plugin
import org.gradle.api.Project

/** Ktor game server: a JVM module with JSON serialization and an entry point. */
class ServerConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("wot.jvm.library")
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
            pluginManager.apply("application")
        }
}
