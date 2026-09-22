package com.septaalfauzan.saku.sentry

import io.sentry.kotlin.multiplatform.PlatformOptionsConfiguration
import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb

internal expect fun platformOptionsConfiguration(dsn: String): PlatformOptionsConfiguration

fun initSentry(dsn: String) {
    if (dsn.isBlank()) return
    Sentry.initWithPlatformOptions(platformOptionsConfiguration(dsn))
    if (Sentry.isEnabled()) {
        Sentry.addBreadcrumb(
            Breadcrumb().apply {
                message = "sentry initialized"
                category = "lifecycle"
            },
        )
    }
}