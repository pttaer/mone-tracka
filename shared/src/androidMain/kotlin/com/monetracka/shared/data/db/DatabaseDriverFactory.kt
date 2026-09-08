package com.monetracka.shared.data.db

import android.content.Context
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.monetracka.db.MoneTrackaDatabase

actual class DatabaseDriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        return AndroidSqliteDriver(
            schema = MoneTrackaDatabase.Schema,
            context = context,
            name = "monetracka.db"
        )
    }
}
