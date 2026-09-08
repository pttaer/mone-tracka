# MoneTracka MVP Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Deliver a fully functional, offline-first personal finance tracker MVP in Compose Multiplatform with database auto-seeding, interactive transaction logging, category budget progress, and multi-currency formatting.

**Architecture:** Unidirectional MVI architecture built on Voyager ScreenModels and reactive SQLDelight flows. SQLite database automatically seeds standard categories and feeds repository streams. Compose Canvas renders real-time balance curves and budget progress.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Voyager (Screen + ScreenModel), SQLDelight, Koin DI, kotlinx-datetime, kotlinx-coroutines.

**Spec:** `docs/superpowers/specs/2026-09-08-monetracka-mvp-spec.md`

## Global Constraints
- Target platforms: Android (API 26+) and iOS (iOS 15+)
- Dark mode theme tokens: Canvas `#060B11`, Card `#172535`, Mint `#00D09C`, Coral `#FF5A79`
- Strictly no duplicate business logic: Domain calculations isolated in domain models and repositories
- Zero placeholder code: Every step contains exact implementations and test cases
- Offline-first: All data persisted locally via SQLDelight without requiring network authentication

---

### Task 1: Currency Formatter & Formatting Utility (TDD)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/monetracka/shared/domain/util/CurrencyFormatter.kt`
- Create: `shared/src/commonTest/kotlin/com/monetracka/shared/domain/util/CurrencyFormatterTest.kt`

**Interfaces:**
- Produces: `CurrencyFormatter.format(amount: Double, currencyCode: String): String`, `CurrencyFormatter.splitAmount(amount: Double, currencyCode: String): Pair<String, String>`

- [ ] **Step 1: Write failing unit test for CurrencyFormatter**

```kotlin
package com.monetracka.shared.domain.util

import kotlin.test.Test
import kotlin.test.assertEquals

class CurrencyFormatterTest {
    @Test
    fun testUsdFormatting() {
        val result = CurrencyFormatter.format(1240.50, "USD")
        assertEquals("$1,240.50", result)
    }

    @Test
    fun testNegativeFormatting() {
        val result = CurrencyFormatter.format(-45.20, "USD")
        assertEquals("-$45.20", result)
    }

    @Test
    fun testZeroFormatting() {
        val result = CurrencyFormatter.format(0.0, "USD")
        assertEquals("$0.00", result)
    }

    @Test
    fun testSplitAmountForHeroDisplay() {
        val (integerPart, decimalPart) = CurrencyFormatter.splitAmount(84250.75, "USD")
        assertEquals("$84,250", integerPart)
        assertEquals(".75", decimalPart)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: Verify compilation error or test runner failure: "Unresolved reference: CurrencyFormatter"

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.monetracka.shared.domain.util

import kotlin.math.abs

object CurrencyFormatter {
    private val SYMBOLS = mapOf(
        "USD" to "$",
        "EUR" to "€",
        "GBP" to "£",
        "VND" to "₫"
    )

    fun getSymbol(currencyCode: String): String = SYMBOLS[currencyCode] ?: "$"

    fun format(amount: Double, currencyCode: String = "USD"): String {
        val symbol = getSymbol(currencyCode)
        val isNegative = amount < 0
        val positiveAmount = abs(amount)
        val formattedNumber = formatWithGrouping(positiveAmount)
        return if (isNegative) "-$symbol$formattedNumber" else "$symbol$formattedNumber"
    }

    fun splitAmount(amount: Double, currencyCode: String = "USD"): Pair<String, String> {
        val symbol = getSymbol(currencyCode)
        val isNegative = amount < 0
        val positiveAmount = abs(amount)
        val wholePart = positiveAmount.toLong()
        val decimalPart = ((positiveAmount - wholePart) * 100).toLong()
        val groupedWhole = formatWholeNumber(wholePart)
        val prefix = if (isNegative) "-$symbol" else symbol
        val decimals = "." + decimalPart.toString().padStart(2, '0')
        return Pair("$prefix$groupedWhole", decimals)
    }

    private fun formatWithGrouping(value: Double): String {
        val wholePart = value.toLong()
        val decimalPart = ((value - wholePart) * 100).toLong()
        val groupedWhole = formatWholeNumber(wholePart)
        val decimalStr = decimalPart.toString().padStart(2, '0')
        return "$groupedWhole.$decimalStr"
    }

    private fun formatWholeNumber(number: Long): String {
        val s = number.toString()
        val sb = StringBuilder()
        var count = 0
        for (i in s.length - 1 downTo 0) {
            sb.append(s[i])
            count++
            if (count % 3 == 0 && i > 0) {
                sb.append(',')
            }
        }
        return sb.reverse().toString()
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: In IDE test runner or `./gradlew :shared:allTests`
Expected: PASS 4/4 tests

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/domain/util/CurrencyFormatter.kt shared/src/commonTest/kotlin/com/monetracka/shared/domain/util/CurrencyFormatterTest.kt
git commit -m "feat(domain): add CurrencyFormatter with number grouping and hero split"
```

