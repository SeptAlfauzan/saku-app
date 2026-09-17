package com.septaalfauzan.saku.di

import com.septaalfauzan.saku.ui.configure.ConfigureParserViewModel
import com.septaalfauzan.saku.ui.scanner.ScannerViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val androidAppModule = module {
    viewModelOf(::ConfigureParserViewModel)
    viewModelOf(::ScannerViewModel)
}
