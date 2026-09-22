package com.septaalfauzan.saku.sentry

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class SentryReporterTest {

    @Test
    fun `disabled factory returns noop`() {
        val reporter = createSentryReporter(false)
        assertIs<NoopSentryReporter>(reporter)
        assertFalse(reporter.enabled)
    }

    @Test
    fun `enabled factory returns real`() {
        val reporter = createSentryReporter(true)
        assertIs<RealSentryReporter>(reporter)
        assertTrue(reporter.enabled)
    }

    @Test
    fun `noop reporter is a no-op`() {
        val reporter: SentryReporter = NoopSentryReporter
        reporter.captureException(RuntimeException("boom"))
        reporter.captureMessage("hello")
        reporter.addBreadcrumb("step", "flow")
        assertFalse(reporter.enabled)
        assertEquals("NoopSentryReporter", reporter.toString())
    }
}