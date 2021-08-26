import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType

plugins {
    kotlin("multiplatform")
}

kotlin {
    val presetName = if (BuildEnvironment.isBuildingForiOSDevice) "iosArm64" else "iosX64"
    val buildTypes = when (BuildEnvironment.isBuildingForiOSDevice) {
        true -> when (BuildEnvironment.isXcodeBuildingForRelease) {
            true -> listOf(NativeBuildType.DEBUG, NativeBuildType.RELEASE)
            false -> listOf(NativeBuildType.DEBUG)
        }
        false -> listOf(NativeBuildType.DEBUG)
    }

    targetFromPreset(presets[presetName], "ios") {
        (this as KotlinNativeTarget).apply {
            binaries {
                framework(buildTypes) {
                    baseName = BuildEnvironment.iosBinaryName
                    isStatic = false
                    embedBitcode = org.jetbrains.kotlin.gradle.plugin.mpp.Framework.BitcodeEmbeddingMode.DISABLE

                    linkerOpts("-lsqlite3")

                    export(BuildDependency.Kotlin.Datetime)
                    export(project(":appcore-spi-network"))
                    export(project(":appcore-spi-logging"))
                    export(project(":appcore-spi-database"))
                    export(project(":appcore-api-models"))
                    export(project(":appcore-actors-backend0"))
                    export(project(":appcore-actors-graphql"))
                    export(project(":appcore-api"))
                }
            }
        }
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                api(project(":appcore-spi-network"))
                api(project(":appcore-spi-logging"))
                api(project(":appcore-spi-firestore"))
                api(project(":appcore-spi-database"))
                api(project(":appcore-actors-backend0"))
                api(project(":appcore-actors-graphql"))
                api(project(":appcore-api"))
                api(project(":appcore-api-models"))
                implementation(project(":appcore-utils"))
            }
        }
        val iosMain by getting {
            dependencies {
                implementation(BuildDependency.SQLDelight.native)
            }
        }
    }
}

