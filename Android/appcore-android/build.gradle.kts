import me.sphere.unicorn.buildsrc.Android
import me.sphere.unicorn.buildsrc.Libs

plugins {
    id("com.android.library")
    id("kotlin-android")
}

android {
    compileSdk = Android.compileSdkVersion
    defaultConfig {
        minSdk = Android.minSdkVersion
        targetSdk = Android.targetSdkVersion
        consumerProguardFile("consumer-rules.pro")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
}

dependencies {
    implementation(Libs.Kotlin.stdlib)
    implementation(Libs.AndroidX.annotation)
    implementation(Libs.AndroidX.datastore)
    implementation(Libs.Utils.timber)
    implementation(Libs.Square.okhttp)

    // AppCore
    implementation(project(Libs.AppCore.api))
    implementation(project(Libs.AppCore.database))
    implementation(project(Libs.AppCore.spiDatabase))
    implementation(project(Libs.AppCore.spiNetwork))
    implementation(project(Libs.AppCore.spiLogging))
}