plugins {
    kotlin("multiplatform")
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
                api(kotlin("test-common"))
                api(kotlin("test-annotations-common"))
                api(project(":appcore-test-utils"))
                api(project(":appcore-database"))
                api(project(":appcore-spi-database"))
                api(BuildDependency.Kotlin.Coroutines.core)
            }
        }
        val iosMain by getting {
            dependencies {
                implementation(BuildDependency.SQLDelight.native)
            }
        }
        val jvmMain by getting {
            dependencies {
                implementation(kotlin("test-junit"))
                implementation(BuildDependency.SQLDelight.jdbcDriver)
                implementation(BuildDependency.SQLDelight.sqliteJdbc)
            }
        }
        if (BuildEnvironment.isJsEnabled(rootProject)) {
            val jsMain by getting {
                dependencies {
                    implementation(BuildDependency.SQLDelight.js)
                }
            }
        }
    }
}
