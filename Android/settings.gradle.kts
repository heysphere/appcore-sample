rootProject.name = "SphereUnicorn"

include(":app", ":appcore-android", ":flowredux")

val appCoreModules = listOf(
    "appcore-actors-backend0",
    "appcore-api-models",
    "appcore-api",
    "appcore-database",
    "appcore-spi-database",
    "appcore-spi-logging",
    "appcore-spi-network",
    "appcore-test-utils-database",
    "appcore-test-utils-network",
    "appcore-test-utils",
    "appcore-utils"
)

appCoreModules.forEach { module ->
    include(":$module")
    project(":$module").projectDir = file("../AppCore/$module")
}