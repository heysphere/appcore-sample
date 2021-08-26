plugins {
    kotlin("multiplatform")
}

kotlin {
    jvm()

    if (BuildEnvironment.isBuildingForiOSDevice) iosArm64("ios") else iosX64("ios")

    if (BuildEnvironment.isJsEnabled(rootProject)) {
        js {
            browser()
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(BuildDependency.SQLDelight.runtime)
                api(BuildDependency.Kotlin.Serialization.runtime)
            }
        }
    }
}
