package com.septaalfauzan.saku.di

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.check.checkModules
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test
import kotlin.test.assertNotNull
import com.septaalfauzan.saku.domain.importexport.ExportTransactions
import com.septaalfauzan.saku.domain.importexport.ImportTransactions
import com.septaalfauzan.saku.sentry.sentryModule

@RunWith(RobolectricTestRunner::class)
class AppModuleTest : KoinTest {

    @Suppress("DEPRECATION")
    @Test
    fun appModuleGraphResolves() {
        AppModuleHostSetup.apply()
        val app = startKoin {
            modules(appModule, sentryModule)
        }
        app.checkModules { }
        stopKoin()
    }

    @Suppress("DEPRECATION")
    @Test
    fun importExportGraphResolves() {
        AppModuleHostSetup.apply()
        val app = startKoin {
            modules(appModule, sentryModule)
        }
        assertNotNull(app.koin.get<ExportTransactions>())
        assertNotNull(app.koin.get<ImportTransactions>())
        stopKoin()
    }
}
