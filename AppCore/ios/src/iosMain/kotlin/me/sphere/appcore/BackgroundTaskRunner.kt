package me.sphere.appcore

import platform.UIKit.UIBackgroundTaskIdentifier

interface BackgroundTaskRunner {
    fun beginTask(name: String): UIBackgroundTaskIdentifier
    fun endTask(identifier: UIBackgroundTaskIdentifier)
}