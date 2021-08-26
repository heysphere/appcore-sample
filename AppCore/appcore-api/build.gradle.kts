plugins {
    kotlin("multiplatform")
    id("org.jetbrains.kotlin.plugin.serialization")
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
                implementation(BuildDependency.Kotlin.Coroutines.core)
                implementation(BuildDependency.Kotlin.Serialization.runtime)
                implementation(project(":appcore-database"))
                api(project(":appcore-spi-logging"))
                api(project(":appcore-spi-database"))
                api(project(":appcore-spi-network"))
                api(project(":appcore-api-models"))
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":appcore-test-utils-database"))
                implementation(BuildDependency.turbine)
            }
        }
        val iosTest by getting {
            dependencies {
                implementation(BuildDependency.SQLDelight.native)
            }
        }
        val jvmTest by getting {
            dependencies {
                implementation(BuildDependency.junit)
                implementation(BuildDependency.SQLDelight.jdbcDriver)
                implementation(BuildDependency.SQLDelight.sqliteJdbc)
            }
        }

        all {
            languageSettings.useExperimentalAnnotation("kotlin.js.ExperimentalJsExport")
            languageSettings.useExperimentalAnnotation("kotlin.time.ExperimentalTime")
        }
    }
}
