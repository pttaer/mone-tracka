package com.monetracka.shared.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.monetracka.db.MoneTrackaDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        return NativeSqliteDriver(
            schema = MoneTrackaDatabase.Schema,
            name = "monetracka.db"
        )
    }
}
