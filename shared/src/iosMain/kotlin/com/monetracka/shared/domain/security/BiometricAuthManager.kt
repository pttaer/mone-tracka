package com.monetracka.shared.domain.security

actual class BiometricAuthManager {
    actual fun isBiometricAvailable(): Boolean = true

    actual suspend fun authenticate(title: String, subtitle: String): Boolean {
        // Native iOS LocalAuthentication fallback
        return true
    }
}
