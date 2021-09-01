import org.gradle.api.Project

const val SQLDELIGHT_URL = "https://raw.githubusercontent.com/heysphere/sqldelight/anders/1.5.0-sphere-5/maven/"
const val APOLLO_URL = "https://raw.githubusercontent.com/andersio/apollo-android/anders/2.5.6-ir-3/maven/"
const val OKIO_URL = "https://raw.githubusercontent.com/andersio/okio/anders/2.10-ir-2/maven/"

object BuildEnvironment {
    fun isJsEnabled(project: Project): Boolean
        = project.property("me.sphere.enableJs").toString().toBoolean()

    val isBuildingForiOSDevice: Boolean
        get() = (System.getenv("SDK_NAME") ?: "").startsWith("iphoneos")
    val isXcodeBuildingForRelease: Boolean
        get() = (System.getenv("CONFIGURATION") ?: "") == "Release"

    const val iosBinaryName = "AppCoreObjC"
    const val kotlinLanguageVersion = "1.5"
    const val jvmTarget = "1.8"
}

object BuildDependency {
    object Version {
        const val kotlin = "1.5.21"
        const val kotlinCoroutines = "1.5.1-native-mt"
        const val kotlinSerialization = "1.2.2"
        const val sqliteJdbc = "3.36.0.1"
        const val sqldelight = "1.5.0" // Currently pointing to https://github.com/heysphere/sqldelight/anders/1.5.0-sphere-5
        const val apollo = "2.5.6" // Currently pointing to https://github.com/andersio/apollo-android/anders/2.5.6-ir-3
        const val junit = "4.13.2"
        const val kotlinAtomicfu = "0.16.2"
        const val kotlinDatetime = "0.2.1"
        const val turbine = "0.6.0"
        const val okio = "2.10.0" // Currently pointing to https://github.com/andersio/okio/anders/2.10.0-ir-2
    }

    const val junit = "junit:junit:${Version.junit}"
    const val okio = "com.squareup.okio:okio-multiplatform:${Version.okio}"

    object Apollo {
        const val apiCommon = "com.apollographql.apollo:apollo-api:${Version.apollo}"
        const val plugin = "com.apollographql.apollo:apollo-gradle-plugin:${Version.apollo}"
    }

    object Kotlin {
        const val multiplatformPlugin = "org.jetbrains.kotlin:kotlin-gradle-plugin:${Version.kotlin}"

        object Serialization {
            const val plugin = "org.jetbrains.kotlin:kotlin-serialization:${Version.kotlin}"
            const val runtime = "org.jetbrains.kotlinx:kotlinx-serialization-json:${Version.kotlinSerialization}"
        }

        object Coroutines {
            const val core = "org.jetbrains.kotlinx:kotlinx-coroutines-core:${Version.kotlinCoroutines}"
        }

        object Atomicfu {
            const val plugin = "org.jetbrains.kotlinx:atomicfu-gradle-plugin:${Version.kotlinAtomicfu}"
        }

        const val Datetime = "org.jetbrains.kotlinx:kotlinx-datetime:${Version.kotlinDatetime}"
    }

    object SQLDelight {
        const val runtime = "com.squareup.sqldelight:runtime:${Version.sqldelight}"
        const val native = "com.squareup.sqldelight:native-driver:${Version.sqldelight}"
        const val js = "com.squareup.sqldelight:sqljs-driver:${Version.sqldelight}"
        const val jdbcDriver = "com.squareup.sqldelight:sqlite-driver:${Version.sqldelight}"
        const val sqliteJdbc = "org.xerial:sqlite-jdbc:${Version.sqliteJdbc}"
        const val plugin = "com.squareup.sqldelight:gradle-plugin:${Version.sqldelight}"
    }

    const val turbine = "app.cash.turbine:turbine:${Version.turbine}"
}
