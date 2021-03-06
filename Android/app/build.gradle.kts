import me.sphere.unicorn.buildsrc.Android
import me.sphere.unicorn.buildsrc.Libs
import me.sphere.unicorn.buildsrc.propOrDefault

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("dagger.hilt.android.plugin")
}

android {

    compileSdk = Android.compileSdkVersion
    defaultConfig {
        applicationId = "me.sphere.unicorn"
        minSdk = Android.minSdkVersion
        targetSdk = Android.targetSdkVersion
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "GITHUB_TOKEN", propOrDefault("GITHUB_TOKEN", ""))
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }

    buildFeatures {
        compose = true

        // Disable unused AGP features
        aidl = false
        renderScript = false
        resValues = false
        shaders = false
    }

    composeOptions {
        kotlinCompilerExtensionVersion = Libs.AndroidX.Compose.version
    }

    packagingOptions {
        // Multiple dependency bring these files in. Exclude them to enable
        // our test APK to build (has no effect on our AARs)
        resources.excludes += "/META-INF/AL2.0"
        resources.excludes += "/META-INF/LGPL2.1"
    }
}

dependencies {
    implementation(Libs.Kotlin.stdlib)
    implementation(Libs.Kotlin.Coroutines.android)
    implementation(Libs.AndroidX.material)

    implementation(Libs.Accompanist.insets)
    implementation(Libs.Accompanist.placeholder)
    implementation(Libs.AndroidX.Activity.activityCompose)
    implementation(Libs.AndroidX.appcompat)
    implementation(Libs.AndroidX.Compose.runtime)
    implementation(Libs.AndroidX.Compose.runtimeLivedata)
    implementation(Libs.AndroidX.Compose.foundation)
    implementation(Libs.AndroidX.Compose.material)
    implementation(Libs.AndroidX.Compose.layout)
    implementation(Libs.AndroidX.Compose.animation)
    implementation(Libs.AndroidX.Compose.tooling)
    implementation(Libs.AndroidX.Lifecycle.viewModelCompose)
    implementation(Libs.AndroidX.Compose.navigation)
    implementation(Libs.Hilt.navigationCompose)

    implementation(Libs.AndroidX.Lifecycle.viewModelKtx)
    implementation(Libs.Hilt.android)
    kapt(Libs.Hilt.compiler)

    implementation(Libs.Coil.coilCompose)
    implementation(Libs.Utils.timber)

    debugImplementation(Libs.AndroidX.Compose.uiTestManifest)

    implementation(Libs.Square.okHttpLoggingInterceptor)
    implementation(project(Libs.FlowRedux))

    // AppCore
    implementation(project(Libs.AppCore.api))
    implementation(project(Libs.AppCore.apiModels))
    implementation(project(Libs.AppCore.database))
    implementation(project(Libs.AppCore.backend0Actors))
    implementation(project(Libs.AppCore.appcoreAndroid))

    androidTestImplementation(Libs.AndroidX.Test.core)
    androidTestImplementation(Libs.AndroidX.Test.runner)
    androidTestImplementation(Libs.AndroidX.Test.Ext.junit)
}