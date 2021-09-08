import org.gradle.kotlin.dsl.`kotlin-dsl`

repositories {
    mavenCentral()
}

plugins {
    `kotlin-dsl`
}

sourceSets {
    main {
        java {
            srcDirs(file("src/main/java"), file("../../AppCore/buildSrc/src/main/kotlin"))
        }
    }
}