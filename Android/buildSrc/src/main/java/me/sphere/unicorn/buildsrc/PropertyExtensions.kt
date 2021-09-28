package me.sphere.unicorn.buildsrc

import org.gradle.api.Project

fun Project.propOrDefault(key: String, defaultValue: String): String {
   val value = if (hasProperty(key)) properties[key]!! else defaultValue

    return "\"$value\""
}