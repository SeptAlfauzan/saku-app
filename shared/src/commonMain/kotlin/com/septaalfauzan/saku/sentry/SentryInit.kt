package com.septaalfauzan.saku.sentry

import io.sentry.kotlin.multiplatform.PlatformOptionsConfiguration
import io.sentry.kotlin.multiplatform.Sentry

internal expect fun platformOptionsConfiguration(dsn: String): PlatformOptionsConfiguration

fun initSentry(dsn: String) {
    if (dsn.isBlank()) return
    Sentry.initWithPlatformOptions(platformOptionsConfiguration(dsn))
}