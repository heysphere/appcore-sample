rootProject.name = "SphereUnicorn"
include(":app", ":flowredux")

val appCoreModules = listOf(
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

appCoreModules.forEach { module ->
    include(":$module")
    project(":$module").projectDir = file("../AppCore/$module")
}