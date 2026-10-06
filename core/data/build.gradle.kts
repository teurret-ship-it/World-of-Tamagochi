plugins {
    alias(libs.plugins.wot.android.library)
    alias(libs.plugins.wot.serialization)
}

android {
    namespace = "com.mymagicalpet.data"
}

dependencies {
    api(projects.core.sim)
    api(projects.core.network)
    api(libs.androidx.datastore)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)
}
