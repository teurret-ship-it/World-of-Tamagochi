import com.worldoftamagochi.buildlogic.lib
import com.worldoftamagochi.buildlogic.libs
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies

/** kotlinx.serialization: the compiler plugin plus the JSON runtime. */
class SerializationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) =
        with(target) {
            pluginManager.apply("org.jetbrains.kotlin.plugin.serialization")
            dependencies { add("implementation", libs.lib("kotlinx-serialization-json")) }
        }
}
