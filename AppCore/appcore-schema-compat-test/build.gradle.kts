plugins {
    kotlin("multiplatform")
    kotlin("plugin.serialization")
}

kotlin {
    jvm()

    sourceSets {
        val jvmTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":appcore-database"))
                implementation(project(":appcore-api-models"))
                implementation(BuildDependency.Kotlin.Serialization.runtime)

                implementation("org.reflections:reflections:0.9.12")
                implementation(kotlin("reflect"))
            }
        }
    }
}

afterEvaluate {
    val enableRecordMode by tasks.registering {
        doFirst {
            (tasks.getByName("jvmTest") as Test)
                .environment("SNAPSHOT_RECORD_MODE", "1")
        }
    }

    val recordSnapshot by tasks.registering {
        dependsOn(enableRecordMode, tasks.getByName("jvmTest"))
        group = "verification"
    }
}
