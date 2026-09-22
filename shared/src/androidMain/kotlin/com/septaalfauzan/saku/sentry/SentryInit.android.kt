package com.septaalfauzan.saku.sentry

import io.sentry.Hint
import io.sentry.SentryEvent
import io.sentry.kotlin.multiplatform.PlatformOptionsConfiguration

internal actual fun platformOptionsConfiguration(dsn: String): PlatformOptionsConfiguration = {
    it.dsn = dsn
    it.tracesSampleRate = 1.0
    it.setAnrEnabled(true)
    it.logs.isEnabled = true
    it.beforeSend = { event: SentryEvent, _: Hint -> event }
}