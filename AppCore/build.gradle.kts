buildscript {
    repositories {
        maven(SQLDELIGHT_URL) { // For SQLDelight workarounds
            content {
                includeGroup("com.squareup.sqldelight")
            }
        }
        maven(APOLLO_URL) { // For Apollo JS IR builds
            content {
                includeModuleByRegex("com.apollographql.apollo", "apollo-api(?:.+)?")
            }
        }
        maven(OKIO_URL) { // For OKIO JS IR builds
            content {
                includeGroup("com.squareup.okio")
            }
        }
        maven("https://plugins.gradle.org/m2/")
        google()

        // For SQLDelight dependency on IntelliJ Platform
        maven("https://www.jetbrains.com/intellij-repository/releases")
        maven("https://cache-redirector.jetbrains.com/intellij-dependencies")

        mavenCentral()
    }

    dependencies {
        classpath(BuildDependency.Kotlin.multiplatformPlugin)
        classpath(BuildDependency.Kotlin.Serialization.plugin)
        classpath(BuildDependency.SQLDelight.plugin)
        classpath(BuildDependency.Apollo.plugin)
        classpath(BuildDependency.Kotlin.Atomicfu.plugin)

        // hashing functions for the SQLCore module.
        classpath("commons-codec:commons-codec:1.15")
    }
}

subprojects {
    repositories {
        maven(SQLDELIGHT_URL) { // For SQLDelight workarounds
            content {
                includeGroup("com.squareup.sqldelight")
            }
        }
        maven(APOLLO_URL) { // For Apollo JS IR builds
            content {
                includeModuleByRegex("com.apollographql.apollo", "apollo-api(?:.+)?")
            }
        }
        maven(OKIO_URL) { // For OKIO JS IR builds
            content {
                includeGroup("com.squareup.okio")
            }
        }
        mavenCentral()
        maven("https://maven.google.com/")
        google()
    }

    configurations.all {
        resolutionStrategy {
            force(BuildDependency.Kotlin.Coroutines.core)
        }
    }

    group = "me.sphere.appcore"
    version = "0.0.1"

    afterEvaluate {
        extensions
            .getByType(org.jetbrains.kotlin.gradle.plugin.KotlinSourceSetContainer::class)
            .sourceSets
            .all {
                languageSettings.run {
                    languageVersion = BuildEnvironment.kotlinLanguageVersion
                    useExperimentalAnnotation("kotlin.RequiresOptIn")
                    useExperimentalAnnotation("kotlinx.coroutines.ExperimentalCoroutinesApi")
                    useExperimentalAnnotation("kotlin.time.ExperimentalTime")
                }
            }

        tasks
            .withType(org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink::class)
            .filter { it.processTests }
            .forEach { it.binary.linkerOpts("-lsqlite3") }
    }

    tasks.withType<Test> {
        finalizedBy(testReport)
    }
}

repositories {
    mavenCentral()
}

val testReport by tasks.registering(TestReport::class) {
    group = "verification"
    destinationDir = file("$buildDir/reports/allTests")
    // Include the results from the `test` task in all subprojects
    reportOn(subprojects.flatMap { it.tasks.filterIsInstance<Test>() })
}
