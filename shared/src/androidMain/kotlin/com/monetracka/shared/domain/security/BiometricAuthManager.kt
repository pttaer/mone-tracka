package com.monetracka.shared.domain.security

import android.content.Context

actual class BiometricAuthManager(private val context: Context) {
    actual fun isBiometricAvailable(): Boolean = true

    actual suspend fun authenticate(title: String, subtitle: String): Boolean {
        // Fallback / standard biometric verification stub for unit tests and direct execution
        return true
    }
}
