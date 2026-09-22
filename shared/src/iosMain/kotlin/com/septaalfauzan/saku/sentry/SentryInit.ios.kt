package com.septaalfauzan.saku.sentry

import io.sentry.kotlin.multiplatform.PlatformOptionsConfiguration
import platform.Foundation.NSNumber

internal actual fun platformOptionsConfiguration(dsn: String): PlatformOptionsConfiguration = {
    it.dsn = dsn
    it.tracesSampleRate = NSNumber(1.0)
    it.debug = true
}