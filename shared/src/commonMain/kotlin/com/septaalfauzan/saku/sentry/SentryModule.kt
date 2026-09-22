package com.septaalfauzan.saku.sentry

import io.sentry.kotlin.multiplatform.Sentry
import org.koin.dsl.module

val sentryModule = module {
    single<SentryReporter> { createSentryReporter(Sentry.isEnabled()) }
}