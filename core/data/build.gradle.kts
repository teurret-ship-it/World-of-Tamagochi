plugins {
    alias(libs.plugins.wot.android.library)
    id("org.jetbrains.kotlin.plugin.serialization")
}

android {
    namespace = "com.worldoftamagochi.data"
}

dependencies {
    api(projects.core.sim)
    api(libs.androidx.datastore)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
