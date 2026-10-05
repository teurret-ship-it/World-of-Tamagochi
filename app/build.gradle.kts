plugins {
    alias(libs.plugins.wot.android.application)
    alias(libs.plugins.wot.android.compose)
}

android {
    namespace = "com.worldoftamagochi"

    defaultConfig {
        applicationId = "com.worldoftamagochi"
        versionCode = 1
        versionName = "0.1.0"
        // Racing server (ADR-008). Empty = offline build: online features stay hidden.
        val serverUrl = providers.gradleProperty("wot.serverUrl").orElse("").get()
        buildConfigField("String", "SERVER_URL", "\"$serverUrl\"")
    }

    buildFeatures {
        buildConfig = true
    }

    // Test builds only: a public key committed on purpose, so every CI build
    // installs over the previous one and keeps the save. The Play release key
    // never lives in the repository (iteration 34).
    signingConfigs {
        create("test") {
            storeFile = file("test-signing.keystore")
            storePassword = "worldoftamagochi"
            keyAlias = "test"
            keyPassword = "worldoftamagochi"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("test")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(projects.core.designsystem)
    implementation(projects.feature.home)
    implementation(projects.feature.race)
    implementation(projects.feature.shop)
    implementation(projects.core.ui)
    implementation(projects.core.data)
    implementation(projects.core.network)
    implementation(libs.ktor.client.android)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
