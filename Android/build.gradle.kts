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

subprojects {
    repositories {
        google()
        mavenCentral()
    }
}