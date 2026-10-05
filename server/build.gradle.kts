plugins {
    alias(libs.plugins.wot.server)
}

application {
    mainClass.set("com.worldoftamagochi.server.MainKt")
}

dependencies {
    implementation(projects.core.sim)
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.ktor.server.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.logback.classic)
    testImplementation(libs.ktor.server.test.host)
}
