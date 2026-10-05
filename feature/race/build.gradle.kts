plugins {
    alias(libs.plugins.wot.android.library)
    alias(libs.plugins.wot.android.compose)
}

android {
    namespace = "com.worldoftamagochi.feature.race"
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.core.ui)
    implementation(projects.core.data)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.kotlinx.coroutines.test)
}
