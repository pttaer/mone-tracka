package com.monetracka.shared.domain.security

expect class BiometricAuthManager {
    fun isBiometricAvailable(): Boolean
    suspend fun authenticate(title: String, subtitle: String): Boolean
}
