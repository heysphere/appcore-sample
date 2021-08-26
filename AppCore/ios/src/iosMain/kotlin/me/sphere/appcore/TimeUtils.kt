package me.sphere.appcore

import platform.Foundation.*
import kotlinx.datetime.Instant
import kotlinx.datetime.toKotlinInstant

public fun toInstant(date: NSDate): Instant = date.toKotlinInstant()