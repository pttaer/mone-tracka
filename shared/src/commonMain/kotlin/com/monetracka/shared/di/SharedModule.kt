package com.monetracka.shared.di

import com.monetracka.db.MoneTrackaDatabase
import com.monetracka.shared.data.db.DatabaseDriverFactory
import com.monetracka.shared.data.repository.CategoryRepositoryImpl
import com.monetracka.shared.data.repository.TransactionRepositoryImpl
import com.monetracka.shared.domain.repository.CategoryRepository
import com.monetracka.shared.domain.repository.TransactionRepository
import com.monetracka.shared.ui.home.HomeScreenModel
import com.monetracka.shared.ui.transaction.TransactionListScreenModel
import org.koin.core.module.Module
import org.koin.dsl.module

fun sharedModule(): Module = module {
    // Database
    single {
        val driver = get<DatabaseDriverFactory>().createDriver()
        MoneTrackaDatabase(driver)
    }

    // Repositories
    single<TransactionRepository> { TransactionRepositoryImpl(get()) }
    single<CategoryRepository> { CategoryRepositoryImpl(get()) }
    single<com.monetracka.shared.domain.repository.AccountRepository> { com.monetracka.shared.data.repository.AccountRepositoryImpl(get()) }
    single<com.monetracka.shared.domain.repository.UserProfileRepository> { com.monetracka.shared.data.repository.UserProfileRepositoryImpl(get()) }
    single<com.monetracka.shared.domain.quote.QuoteRepository> { com.monetracka.shared.domain.quote.QuoteRepositoryImpl() }

    // Coach Engine
    single { com.monetracka.shared.domain.coach.SmartSavingCoachEngine() }

    // Screen Models
    factory { HomeScreenModel(get(), get(), get(), get(), get(), get()) }
    factory { TransactionListScreenModel(get(), get()) }
    factory { com.monetracka.shared.ui.onboarding.OnboardingScreenModel(get(), get(), get()) }
}

