import me.sphere.unicorn.buildsrc.configureAppCoreRepositories

buildscript {
    repositories {
        maven(SQLDELIGHT_URL) { // For SQLDelight workarounds
            content {
                includeGroup("com.squareup.sqldelight")
            }
        }
        maven(APOLLO_URL) { // For Apollo JS IR builds
            content {
                includeModuleByRegex("com.apollographql.apollo", "apollo-api(?:.+)?")
            }
        }
        maven(OKIO_URL) { // For OKIO JS IR builds
            content {
                includeGroup("com.squareup.okio")
            }
        }
        maven("https://plugins.gradle.org/m2/")

        // For SQLDelight dependency on IntelliJ Platform
        maven("https://www.jetbrains.com/intellij-repository/releases")
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")
        google()
        mavenCentral()
    }

    dependencies {
        classpath(me.sphere.unicorn.buildsrc.Libs.androidGradlePlugin)
        classpath(me.sphere.unicorn.buildsrc.Libs.Kotlin.gradlePlugin)
        classpath(me.sphere.unicorn.buildsrc.Libs.Hilt.gradlePlugin)
        classpath(me.sphere.unicorn.buildsrc.Libs.AppCorePlugins.kotlinSerialization)
        classpath(me.sphere.unicorn.buildsrc.Libs.AppCorePlugins.apollo)
        classpath(me.sphere.unicorn.buildsrc.Libs.AppCorePlugins.atomicfu)
        classpath(me.sphere.unicorn.buildsrc.Libs.AppCorePlugins.sqldelight)
    }
}

allprojects {
    repositories {
        configureAppCoreRepositories()
        mavenCentral()
        google()
    }
}