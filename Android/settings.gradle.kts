rootProject.name = "SphereUnicorn"
include(":app")

val appCore = listOf(
    "appcore-actors-backend0",
    "appcore-actors-graphql",
    "appcore-api-models",
    "appcore-api",
    "appcore-database",
    "appcore-schema-compat-test",
    "appcore-spi-database",
    "appcore-spi-firestore",
    "appcore-spi-logging",
    "appcore-spi-network",
    "appcore-test-utils-database",
    "appcore-test-utils-network",
    "appcore-test-utils",
    "appcore-utils",
)

includeBuild("../AppCore") {
    dependencySubstitution {
        appCore.forEach { module ->
            substitute(module("me.sphere:$module")).using(project(":$module"))
        }
    }
}