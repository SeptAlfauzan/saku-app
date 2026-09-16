package com.septaalfauzan.saku.di

import kotlinx.coroutines.flow.first
import com.septaalfauzan.saku.data.dao.CategoryDao
import com.septaalfauzan.saku.data.dao.NotificationSourceDao
import com.septaalfauzan.saku.data.dao.ParserKeywordDao
import com.septaalfauzan.saku.data.dao.SettingsDao
import com.septaalfauzan.saku.data.dao.TransactionDao
import com.septaalfauzan.saku.data.database.AppDatabase
import com.septaalfauzan.saku.data.database.buildRoomDatabase
import com.septaalfauzan.saku.data.database.createDatabaseBuilder
import com.septaalfauzan.saku.data.remote.ApiService
import com.septaalfauzan.saku.data.remote.ApiServiceImpl
import com.septaalfauzan.saku.data.remote.RemoteClient
import com.septaalfauzan.saku.data.repository.RemoteOcrRepository
import com.septaalfauzan.saku.data.repository.RoomNotificationSettingsRepository
import com.septaalfauzan.saku.data.repository.RoomTransactionRepository
import com.septaalfauzan.saku.domain.importexport.ExportTransactions
import com.septaalfauzan.saku.domain.importexport.ImportTransactions
import com.septaalfauzan.saku.domain.repository.NotificationSettingsRepository
import com.septaalfauzan.saku.domain.repository.OcrRepository
import com.septaalfauzan.saku.domain.repository.TransactionRepository
import com.septaalfauzan.saku.domain.usecase.AddNotificationSource
import com.septaalfauzan.saku.domain.usecase.AddTransaction
import com.septaalfauzan.saku.domain.usecase.DeleteNotificationSource
import com.septaalfauzan.saku.domain.usecase.DeleteTransaction
import com.septaalfauzan.saku.domain.usecase.GetMonthlySummary
import com.septaalfauzan.saku.domain.usecase.GetReceiptValue
import com.septaalfauzan.saku.domain.usecase.ObserveAutoConfirm
import com.septaalfauzan.saku.domain.usecase.ObserveCategories
import com.septaalfauzan.saku.domain.usecase.ObserveNotificationSources
import com.septaalfauzan.saku.domain.usecase.ObserveParserKeywords
import com.septaalfauzan.saku.domain.usecase.ObservePending
import com.septaalfauzan.saku.domain.usecase.ObserveTrackingEnabled
import com.septaalfauzan.saku.domain.usecase.ObserveTransactions
import com.septaalfauzan.saku.domain.usecase.SetAutoConfirm
import com.septaalfauzan.saku.domain.usecase.SetNotificationSourceEnabled
import com.septaalfauzan.saku.domain.usecase.SetTrackingEnabled
import com.septaalfauzan.saku.domain.usecase.SetTransactionStatus
import com.septaalfauzan.saku.domain.usecase.UpdateParserKeywords
import com.septaalfauzan.saku.domain.usecase.UpdateTransaction
import com.septaalfauzan.saku.notification.duplicate.DuplicateDetector
import com.septaalfauzan.saku.notification.engine.NotificationParserEngine
import com.septaalfauzan.saku.notification.provider.ParserRegistry
import com.septaalfauzan.saku.notification.usecase.ProcessNotificationUseCase
import com.septaalfauzan.saku.ui.addedit.AddEditTransactionViewModel
import com.septaalfauzan.saku.ui.dashboard.DashboardViewModel
import com.septaalfauzan.saku.ui.detail.TransactionDetailViewModel
import com.septaalfauzan.saku.ui.importexport.ExportCsvViewModel
import com.septaalfauzan.saku.ui.importexport.ImportCsvViewModel
import com.septaalfauzan.saku.ui.review.ReviewQueueViewModel
import com.septaalfauzan.saku.ui.scanreceipt.ScanReceiptViewmodel
import com.septaalfauzan.saku.ui.tracking.TrackingViewModel
import com.septaalfauzan.saku.ui.transactions.TransactionListViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { buildRoomDatabase(createDatabaseBuilder()) }
    single { get<AppDatabase>().transactionDao() }
    single { get<AppDatabase>().categoryDao() }
    single { get<AppDatabase>().sourceDao() }
    single { get<AppDatabase>().settingsDao() }
    single { get<AppDatabase>().keywordDao() }
    single { RemoteClient().createHttpClient() }
    single<ApiService> { ApiServiceImpl(get(), "https://saku-api.septaalfauzan.my.id") }
    single<TransactionRepository> { RoomTransactionRepository(get(), get(), get()) }
    single<NotificationSettingsRepository> {
        val registry = get<ParserRegistry>()
        RoomNotificationSettingsRepository(get(), get(), get()).apply {
            onConfigChanged = {
                val sources = observeSources().first()
                val allKeywords =
                    sources.associate { it.packageName to getKeywords(it.packageName) }
                registry.rebuild(sources, allKeywords)
            }
        }
    }
    single<OcrRepository> { RemoteOcrRepository(get()) }

    singleOf(::ObserveTransactions)
    singleOf(::ObserveCategories)
    singleOf(::AddTransaction)
    singleOf(::UpdateTransaction)
    singleOf(::DeleteTransaction)
    singleOf(::GetMonthlySummary)
    singleOf(::ObservePending)
    singleOf(::SetTransactionStatus)
    singleOf(::ObserveNotificationSources)
    singleOf(::SetNotificationSourceEnabled)
    singleOf(::AddNotificationSource)
    singleOf(::DeleteNotificationSource)
    singleOf(::UpdateParserKeywords)
    singleOf(::ObserveParserKeywords)
    singleOf(::ObserveTrackingEnabled)
    singleOf(::SetTrackingEnabled)
    singleOf(::ObserveAutoConfirm)
    singleOf(::SetAutoConfirm)
    singleOf(::GetReceiptValue)
    singleOf(::ExportTransactions)
    singleOf(::ImportTransactions)

    single { ParserRegistry() }
    single { NotificationParserEngine(get()) }
    single { DuplicateDetector(get()) }
    single { ProcessNotificationUseCase(get(), get(), get(), get(), get()) }

    viewModelOf(::DashboardViewModel)
    viewModelOf(::TransactionListViewModel)
    viewModelOf(::TrackingViewModel)
    viewModelOf(::ReviewQueueViewModel)
    viewModelOf(::ScanReceiptViewmodel)
    viewModelOf(::ExportCsvViewModel)
    viewModelOf(::ImportCsvViewModel)
    viewModel { params ->
        TransactionDetailViewModel(get(), get(), params.getOrNull() ?: "")
    }
    viewModel { params ->
        AddEditTransactionViewModel(
            get(), get(), get(), get(),
            // Koin 4.2.1 lacks typed getOrNull(Int); positional values list access
            params.values.getOrNull(0) as? String,  // transactionId
            params.values.getOrNull(1) as? String,   // prefillJson
            params.values.getOrNull(2) as? Boolean ?: false, //editingScan
        )
    }
}
