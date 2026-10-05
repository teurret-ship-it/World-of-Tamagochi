plugins {
    alias(libs.plugins.wot.jvm.library)
}

dependencies {
    testImplementation(libs.kotlinx.coroutines.core)
}
