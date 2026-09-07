package com.septaalfauzan.saku.di

import com.septaalfauzan.saku.data.dao.CategoryDao
import com.septaalfauzan.saku.data.dao.TransactionDao
import com.septaalfauzan.saku.data.database.AppDatabase
import com.septaalfauzan.saku.data.database.buildRoomDatabase
import com.septaalfauzan.saku.data.database.createDatabaseBuilder
import com.septaalfauzan.saku.data.repository.RoomTransactionRepository
import com.septaalfauzan.saku.domain.repository.TransactionRepository
import com.septaalfauzan.saku.domain.usecase.AddTransaction
import com.septaalfauzan.saku.domain.usecase.DeleteTransaction
import com.septaalfauzan.saku.domain.usecase.GetMonthlySummary
import com.septaalfauzan.saku.domain.usecase.ObserveCategories
import com.septaalfauzan.saku.domain.usecase.ObserveTransactions
import com.septaalfauzan.saku.domain.usecase.UpdateTransaction
import com.septaalfauzan.saku.ui.addedit.AddEditTransactionViewModel
import com.septaalfauzan.saku.ui.dashboard.DashboardViewModel
import com.septaalfauzan.saku.ui.detail.TransactionDetailViewModel
import com.septaalfauzan.saku.ui.transactions.TransactionListViewModel
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    single { buildRoomDatabase(createDatabaseBuilder()) }
    single { get<AppDatabase>().transactionDao() }
    single { get<AppDatabase>().categoryDao() }
    single<TransactionRepository> { RoomTransactionRepository(get(), get()) }

    singleOf(::ObserveTransactions)
    singleOf(::ObserveCategories)
    singleOf(::AddTransaction)
    singleOf(::UpdateTransaction)
    singleOf(::DeleteTransaction)
    singleOf(::GetMonthlySummary)

    viewModelOf(::DashboardViewModel)
    viewModelOf(::TransactionListViewModel)
    viewModel { params ->
        TransactionDetailViewModel(get(), get(), params.getOrNull() ?: "")
    }
    viewModel { params ->
        AddEditTransactionViewModel(get(), get(), get(), params.getOrNull())
    }
}
