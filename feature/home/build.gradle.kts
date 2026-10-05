plugins {
    alias(libs.plugins.wot.android.library)
    alias(libs.plugins.wot.android.compose)
}

android {
    namespace = "com.worldoftamagochi.feature.home"
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.model)
}
