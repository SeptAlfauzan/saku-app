package com.septaalfauzan.saku.sentry

import cocoapods.Sentry.SentryEvent
import cocoapods.Sentry.SentryException
import cocoapods.Sentry.experimental
import io.sentry.kotlin.multiplatform.PlatformOptionsConfiguration
import platform.Foundation.NSHomeDirectory
import platform.Foundation.NSNumber
import platform.Foundation.NSTemporaryDirectory

internal actual fun platformOptionsConfiguration(dsn: String): PlatformOptionsConfiguration = {
    it.dsn = dsn
    it.tracesSampleRate = NSNumber(1.0)
    it.experimental().setEnableLogs(true)
    it.beforeSend = { event ->
        scrubPii(event)
        event
    }
}

private fun scrubPii(event: SentryEvent?) {
    val sensitivePrefixes = listOfNotNull(NSHomeDirectory(), NSTemporaryDirectory())
    if (sensitivePrefixes.isEmpty()) return
    val exceptions = event?.exceptions() ?: return
    for (exceptionRef in exceptions) {
        val exception = exceptionRef as? SentryException ?: continue
        val message = exception.value()
        if (sensitivePrefixes.any { message.contains(it) }) {
            exception.setValue("<redacted>")
        }
    }
}