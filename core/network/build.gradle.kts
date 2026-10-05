plugins {
    alias(libs.plugins.wot.jvm.library)
    alias(libs.plugins.wot.serialization)
}

dependencies {
    api(projects.core.api)
    api(projects.core.sim)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.kotlinx.coroutines.core)
    testImplementation(libs.ktor.client.mock)
    testImplementation(projects.server)
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlinx.coroutines.core)
}
