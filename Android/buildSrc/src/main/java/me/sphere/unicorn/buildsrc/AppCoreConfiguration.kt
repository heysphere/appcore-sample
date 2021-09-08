package me.sphere.unicorn.buildsrc

import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.kotlin.dsl.maven

private const val SQLDELIGHT_URL = "https://raw.githubusercontent.com/heysphere/sqldelight/anders/1.5.0-sphere-5/maven/"
private const val APOLLO_URL = "https://raw.githubusercontent.com/andersio/apollo-android/anders/2.5.6-ir-3/maven/"
private const val OKIO_URL = "https://raw.githubusercontent.com/andersio/okio/anders/2.10-ir-2/maven/"

fun RepositoryHandler.configureAppCoreRepositories() {
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
}