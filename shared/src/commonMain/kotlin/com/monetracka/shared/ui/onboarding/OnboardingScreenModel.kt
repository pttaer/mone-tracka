package com.monetracka.shared.ui.onboarding

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.monetracka.shared.domain.model.Account
import com.monetracka.shared.domain.model.UserProfile
import com.monetracka.shared.domain.repository.AccountRepository
import com.monetracka.shared.domain.repository.CategoryRepository
import com.monetracka.shared.domain.repository.UserProfileRepository
import kotlinx.coroutines.launch

data class OnboardingUiState(
    val step: Int = 0,
    val userName: String = "",
    val selectedCurrency: String = "USD",
    val checkingBalance: String = "0",
    val cashBalance: String = "0",
    val isLoading: Boolean = false
) {
    val userInitials: String
        get() = userName.trim().split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercase() }
            .joinToString("")
            .ifEmpty { "U" }
}

class OnboardingScreenModel(
    private val userProfileRepository: UserProfileRepository,
    private val accountRepository: AccountRepository,
    private val categoryRepository: CategoryRepository
) : StateScreenModel<OnboardingUiState>(OnboardingUiState()) {

    fun onNameChanged(name: String) {
        mutableState.value = mutableState.value.copy(userName = name)
    }

    fun onCurrencySelected(currency: String) {
        mutableState.value = mutableState.value.copy(selectedCurrency = currency)
    }

    fun onCheckingBalanceChanged(amount: String) {
        val filtered = amount.filterIndexed { i, c -> c.isDigit() || (c == '.' && '.' !in amount.substring(0, i)) }
        mutableState.value = mutableState.value.copy(checkingBalance = filtered)
    }

    fun onCashBalanceChanged(amount: String) {
        val filtered = amount.filterIndexed { i, c -> c.isDigit() || (c == '.' && '.' !in amount.substring(0, i)) }
        mutableState.value = mutableState.value.copy(cashBalance = filtered)
    }

    fun nextStep() {
        if (mutableState.value.step < 3) {
            mutableState.value = mutableState.value.copy(step = mutableState.value.step + 1)
        }
    }

    fun prevStep() {
        if (mutableState.value.step > 0) {
            mutableState.value = mutableState.value.copy(step = mutableState.value.step - 1)
        }
    }

    fun completeOnboarding(onSuccess: () -> Unit) {
        if (mutableState.value.isLoading) return
        mutableState.value = mutableState.value.copy(isLoading = true)
        screenModelScope.launch {
            val checking = mutableState.value.checkingBalance.toDoubleOrNull() ?: 0.0
            val cash = mutableState.value.cashBalance.toDoubleOrNull() ?: 0.0
            val finalName = mutableState.value.userName.trim().ifBlank { "User" }
            val currency = mutableState.value.selectedCurrency

            categoryRepository.insertDefaultCategories()
            accountRepository.insertAccount(
                Account(name = "Main Checking", emoji = "🏦", initialBalance = checking, description = "Daily operational account")
            )
            accountRepository.insertAccount(
                Account(name = "Cash Wallet", emoji = "💵", initialBalance = cash, description = "Physical cash on hand")
            )
            userProfileRepository.saveUserProfile(
                UserProfile(
                    userName = finalName,
                    currency = currency,
                    hasCompletedOnboarding = true
                )
            )

            mutableState.value = mutableState.value.copy(isLoading = false)
            onSuccess()
        }
    }
}
