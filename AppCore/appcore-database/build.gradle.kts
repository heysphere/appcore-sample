import java.io.*

plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("com.squareup.sqldelight")
}

sqldelight {
    database("SqlDatabaseGateway") {
        packageName = "me.sphere.sqldelight"
        dialect = "sqlite:3.25"

        // Enable when we start supporting DB migrations.
        // schemaOutputDirectory = file("src/commonMain/sqldelight/me.sphere.sqldelight/archive")
    }
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
                api(BuildDependency.Kotlin.Serialization.runtime)
                api(BuildDependency.SQLDelight.runtime)
                api(BuildDependency.Kotlin.Coroutines.core)
                api(BuildDependency.Kotlin.Datetime)
                api(project(":appcore-spi-database"))
                api(project(":appcore-spi-logging"))
                api(project(":appcore-spi-firestore"))
                api(project(":appcore-utils"))
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
    }
}

afterEvaluate {
    val permittedExtensions = setOf("sq", "sqm", "kt")
    val dbCommonMainRoot = File(projectDir, "src/commonMain/")
    val apiModelsKotlinSourceRoot = File(project(":appcore-api-models").projectDir, "src/commonMain/kotlin/")
    val sources = listOf(
        File(dbCommonMainRoot, "sqldelight/"),
        File(dbCommonMainRoot, "kotlin/me/sphere/models/"),
        apiModelsKotlinSourceRoot
    )
    val generatedSourceDir = File(projectDir, "build/generated/appcore/")
    val packageDir = File(generatedSourceDir, "me/sphere/sqldelight/")

    val sqlSchemaHash by tasks.registering {
        sources.onEach { inputs.dir(it) }
        outputs.dir(generatedSourceDir)

        doLast {
            val hashInputFiles = inputs.files.filter { it.extension in permittedExtensions }

            val fileHashes = hashInputFiles
                .sortedBy { it.absolutePath }
                .joinToString(
                    separator = ",",
                    transform = { file ->
                        FileInputStream(file).use {
                            org.apache.commons.codec.digest.DigestUtils.sha1Hex(it)
                        }
                    }
                )

            val finalHash = org.apache.commons.codec.digest.DigestUtils.sha1Hex(fileHashes)
            val generatedCode = """
                package me.sphere.sqldelight
                
                const val SQLCORE_SCHEMA_HASH: String = "$finalHash"
            """.trimIndent()

            if (!packageDir.exists()) {
                packageDir.mkdirs()
            }

            val outputSource = File(packageDir, "SchemaHash.kt")
            val sourceInUtf8Bytes = generatedCode.toByteArray(Charsets.UTF_8)

            if (outputSource.exists()) {
                FileInputStream(outputSource).use {
                    val currentSource = it.readBytes()

                    if (!currentSource.contentEquals(sourceInUtf8Bytes)) {
                        logger.warn("Schema Hash has changed to ${finalHash}.")
                    }
                }
            }

            FileOutputStream(outputSource)
                .use { it.write(sourceInUtf8Bytes) }
        }
    }

    kotlin.sourceSets.findByName("commonMain")!!
        .kotlin
        .srcDir(generatedSourceDir.toRelativeString(projectDir))

    kotlin.targets
        .mapNotNull { it.compilations.firstOrNull { it.name.endsWith("main") } }
        .forEach {
            it.compileKotlinTask.dependsOn(sqlSchemaHash)
        }
}
