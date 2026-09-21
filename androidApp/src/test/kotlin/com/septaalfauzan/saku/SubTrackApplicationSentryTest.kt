package com.septaalfauzan.saku

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.septaalfauzan.saku.di.AndroidContextHolder
import com.septaalfauzan.saku.di.androidAppModule
import com.septaalfauzan.saku.di.appModule
import com.septaalfauzan.saku.sentry.NoopSentryReporter
import com.septaalfauzan.saku.sentry.SentryReporter
import com.septaalfauzan.saku.sentry.sentryModule
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.inject
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class SubTrackApplicationSentryTest : KoinTest {
    private val reporter: SentryReporter by inject()

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `sentry module resolves noop reporter when dsn blank`() {
        AndroidContextHolder.applicationContext =
            ApplicationProvider.getApplicationContext<Application>()
        startKoin {
            androidContext(
                ApplicationProvider.getApplicationContext<Application>()
            )
            modules(appModule, androidAppModule, sentryModule)
        }
        assertTrue(reporter is NoopSentryReporter)
        assertTrue(!reporter.enabled)
    }
}