package me.sphere.appcore.utils

/**
 * On iOS, this wraps the action lambda around an Autorelease Pool.
 * On Android/JS, this invokes the action directly.
 */
expect inline fun <R> autoreleasepool(action: () -> R): R
