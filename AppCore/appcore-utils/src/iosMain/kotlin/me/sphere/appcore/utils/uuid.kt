package me.sphere.appcore.utils

actual fun uuid() = autoreleasepool { platform.Foundation.NSUUID.UUID().UUIDString }
