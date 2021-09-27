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
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
}

dependencies {
    implementation(Libs.Kotlin.stdlib)
    implementation(Libs.AndroidX.Lifecycle.viewModelKtx)
    implementation(Libs.AndroidX.Lifecycle.livedata)

    implementation(Libs.Kotlin.Coroutines.android)
    testImplementation(Libs.Kotlin.Coroutines.test)

    testImplementation(Libs.Test.turbine)
    testImplementation(Libs.Test.junit)
    testImplementation(Libs.AndroidX.Test.androidxArchTestCore)
}