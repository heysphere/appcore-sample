package me.sphere.unicorn.buildsrc

object Android {
    const val compileSdkVersion = 30
    const val minSdkVersion = 23
    const val targetSdkVersion = 30
}

object Libs {
    const val androidGradlePlugin = "com.android.tools.build:gradle:7.0.0"

    object Accompanist {
        const val version = "0.16.0"
        const val insets = "com.google.accompanist:accompanist-insets:$version"
    }

    object AppCorePlugins {
        const val kotlinSerialization = "org.jetbrains.kotlin:kotlin-serialization:1.5.21"
        const val apollo = "com.apollographql.apollo:apollo-gradle-plugin:2.5.6"
        const val atomicfu = "org.jetbrains.kotlinx:atomicfu-gradle-plugin:0.16.2"
        const val sqldelight = "com.squareup.sqldelight:gradle-plugin:1.5.0"
    }

    object Kotlin {
        private const val version = "1.5.21"
        const val stdlib = "org.jetbrains.kotlin:kotlin-stdlib-jdk8:$version"
        const val gradlePlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:$version"

        object Coroutines {
            private const val version = "1.5.0"
            const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:$version"
            const val test = "org.jetbrains.kotlinx:kotlinx-coroutines-test:$version"
        }
    }

    object AndroidX {
        object Activity {
            const val activityCompose = "androidx.activity:activity-compose:1.3.1"
        }

        const val appcompat = "androidx.appcompat:appcompat:1.3.0"
        const val material = "com.google.android.material:material:1.3.0"

        object Compose {
            const val version = "1.0.1"

            const val runtime = "androidx.compose.runtime:runtime:$version"
            const val runtimeLivedata = "androidx.compose.runtime:runtime-livedata:$version"
            const val material = "androidx.compose.material:material:$version"
            const val foundation = "androidx.compose.foundation:foundation:$version"
            const val layout = "androidx.compose.foundation:foundation-layout:$version"
            const val tooling = "androidx.compose.ui:ui-tooling:$version"
            const val animation = "androidx.compose.animation:animation:$version"
            const val uiTestManifest = "androidx.compose.ui:ui-test-manifest:$version"
        }

        object Lifecycle {
            private const val version = "2.3.1"
            const val viewModelCompose = "androidx.lifecycle:lifecycle-viewmodel-compose:1.0.0-alpha07"
            const val viewModelKtx = "androidx.lifecycle:lifecycle-viewmodel-ktx:$version"
        }

        object Test {
            private const val version = "1.4.0"
            const val core = "androidx.test:core:$version"
            const val runner = "androidx.test:runner:$version"
            const val rules = "androidx.test:rules:$version"
            object Ext {
                private const val version = "1.1.2"
                const val junit = "androidx.test.ext:junit-ktx:$version"
            }
        }
    }

    object Hilt {
        private const val version = "2.38.1"

        const val gradlePlugin = "com.google.dagger:hilt-android-gradle-plugin:$version"
        const val android = "com.google.dagger:hilt-android:$version"
        const val compiler = "com.google.dagger:hilt-compiler:$version"
        const val testing = "com.google.dagger:hilt-android-testing:$version"
    }

    object AppCore {
        const val api = ":appcore-api"
        const val apiModels = ":appcore-api-models"
    }

    object Coil {
        const val coilCompose = "io.coil-kt:coil-compose:1.3.0"
    }
}