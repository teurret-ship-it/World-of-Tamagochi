plugins {
    alias(libs.plugins.wot.jvm.library)
    alias(libs.plugins.wot.coverage)
}

dependencies {
    api(projects.core.model)
    testImplementation(libs.kotlinx.coroutines.core)
}
