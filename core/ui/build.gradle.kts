plugins {
    alias(libs.plugins.wot.android.library)
    alias(libs.plugins.wot.android.compose)
}

android {
    namespace = "com.worldoftamagochi.ui"
}

dependencies {
    api(projects.core.sim)
    implementation(projects.core.designsystem)
}
