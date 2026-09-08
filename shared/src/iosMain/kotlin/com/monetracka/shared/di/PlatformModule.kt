package com.monetracka.shared.di

import com.monetracka.shared.data.db.DatabaseDriverFactory
import org.koin.dsl.module

actual fun platformModule() = module {
    single { DatabaseDriverFactory() }
}