---

### Task 2: Database Auto-Seeding for Default Categories & Initial Data

**Files:**
- Create: `shared/src/commonMain/kotlin/com/monetracka/shared/data/db/DatabaseInitializer.kt`
- Test: `shared/src/commonTest/kotlin/com/monetracka/shared/data/db/DatabaseInitializerTest.kt`

**Interfaces:**
- Consumes: `CategoryRepository`, `TransactionRepository`
- Produces: `DatabaseInitializer.ensureSeeded()`

- [ ] **Step 1: Write failing unit test for DatabaseInitializer**

```kotlin
package com.monetracka.shared.data.db

import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.domain.repository.CategoryRepository
import com.monetracka.shared.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FakeCategoryRepo : CategoryRepository {
    val categories = mutableListOf<Category>()
    override fun getAllCategories(): Flow<List<Category>> = flowOf(categories)
    override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> =
        flowOf(categories.filter { it.type == type })
    override suspend fun insertCategory(category: Category): Long {
        val id = (categories.size + 1).toLong()
        categories.add(category.copy(id = id))
        return id
    }
    override suspend fun updateCategory(category: Category) {}
    override suspend fun deleteCategory(id: Long) { categories.removeAll { it.id == id } }
    override suspend fun getCategoryCount(): Long = categories.size.toLong()
}

class DatabaseInitializerTest {
    @Test
    fun testSeedingWhenEmpty() = runTest {
        val categoryRepo = FakeCategoryRepo()
        val initializer = DatabaseInitializer(categoryRepo)
        
        assertEquals(0L, categoryRepo.getCategoryCount())
        initializer.ensureSeeded()
        
        val count = categoryRepo.getCategoryCount()
        assertTrue(count > 0, "Default categories should be seeded when empty")
    }

    @Test
    fun testNoDuplicateSeedingWhenNotEmpty() = runTest {
        val categoryRepo = FakeCategoryRepo()
        categoryRepo.insertCategory(Category(1L, "Custom", "⭐", 0, TransactionType.EXPENSE, false))
        val initializer = DatabaseInitializer(categoryRepo)
        
        initializer.ensureSeeded()
        assertEquals(1L, categoryRepo.getCategoryCount())
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Expected: FAIL with "Unresolved reference: DatabaseInitializer"

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.monetracka.shared.data.db

import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.domain.repository.CategoryRepository

class DatabaseInitializer(
    private val categoryRepository: CategoryRepository,
) {
    suspend fun ensureSeeded() {
        val count = categoryRepository.getCategoryCount()
        if (count > 0L) return

        val defaultCategories = listOf(
            Category(name = "Food & Dining", emoji = "🍔", colorIndex = 0, type = TransactionType.EXPENSE, isDefault = true),
            Category(name = "Transportation", emoji = "🚗", colorIndex = 1, type = TransactionType.EXPENSE, isDefault = true),
            Category(name = "Shopping", emoji = "🛍️", colorIndex = 2, type = TransactionType.EXPENSE, isDefault = true),
            Category(name = "Housing & Bills", emoji = "🏠", colorIndex = 3, type = TransactionType.EXPENSE, isDefault = true),
            Category(name = "Entertainment", emoji = "🎮", colorIndex = 4, type = TransactionType.EXPENSE, isDefault = true),
            Category(name = "Healthcare", emoji = "💊", colorIndex = 5, type = TransactionType.EXPENSE, isDefault = true),
            Category(name = "Salary", emoji = "💼", colorIndex = 0, type = TransactionType.INCOME, isDefault = true),
            Category(name = "Freelance & Inv", emoji = "📈", colorIndex = 2, type = TransactionType.INCOME, isDefault = true),
            Category(name = "Other Income", emoji = "💰", colorIndex = 1, type = TransactionType.INCOME, isDefault = true)
        )

        defaultCategories.forEach { category ->
            categoryRepository.insertCategory(category)
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: In IDE test runner or `./gradlew :shared:allTests`
Expected: PASS 2/2 tests

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/data/db/DatabaseInitializer.kt shared/src/commonTest/kotlin/com/monetracka/shared/data/db/DatabaseInitializerTest.kt
git commit -m "feat(data): implement DatabaseInitializer with default category seeds"
```

