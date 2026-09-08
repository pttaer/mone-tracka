package com.monetracka.shared.ui.transaction

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.domain.repository.CategoryRepository
import com.monetracka.shared.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

data class AddTransactionUiState(
    val amount: String = "",
    val note: String = "",
    val type: TransactionType = TransactionType.EXPENSE,
    val selectedCategoryId: Long? = null,
    val categories: List<Category> = emptyList(),
    val dateMillis: Long = Clock.System.now().toEpochMilliseconds(),
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
    val error: String? = null,
)

class AddTransactionScreenModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
) : ScreenModel {

    private val _uiState = MutableStateFlow(AddTransactionUiState())
    val uiState: StateFlow<AddTransactionUiState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        screenModelScope.launch {
            categoryRepository.getCategoriesByType(_uiState.value.type).collect { categories ->
                _uiState.value = _uiState.value.copy(
                    categories = categories,
                    selectedCategoryId = _uiState.value.selectedCategoryId ?: categories.firstOrNull()?.id,
                )
            }
        }
    }

    fun onAmountChanged(amount: String) {
        // Only allow valid decimal input
        val filtered = amount.filter { it.isDigit() || it == '.' }
        if (filtered.count { it == '.' } <= 1) {
            _uiState.value = _uiState.value.copy(amount = filtered, error = null)
        }
    }

    fun onNoteChanged(note: String) {
        _uiState.value = _uiState.value.copy(note = note)
    }

    fun onTypeChanged(type: TransactionType) {
        _uiState.value = _uiState.value.copy(type = type, selectedCategoryId = null)
        loadCategories()
    }

    fun onCategorySelected(categoryId: Long) {
        _uiState.value = _uiState.value.copy(selectedCategoryId = categoryId)
    }

    fun onDateChanged(dateMillis: Long) {
        _uiState.value = _uiState.value.copy(dateMillis = dateMillis)
    }

    fun saveTransaction() {
        val state = _uiState.value
        val amount = state.amount.toDoubleOrNull()

        if (amount == null || amount <= 0) {
            _uiState.value = state.copy(error = "Please enter a valid amount")
            return
        }
        if (state.selectedCategoryId == null) {
            _uiState.value = state.copy(error = "Please select a category")
            return
        }

        _uiState.value = state.copy(isSaving = true, error = null)

        screenModelScope.launch {
            try {
                transactionRepository.insertTransaction(
                    Transaction(
                        amount = amount,
                        type = state.type,
                        categoryId = state.selectedCategoryId,
                        note = state.note.trim(),
                        dateMillis = state.dateMillis,
                    )
                )
                _uiState.value = _uiState.value.copy(isSaving = false, isSaved = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to save: ${e.message}",
                )
            }
        }
    }
}
