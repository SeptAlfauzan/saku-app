package com.septaalfauzan.saku.sentry

import io.sentry.kotlin.multiplatform.PlatformOptionsConfiguration

internal actual fun platformOptionsConfiguration(dsn: String): PlatformOptionsConfiguration = {
    it.dsn = dsn
    it.tracesSampleRate = 1.0
    it.logs.isEnabled = true
}