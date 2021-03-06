plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
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
                api(project(":appcore-database"))
                implementation(project(":appcore-api-models"))
                api(project(":appcore-spi-network"))
                api(BuildDependency.Kotlin.Coroutines.core)
                implementation(BuildDependency.Kotlin.Serialization.runtime)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":appcore-test-utils-database"))
                implementation(BuildDependency.turbine)
            }
        }
    }
}

