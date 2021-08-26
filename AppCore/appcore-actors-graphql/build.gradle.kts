plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.apollographql.apollo")
    kotlin("plugin.serialization")
}

apollo {
    customTypeMapping.set(mapOf("Date" to "kotlinx.datetime.Instant"))
    generateAsInternal.set(true)

    service("sphere") {
        schemaPath.set("me/sphere/graphql/generated/schema.json")
        introspection {
            sourceSetName.set("commonMain")
            endpointUrl.set("https://graphqlzero.almansi.me/api")
        }
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
                implementation(BuildDependency.Kotlin.Serialization.runtime)
                implementation(project(":appcore-database"))
                implementation(project(":appcore-api-models"))

                api(project(":appcore-spi-network"))
                api(project(":appcore-spi-logging"))
                api(BuildDependency.Apollo.apiCommon)
            }
        }
        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation(project(":appcore-test-utils-database"))
                implementation(project(":appcore-test-utils-network"))
            }
        }
    }
}