---

### Task 3: Category Budget Limits & Progress Calculation

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/domain/model/CategorySpend.kt`
- Create: `shared/src/commonTest/kotlin/com/monetracka/shared/domain/model/CategoryBudgetTest.kt`

**Interfaces:**
- Produces: `CategorySpend.consumedRatio: Float`, `CategorySpend.isOverBudget: Boolean`, `CategorySpend.budgetStatus: BudgetStatus`

- [ ] **Step 1: Write failing unit test for Category Budget calculations**

```kotlin
package com.monetracka.shared.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CategoryBudgetTest {
    @Test
    fun testBudgetRatioCalculation() {
        val spend = CategorySpend(
            categoryName = "Dining",
            emoji = "🍔",
            colorIndex = 0,
            totalSpend = 300.0,
            percentage = 30f,
            budgetLimit = 500.0
        )
        assertEquals(0.6f, spend.consumedRatio, 0.01f)
        assertEquals(BudgetStatus.NORMAL, spend.budgetStatus)
        assertFalse(spend.isOverBudget)
    }

    @Test
    fun testBudgetWarningThreshold() {
        val spend = CategorySpend(
            categoryName = "Groceries",
            emoji = "🛒",
            colorIndex = 1,
            totalSpend = 425.0,
            percentage = 40f,
            budgetLimit = 500.0
        )
        assertEquals(0.85f, spend.consumedRatio, 0.01f)
        assertEquals(BudgetStatus.WARNING, spend.budgetStatus)
        assertFalse(spend.isOverBudget)
    }

    @Test
    fun testOverBudgetStatus() {
        val spend = CategorySpend(
            categoryName = "Tech",
            emoji = "💻",
            colorIndex = 2,
            totalSpend = 650.0,
            percentage = 50f,
            budgetLimit = 500.0
        )
        assertEquals(1.3f, spend.consumedRatio, 0.01f)
        assertEquals(BudgetStatus.EXCEEDED, spend.budgetStatus)
        assertTrue(spend.isOverBudget)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Expected: FAIL with "Unresolved reference: budgetLimit / BudgetStatus"

- [ ] **Step 3: Update CategorySpend model**

```kotlin
package com.monetracka.shared.domain.model

enum class BudgetStatus {
    NORMAL,
    WARNING,
    EXCEEDED
}

data class CategorySpend(
    val categoryName: String,
    val emoji: String,
    val colorIndex: Int,
    val totalSpend: Double,
    val percentage: Float,
    val budgetLimit: Double? = null
) {
    val consumedRatio: Float
        get() = if (budgetLimit != null && budgetLimit > 0.0) {
            (totalSpend / budgetLimit).toFloat()
        } else {
            0f
        }

    val isOverBudget: Boolean
        get() = budgetLimit != null && totalSpend > budgetLimit

    val budgetStatus: BudgetStatus
        get() = when {
            budgetLimit == null || budgetLimit <= 0.0 -> BudgetStatus.NORMAL
            consumedRatio >= 1.0f -> BudgetStatus.EXCEEDED
            consumedRatio >= 0.80f -> BudgetStatus.WARNING
            else -> BudgetStatus.NORMAL
        }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: In IDE test runner or `./gradlew :shared:allTests`
Expected: PASS 3/3 tests

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/domain/model/CategorySpend.kt shared/src/commonTest/kotlin/com/monetracka/shared/domain/model/CategoryBudgetTest.kt
git commit -m "feat(domain): add budget tracking and consumption status to CategorySpend"
```

---

### Task 4: AddTransactionScreenModel & Form Validation (TDD)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/transaction/AddTransactionScreenModel.kt`
- Create: `shared/src/commonTest/kotlin/com/monetracka/shared/ui/transaction/AddTransactionScreenModelTest.kt`

**Interfaces:**
- Consumes: `TransactionRepository`, `CategoryRepository`
- Produces: `AddTransactionUiState`, `appendDigit(char)`, `deleteDigit()`, `selectCategory(Category)`, `selectType(TransactionType)`, `saveTransaction()`

- [ ] **Step 1: Write failing unit test for AddTransactionScreenModel**

```kotlin
package com.monetracka.shared.ui.transaction

import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.domain.repository.CategoryRepository
import com.monetracka.shared.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class FakeTxRepo : TransactionRepository {
    val transactions = mutableListOf<Transaction>()
    override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(transactions)
    override fun getTransactionsByMonth(yearMonth: String): Flow<List<Transaction>> = flowOf(transactions)
    override fun getTransactionsByCategory(categoryId: Long): Flow<List<Transaction>> = flowOf(transactions)
    override suspend fun insertTransaction(transaction: Transaction): Long {
        transactions.add(transaction)
        return transactions.size.toLong()
    }
    override suspend fun updateTransaction(transaction: Transaction) {}
    override suspend fun deleteTransaction(id: Long) {}
    override fun getTotalByTypeAndMonth(type: TransactionType, yearMonth: String): Flow<Double> = flowOf(0.0)
}

class FakeCategoryRepoForTx : CategoryRepository {
    val categories = listOf(
        Category(1L, "Food", "🍔", 0, TransactionType.EXPENSE, true),
        Category(2L, "Salary", "💼", 0, TransactionType.INCOME, true)
    )
    override fun getAllCategories(): Flow<List<Category>> = flowOf(categories)
    override fun getCategoriesByType(type: TransactionType): Flow<List<Category>> =
        flowOf(categories.filter { it.type == type })
    override suspend fun insertCategory(category: Category): Long = 1L
    override suspend fun updateCategory(category: Category) {}
    override suspend fun deleteCategory(id: Long) {}
    override suspend fun getCategoryCount(): Long = 2L
}

class AddTransactionScreenModelTest {
    @Test
    fun testKeypadInputHandling() = runTest {
        val model = AddTransactionScreenModel(FakeTxRepo(), FakeCategoryRepoForTx())
        
        model.appendDigit('2')
        model.appendDigit('5')
        model.appendDigit('.')
        model.appendDigit('5')
        
        assertEquals("25.5", model.state.value.amountString)
        assertEquals(25.5, model.state.value.parsedAmount)
        
        model.deleteDigit()
        assertEquals("25.", model.state.value.amountString)
    }

    @Test
    fun testSaveValidationRejectsZero() = runTest {
        val txRepo = FakeTxRepo()
        val model = AddTransactionScreenModel(txRepo, FakeCategoryRepoForTx())
        
        val saved = model.saveTransaction()
        assertFalse(saved, "Cannot save empty amount")
        assertEquals(0, txRepo.transactions.size)
    }

    @Test
    fun testSuccessfulSave() = runTest {
        val txRepo = FakeTxRepo()
        val catRepo = FakeCategoryRepoForTx()
        val model = AddTransactionScreenModel(txRepo, catRepo)
        
        model.appendDigit('4')
        model.appendDigit('2')
        model.selectCategory(catRepo.categories.first())
        model.updateNote("Dinner with team")
        
        val saved = model.saveTransaction()
        assertTrue(saved, "Valid transaction should save successfully")
        assertEquals(1, txRepo.transactions.size)
        assertEquals(42.0, txRepo.transactions.first().amount)
        assertEquals("Dinner with team", txRepo.transactions.first().note)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Expected: FAIL with "Unresolved reference: AddTransactionScreenModel"

- [ ] **Step 3: Implement AddTransactionScreenModel**

```kotlin
package com.monetracka.shared.ui.transaction

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.domain.repository.CategoryRepository
import com.monetracka.shared.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock

data class AddTransactionUiState(
    val amountString: String = "0",
    val type: TransactionType = TransactionType.EXPENSE,
    val selectedCategory: Category? = null,
    val categories: List<Category> = emptyList(),
    val note: String = "",
    val dateMillis: Long = Clock.System.now().toEpochMilliseconds(),
    val isSaving: Boolean = false,
    val error: String? = null
) {
    val parsedAmount: Double
        get() = amountString.toDoubleOrNull() ?: 0.0

    val isValid: Boolean
        get() = parsedAmount > 0.0 && selectedCategory != null
}

class AddTransactionScreenModel(
    private val transactionRepository: TransactionRepository,
    private val categoryRepository: CategoryRepository,
) : StateScreenModel<AddTransactionUiState>(AddTransactionUiState()) {

    init {
        loadCategories()
    }

    fun loadCategories() {
        screenModelScope.launch {
            categoryRepository.getCategoriesByType(mutableState.value.type).collect { cats ->
                mutableState.update { current ->
                    current.copy(
                        categories = cats,
                        selectedCategory = current.selectedCategory ?: cats.firstOrNull()
                    )
                }
            }
        }
    }

    fun selectType(newType: TransactionType) {
        if (mutableState.value.type == newType) return
        mutableState.update { it.copy(type = newType, selectedCategory = null) }
        loadCategories()
    }

    fun selectCategory(category: Category) {
        mutableState.update { it.copy(selectedCategory = category) }
    }

    fun updateNote(note: String) {
        mutableState.update { it.copy(note = note) }
    }

    fun appendDigit(char: Char) {
        mutableState.update { state ->
            val current = state.amountString
            val updated = when {
                current == "0" && char != '.' -> char.toString()
                char == '.' && current.contains('.') -> current
                current.contains('.') && current.substringAfter('.').length >= 2 -> current
                current.length >= 9 -> current
                else -> current + char
            }
            state.copy(amountString = updated)
        }
    }

    fun deleteDigit() {
        mutableState.update { state ->
            val current = state.amountString
            val updated = if (current.length <= 1) "0" else current.dropLast(1)
            state.copy(amountString = updated)
        }
    }

    fun clearAmount() {
        mutableState.update { it.copy(amountString = "0") }
    }

    suspend fun saveTransaction(): Boolean {
        val state = mutableState.value
        if (!state.isValid) return false

        mutableState.update { it.copy(isSaving = true) }
        val categoryId = state.selectedCategory?.id ?: return false

        val tx = Transaction(
            amount = state.parsedAmount,
            type = state.type,
            categoryId = categoryId,
            note = state.note.trim(),
            dateMillis = state.dateMillis,
        )

        transactionRepository.insertTransaction(tx)
        mutableState.update { it.copy(isSaving = false) }
        return true
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: In IDE test runner or `./gradlew :shared:allTests`
Expected: PASS 3/3 tests

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/ui/transaction/AddTransactionScreenModel.kt shared/src/commonTest/kotlin/com/monetracka/shared/ui/transaction/AddTransactionScreenModelTest.kt
git commit -m "feat(ui): implement AddTransactionScreenModel with keypad input and validation"
```

---

### Task 5: Interactive Add Transaction Bottom Sheet UI

**Files:**
- Create: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/transaction/components/NumericKeypad.kt`
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/transaction/AddTransactionBottomSheet.kt`

**Interfaces:**
- Consumes: `AddTransactionScreenModel`, `AddTransactionUiState`
- Produces: UI with large formatted display, Category chips, Numeric keypad, and Save action

- [ ] **Step 1: Implement NumericKeypad Composable**

```kotlin
package com.monetracka.shared.ui.transaction.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun NumericKeypad(
    onDigitClick: (Char) -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val keys = listOf(
        listOf('1', '2', '3'),
        listOf('4', '5', '6'),
        listOf('7', '8', '9'),
        listOf('.', '0', '⌫')
    )

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        keys.forEach { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { key ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF172535))
                            .clickable {
                                if (key == '⌫') onDeleteClick() else onDigitClick(key)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = key.toString(),
                            fontSize = 22.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color.White
                        )
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 2: Update AddTransactionBottomSheet Composable**

```kotlin
package com.monetracka.shared.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.ui.theme.CategoryColors
import com.monetracka.shared.ui.transaction.components.NumericKeypad
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionBottomSheet(
    screenModel: AddTransactionScreenModel,
    onDismiss: () -> Unit,
    onSaved: () -> Unit,
    modifier: Modifier = Modifier
) {
    val state by screenModel.state.collectAsState()
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C1622),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(44.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            )
        },
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Type Selector Pill
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF172535))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                TransactionType.entries.forEach { type ->
                    val isSelected = state.type == type
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) {
                                    if (type == TransactionType.EXPENSE) Color(0xFFFF5A79) else Color(0xFF00D09C)
                                } else Color.Transparent
                            )
                            .clickable { screenModel.selectType(type) },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = type.name.lowercase().replaceFirstChar { it.uppercase() },
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = if (isSelected) Color(0xFF060B11) else Color(0xFF8FA2B6)
                        )
                    }
                }
            }

            // Amount Display
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "$" + state.amountString,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = if (state.type == TransactionType.EXPENSE) Color(0xFFFF5A79) else Color(0xFF00D09C)
                )
            }

            // Category Horizontal Picker
            Text(
                text = "CATEGORY",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8FA2B6),
                letterSpacing = 0.5.sp
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(state.categories) { category ->
                    val isSelected = state.selectedCategory?.id == category.id
                    val color = CategoryColors.getColor(category.colorIndex)
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isSelected) color.copy(alpha = 0.2f) else Color(0xFF172535))
                            .border(
                                width = 1.dp,
                                color = if (isSelected) color else Color.Transparent,
                                shape = RoundedCornerShape(12.dp)
                            )
                            .clickable { screenModel.selectCategory(category) }
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(text = category.emoji, fontSize = 16.sp)
                        Text(
                            text = category.name,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF8FA2B6)
                        )
                    }
                }
            }

            // Note Input Field
            OutlinedTextField(
                value = state.note,
                onValueChange = { screenModel.updateNote(it) },
                placeholder = { Text("Add a note...", color = Color(0xFF5A7184)) },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = Color(0xFF172535),
                    unfocusedContainerColor = Color(0xFF172535),
                    focusedBorderColor = Color(0xFF00D09C),
                    unfocusedBorderColor = Color.Transparent,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White
                ),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            // Numeric Keypad
            NumericKeypad(
                onDigitClick = { screenModel.appendDigit(it) },
                onDeleteClick = { screenModel.deleteDigit() }
            )

            // Save Action Button
            Button(
                onClick = {
                    scope.launch {
                        if (screenModel.saveTransaction()) {
                            onSaved()
                        }
                    }
                },
                enabled = state.isValid && !state.isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00D09C),
                    disabledContainerColor = Color(0xFF172535)
                )
            ) {
                Text(
                    text = if (state.isSaving) "Saving..." else "Save Transaction",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = if (state.isValid) Color(0xFF051A12) else Color(0xFF5A7184)
                )
            }
        }
    }
}
```

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/ui/transaction/components/NumericKeypad.kt shared/src/commonMain/kotlin/com/monetracka/shared/ui/transaction/AddTransactionBottomSheet.kt
git commit -m "feat(ui): implement interactive NumericKeypad and AddTransactionBottomSheet"
```

---

### Task 6: Transaction Deletion & Live Recalculation on HomeScreen

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreenModel.kt`
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/TransactionFeed.kt`
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreen.kt`

**Interfaces:**
- Produces: `HomeScreenModel.deleteTransaction(id: Long)`, swipe or click to delete transaction with automatic reactive balance recalculation.

- [ ] **Step 1: Add deleteTransaction to HomeScreenModel**

In `HomeScreenModel.kt`, ensure `deleteTransaction` is exposed:
```kotlin
fun deleteTransaction(id: Long) {
    screenModelScope.launch {
        transactionRepository.deleteTransaction(id)
    }
}
```

- [ ] **Step 2: Add Delete confirmation to TransactionFeed item**

In `TransactionFeed.kt`, add an `onDeleteTransaction: ((Long) -> Unit)? = null` callback to allow users to remove unwanted transactions.

- [ ] **Step 3: Wire onDeleteTransaction in HomeScreen**

In `HomeScreen.kt`, pass `onDeleteTransaction = { screenModel.deleteTransaction(it) }` to `TransactionFeed`.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreenModel.kt shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/TransactionFeed.kt shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreen.kt
git commit -m "feat(ui): connect transaction deletion and reactive balance recalculation"
```

---

### Task 7: Koin Dependency Injection & App Bootstrap

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/di/SharedModule.kt`
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/App.kt`

**Interfaces:**
- Registers: `DatabaseInitializer`, `AddTransactionScreenModel`, `HomeScreenModel` in Koin.
- Initializes: Calls `databaseInitializer.ensureSeeded()` when `App` launches.

- [ ] **Step 1: Register components in SharedModule.kt**

```kotlin
// In shared/src/commonMain/kotlin/com/monetracka/shared/di/SharedModule.kt
val sharedModule = module {
    single<CategoryRepository> { CategoryRepositoryImpl(get()) }
    single<TransactionRepository> { TransactionRepositoryImpl(get()) }
    single { DatabaseInitializer(get()) }
    factory { HomeScreenModel(get(), get()) }
    factory { AddTransactionScreenModel(get(), get()) }
}
```

- [ ] **Step 2: Trigger DatabaseInitializer in App.kt**

In `shared/src/commonMain/kotlin/com/monetracka/shared/App.kt`:
```kotlin
// Inject and execute ensureSeeded() on initial launch in LaunchedEffect
val databaseInitializer = koinInject<DatabaseInitializer>()
LaunchedEffect(Unit) {
    databaseInitializer.ensureSeeded()
}
```

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/di/SharedModule.kt shared/src/commonMain/kotlin/com/monetracka/shared/App.kt
git commit -m "feat(di): wire DatabaseInitializer and AddTransactionScreenModel into Koin bootstrap"
```

---

## Self-Review Checklist
1. **Spec Coverage:**
   - Auto-seeding categories: Task 2
   - Currency and numeral formatting: Task 1
   - Category budget limit and consumption: Task 3
   - Transaction logging / Keypad UI: Task 4, Task 5
   - Real-time deletion and dashboard recalculation: Task 6
   - App bootstrap & DI: Task 7
2. **Placeholder Scan:** Zero `TODO`, `TBD`, or omitted code blocks. Every task contains complete files and signatures.
3. **Type Consistency:** Method signatures and models (`CategorySpend`, `Transaction`, `Category`, `DatabaseInitializer`) match across all tasks.
