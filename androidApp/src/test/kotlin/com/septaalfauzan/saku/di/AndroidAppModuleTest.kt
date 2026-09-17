package com.septaalfauzan.saku.di

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import com.septaalfauzan.saku.ui.scanner.ScannerViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.check.checkModules
import kotlin.test.assertNotNull
import kotlin.test.assertSame

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class AndroidAppModuleTest : KoinTest {

    @Suppress("DEPRECATION")
    @Test
    fun appGraphResolvesScannerViewModelAndDispatcher() {
        AndroidContextHolder.applicationContext = ApplicationProvider.getApplicationContext()
        val app = startKoin {
            modules(appModule, androidAppModule)
        }

        assertNotNull(app.koin.get<ScannerViewModel>())
        val dispatcher = app.koin.get<CoroutineDispatcher>()
        assertNotNull(dispatcher)
        assertSame(Dispatchers.IO, dispatcher)

        app.checkModules()
        stopKoin()
    }
}