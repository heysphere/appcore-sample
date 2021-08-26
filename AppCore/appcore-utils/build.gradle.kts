plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("kotlinx-atomicfu")
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
                implementation(BuildDependency.Kotlin.Serialization.runtime)
                api(BuildDependency.Kotlin.Coroutines.core)
                api(BuildDependency.Kotlin.Datetime)
                api(BuildDependency.okio)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":appcore-test-utils"))
                implementation(BuildDependency.turbine)
            }
        }
        if (BuildEnvironment.isJsEnabled(rootProject)) {
            val jsMain by getting {
                dependencies {
                    api(npm("uuid", "8.3.2"))
                }
            }
        }
    }
}
