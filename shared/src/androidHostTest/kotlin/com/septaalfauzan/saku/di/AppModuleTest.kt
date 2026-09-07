package com.septaalfauzan.saku.di

import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.test.KoinTest
import org.koin.test.check.checkModules
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.Test

@RunWith(RobolectricTestRunner::class)
class AppModuleTest : KoinTest {

    @Suppress("DEPRECATION")
    @Test
    fun appModuleGraphResolves() {
        AppModuleHostSetup.apply()
        val app = startKoin {
            modules(appModule)
        }
        app.checkModules { }
        stopKoin()
    }
}
