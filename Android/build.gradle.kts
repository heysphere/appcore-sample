//import me.sphere.unicorn.buildsrc.Libs

buildscript {
    repositories {
        google()
        mavenCentral()
    }

    dependencies {
        classpath(me.sphere.unicorn.buildsrc.Libs.androidGradlePlugin)
        classpath(me.sphere.unicorn.buildsrc.Libs.Kotlin.gradlePlugin)
        classpath(me.sphere.unicorn.buildsrc.Libs.Hilt.gradlePlugin)
    }
}

//plugins {
//    id 'com.diffplug.spotless' version '5.7.0'
//}

subprojects {
    repositories {
        google()
        mavenCentral()
    }

//    apply plugin: 'com.diffplug.spotless'
//    spotless {
//        kotlin {
//            target '**/*.kt'
//            targetExclude("$buildDir/**/*.kt")
//            targetExclude('bin/**/*.kt')
//
//            ktlint("0.40.0")
//            licenseHeaderFile rootProject.file('spotless/copyright.kt')
//        }
//    }
}