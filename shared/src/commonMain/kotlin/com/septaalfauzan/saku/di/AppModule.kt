package com.septaalfauzan.saku.di

import com.septaalfauzan.saku.data.dao.CategoryDao
import com.septaalfauzan.saku.data.dao.NotificationSourceDao
import com.septaalfauzan.saku.data.dao.ParserKeywordDao
import com.septaalfauzan.saku.data.dao.SettingsDao
import com.septaalfauzan.saku.data.dao.TransactionDao
import com.septaalfauzan.saku.data.database.AppDatabase
import com.septaalfauzan.saku.data.database.buildRoomDatabase
import com.septaalfauzan.saku.data.database.createDatabaseBuilder
import com.septaalfauzan.saku.data.repository.RoomNotificationSettingsRepository
import com.septaalfauzan.saku.data.repository.RoomTransactionRepository
import com.septaalfauzan.saku.domain.repository.NotificationSettingsRepository
import com.septaalfauzan.saku.domain.repository.TransactionRepository
import com.septaalfauzan.saku.domain.usecase.AddTransaction
import com.septaalfauzan.saku.domain.usecase.DeleteTransaction
import com.septaalfauzan.saku.domain.usecase.GetMonthlySummary
import com.septaalfauzan.saku.domain.usecase.ObserveAutoConfirm
import com.septaalfauzan.saku.domain.usecase.ObserveCategories
import com.septaalfauzan.saku.domain.usecase.ObserveNotificationSources
import com.septaalfauzan.saku.domain.usecase.ObservePending
import com.septaalfauzan.saku.domain.usecase.ObserveTrackingEnabled
import com.septaalfauzan.saku.domain.usecase.ObserveTransactions
import com.septaalfauzan.saku.domain.usecase.SetAutoConfirm
import com.septaalfauzan.saku.domain.usecase.SetNotificationSourceEnabled
import com.septaalfauzan.saku.domain.usecase.SetTrackingEnabled
import com.septaalfauzan.saku.domain.usecase.SetTransactionStatus
import com.septaalfauzan.saku.domain.usecase.UpdateTransaction
import com.septaalfauzan.saku.notification.duplicate.DuplicateDetector
import com.septaalfauzan.saku.notification.engine.NotificationParserEngine
import com.septaalfauzan.saku.notification.provider.ParserRegistry
import com.septaalfauzan.saku.notification.provider.bca.BcaNotificationParser
import com.septaalfauzan.saku.notification.provider.dana.DanaNotificationParser
import com.septaalfauzan.saku.notification.provider.gopay.GoPayNotificationParser
import com.septaalfauzan.saku.notification.provider.ovo.OvoNotificationParser
import com.septaalfauzan.saku.notification.provider.testapp.TestAppParser
import com.septaalfauzan.saku.notification.usecase.ProcessNotificationUseCase
import com.septaalfauzan.saku.ui.addedit.AddEditTransactionViewModel
import com.septaalfauzan.saku.ui.dashboard.DashboardViewModel
import com.septaalfauzan.saku.ui.detail.TransactionDetailViewModel
import com.septaalfauzan.saku.ui.review.ReviewQueueViewModel
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
    single<TransactionRepository> { RoomTransactionRepository(get(), get()) }
    single<NotificationSettingsRepository> { RoomNotificationSettingsRepository(get(), get(), get()) }

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
    singleOf(::ObserveTrackingEnabled)
    singleOf(::SetTrackingEnabled)
    singleOf(::ObserveAutoConfirm)
    singleOf(::SetAutoConfirm)

    single { BcaNotificationParser() }
    single { GoPayNotificationParser() }
    single { OvoNotificationParser() }
    single { DanaNotificationParser() }
    single { TestAppParser() }
    single {
        ParserRegistry(
            listOf(
                get<BcaNotificationParser>(),
                get<GoPayNotificationParser>(),
                get<OvoNotificationParser>(),
                get<DanaNotificationParser>(),
                //TODO: delete this after testing
                get<TestAppParser>(),
            ),
        )
    }
    single { NotificationParserEngine(get()) }
    single { DuplicateDetector(get()) }
    single { ProcessNotificationUseCase(get(), get(), get(), get()) }

    viewModelOf(::DashboardViewModel)
    viewModelOf(::TransactionListViewModel)
    viewModelOf(::TrackingViewModel)
    viewModelOf(::ReviewQueueViewModel)
    viewModel { params ->
        TransactionDetailViewModel(get(), get(), params.getOrNull() ?: "")
    }
    viewModel { params ->
        AddEditTransactionViewModel(get(), get(), get(), get(), params.getOrNull<String>())
    }
}
