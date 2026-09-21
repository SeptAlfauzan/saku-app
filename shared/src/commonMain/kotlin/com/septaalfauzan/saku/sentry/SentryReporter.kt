package com.septaalfauzan.saku.sentry

import io.sentry.kotlin.multiplatform.Sentry
import io.sentry.kotlin.multiplatform.protocol.Breadcrumb

interface SentryReporter {
    val enabled: Boolean
    fun captureException(t: Throwable)
    fun captureMessage(message: String)
    fun addBreadcrumb(message: String, category: String? = null)
}

object NoopSentryReporter : SentryReporter {
    override val enabled: Boolean = false
    override fun captureException(t: Throwable) {}
    override fun captureMessage(message: String) {}
    override fun addBreadcrumb(message: String, category: String?) {}
    override fun toString(): String = "NoopSentryReporter"
}

class RealSentryReporter : SentryReporter {
    override val enabled: Boolean = true
    override fun captureException(t: Throwable) {
        Sentry.captureException(t)
    }
    override fun captureMessage(message: String) {
        Sentry.captureMessage(message)
    }
    override fun addBreadcrumb(message: String, category: String?) {
        Sentry.addBreadcrumb(Breadcrumb().apply {
            this.message = message
            category?.let { this.category = it }
        })
    }
}

fun createSentryReporter(enabled: Boolean): SentryReporter =
    if (enabled) RealSentryReporter() else NoopSentryReporter