pluginManagement {
    includeBuild("build-logic")
    repositories {
        mavenCentral()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
    }
}

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "my-magical-pet"

// JVM-only mode (ADR-002): environments without access to Google Maven build
// and test only the pure Kotlin modules. CI always builds everything.
val jvmOnly =
    providers.gradleProperty("wot.jvmOnly").orNull?.toBoolean()
        ?: providers.environmentVariable("WOT_JVM_ONLY").orNull?.toBoolean()
        ?: false

include(":core:sim")
include(":core:model")
include(":core:api")
include(":core:network")
include(":server")

if (!jvmOnly) {
    include(":core:designsystem")
    include(":core:ui")
    include(":core:data")
    include(":feature:home")
    include(":feature:race")
    include(":feature:shop")
    include(":feature:onboarding")
    include(":app")
}
