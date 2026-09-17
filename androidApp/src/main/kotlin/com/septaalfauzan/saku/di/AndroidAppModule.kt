package com.septaalfauzan.saku.di

import com.septaalfauzan.saku.ui.configure.ConfigureParserViewModel
import com.septaalfauzan.saku.ui.scanner.ScannerViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val androidAppModule = module {
    single<CoroutineDispatcher> { Dispatchers.IO }
    viewModelOf(::ConfigureParserViewModel)
    viewModelOf(::ScannerViewModel)
}
