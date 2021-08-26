package me.sphere.appcore.utils

import kotlin.native.concurrent.freeze
import kotlin.native.concurrent.ensureNeverFrozen

actual fun <T> T.freeze(): T = freeze()
