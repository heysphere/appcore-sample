package me.sphere.appcore.utils

actual inline fun <R> autoreleasepool(action: () -> R) = kotlinx.cinterop.autoreleasepool(action)
