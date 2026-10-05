// Plugins are applied in modules through the convention plugins in build-logic/.
// The root project only hosts aggregate tasks.

tasks.register("jvmCheck") {
    description = "Runs `check` for the modules that need no Android SDK."
    group = "verification"
    dependsOn(":core:sim:check", ":core:model:check", ":core:api:check", ":core:network:check", ":server:check")
}
