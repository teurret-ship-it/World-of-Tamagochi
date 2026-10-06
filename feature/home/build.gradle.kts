plugins {
    alias(libs.plugins.wot.android.library)
    alias(libs.plugins.wot.android.compose)
}

android {
    namespace = "com.mymagicalpet.feature.home"
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
