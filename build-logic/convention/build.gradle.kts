plugins {
    `kotlin-dsl`
}

group = "com.worldoftamagochi.buildlogic"

// Tryb JVM-only: srodowisko bez dostepu do Google Maven (dl.google.com)
// buduje tylko moduly czysto kotlinowe. Patrz ADR-002 w docs/DECISIONS.md.
val jvmOnly =
    providers.gradleProperty("wot.jvmOnly").orNull?.toBoolean()
        ?: providers.environmentVariable("WOT_JVM_ONLY").orNull?.toBoolean()
        ?: false

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

sourceSets {
    main {
        kotlin {
            if (jvmOnly) exclude("**/android/**")
        }
    }
}

dependencies {
    implementation(libs.kotlin.gradlePlugin)
    implementation(libs.kotlin.serialization.gradlePlugin)
    implementation(libs.spotless.gradlePlugin)
    implementation(libs.detekt.gradlePlugin)
    implementation(libs.kover.gradlePlugin)
    if (!jvmOnly) {
        implementation(libs.android.gradlePlugin)
        implementation(libs.compose.gradlePlugin)
        implementation(libs.roborazzi.gradlePlugin)
    }
}

gradlePlugin {
    plugins {
        register("quality") {
            id = "wot.quality"
            implementationClass = "QualityConventionPlugin"
        }
        register("jvmLibrary") {
            id = "wot.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("coverage") {
            id = "wot.coverage"
            implementationClass = "CoverageConventionPlugin"
        }
        register("server") {
            id = "wot.server"
            implementationClass = "ServerConventionPlugin"
        }
        if (!jvmOnly) {
            register("androidApplication") {
                id = "wot.android.application"
                implementationClass = "android.AndroidApplicationConventionPlugin"
            }
            register("androidLibrary") {
                id = "wot.android.library"
                implementationClass = "android.AndroidLibraryConventionPlugin"
            }
            register("androidCompose") {
                id = "wot.android.compose"
                implementationClass = "android.AndroidComposeConventionPlugin"
            }
        }
    }
}
