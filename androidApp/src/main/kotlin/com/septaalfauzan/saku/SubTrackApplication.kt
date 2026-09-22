package com.septaalfauzan.saku

import android.app.Application
import com.septaalfauzan.saku.di.AndroidContextHolder
import com.septaalfauzan.saku.di.androidAppModule
import com.septaalfauzan.saku.di.appModule
import com.septaalfauzan.saku.sentry.initSentry
import com.septaalfauzan.saku.sentry.sentryModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class SubTrackApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initSentry(BuildConfig.SENTRY_DSN)
        AndroidContextHolder.applicationContext = applicationContext
        startKoin {
            androidContext(this@SubTrackApplication)
            modules(appModule, androidAppModule, sentryModule)
        }
    }
}
