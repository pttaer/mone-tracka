# Accounts & Tactile Drag-and-Drop Transfers Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement dynamic user-defined accounts, starting balances, internal net-zero transfers, and tactile drag-and-drop transfer interactions in MoneTracka.

**Architecture:** Extend SQLDelight database with `AccountEntity` and updated `TransactionEntity` supporting `TRANSFER` type and account foreign keys. Calculate per-account and total balances in `HomeScreenModel` via reactive in-memory collection aggregations. Build Compose Multiplatform tactile UI components: an accounts horizontal strip with gesture drag-target tracking, a spring `TransferBottomSheet` with delta balance projections and a "Slide to Transfer" confirmation slider, and an `AddAccountBottomSheet`.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, SQLDelight, Voyager, Koin, Coroutines Flow, JUnit / Kotlin Test.

**Spec:** [docs/superpowers/specs/2026-09-08-accounts-and-drag-transfer-design.md](file:///f:/MoneTracka/docs/superpowers/specs/2026-09-08-accounts-and-drag-transfer-design.md)

## Global Constraints

- Android SDK path is `C:\Users\thanh\AppData\Local\Android\Sdk`. Android build commands must prefix `$env:ANDROID_HOME="C:\Users\thanh\AppData\Local\Android\Sdk"`.
- Never run `dotnet build`.
- Use TDD: red test -> verify fail -> minimal green implementation -> verify pass -> commit.
- Ponytail protocol: No unnecessary interfaces or premature abstractions. Keep diffs surgical and minimal.

---

### Task 1: Database Schema & Domain Models Update

**Files:**
- Modify: `shared/src/commonMain/sqldelight/com/monetracka/db/MoneTrackaDatabase.sq`
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/domain/model/Models.kt`
- Test: `shared/src/commonTest/kotlin/com/monetracka/shared/domain/model/CategorySpendTest.kt`

**Interfaces:**
- Produces:
  - `AccountEntity` table & queries (`getAllAccounts`, `insertAccount`, `deleteAccount`, `getAccountCount`, `lastInsertedAccountId`)
  - `TransactionEntity` with columns `accountId` (Long) and `toAccountId` (Long?)
  - `data class Account(val id: Long = 0, val name: String, val emoji: String, val initialBalance: Double = 0.0, val description: String = "")`
  - `enum class TransactionType { INCOME, EXPENSE, TRANSFER }`
  - `data class Transaction(..., val accountId: Long = 1L, val toAccountId: Long? = null)`

- [ ] **Step 1: Update Models.kt with Account and TRANSFER type**

Edit `shared/src/commonMain/kotlin/com/monetracka/shared/domain/model/Models.kt`:
```kotlin
package com.monetracka.shared.domain.model

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER
}

data class Account(
    val id: Long = 0,
    val name: String,
    val emoji: String,
    val initialBalance: Double = 0.0,
    val description: String = ""
)

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long,
    val accountId: Long = 1L,
    val toAccountId: Long? = null,
    val note: String = "",
    val dateMillis: Long,
    val createdAtMillis: Long = 0,
)

data class Category(
    val id: Long = 0,
    val name: String,
    val emoji: String,
    val colorIndex: Int = 0,
    val type: TransactionType,
    val isDefault: Boolean = true,
)
```

- [ ] **Step 2: Update SQLDelight schema with AccountEntity and TransactionEntity fields**

Edit `shared/src/commonMain/sqldelight/com/monetracka/db/MoneTrackaDatabase.sq`:
```sql
CREATE TABLE IF NOT EXISTS CategoryEntity (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    emoji TEXT NOT NULL,
    colorIndex INTEGER NOT NULL DEFAULT 0,
    type TEXT NOT NULL,
    isDefault INTEGER NOT NULL DEFAULT 1
);

CREATE TABLE IF NOT EXISTS AccountEntity (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    emoji TEXT NOT NULL,
    initialBalance REAL NOT NULL DEFAULT 0.0,
    description TEXT NOT NULL DEFAULT ''
);

CREATE TABLE IF NOT EXISTS TransactionEntity (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    amount REAL NOT NULL,
    type TEXT NOT NULL,
    categoryId INTEGER NOT NULL,
    accountId INTEGER NOT NULL DEFAULT 1,
    toAccountId INTEGER,
    note TEXT NOT NULL DEFAULT '',
    dateMillis INTEGER NOT NULL,
    createdAtMillis INTEGER NOT NULL,
    FOREIGN KEY (categoryId) REFERENCES CategoryEntity(id) ON DELETE CASCADE,
    FOREIGN KEY (accountId) REFERENCES AccountEntity(id) ON DELETE CASCADE
);

CREATE INDEX idx_transaction_date ON TransactionEntity(dateMillis);
CREATE INDEX idx_transaction_category ON TransactionEntity(categoryId);
CREATE INDEX idx_transaction_account ON TransactionEntity(accountId);
CREATE INDEX idx_transaction_type ON TransactionEntity(type);

-- Category Queries
getAllCategories:
SELECT * FROM CategoryEntity ORDER BY name ASC;

insertCategory:
INSERT INTO CategoryEntity(name, emoji, colorIndex, type, isDefault)
VALUES (?, ?, ?, ?, ?);

lastInsertedCategoryId:
SELECT last_insert_rowid();

getCategoryCount:
SELECT COUNT(*) FROM CategoryEntity;

-- Account Queries
getAllAccounts:
SELECT * FROM AccountEntity ORDER BY id ASC;

insertAccount:
INSERT INTO AccountEntity(name, emoji, initialBalance, description)
VALUES (?, ?, ?, ?);

deleteAccount:
DELETE FROM AccountEntity WHERE id = ?;

getAccountCount:
SELECT COUNT(*) FROM AccountEntity;

lastInsertedAccountId:
SELECT last_insert_rowid();

-- Transaction Queries
getAllTransactions:
SELECT * FROM TransactionEntity ORDER BY dateMillis DESC;

insertTransaction:
INSERT INTO TransactionEntity(amount, type, categoryId, accountId, toAccountId, note, dateMillis, createdAtMillis)
VALUES (?, ?, ?, ?, ?, ?, ?, ?);

deleteTransaction:
DELETE FROM TransactionEntity WHERE id = ?;

lastInsertedTransactionId:
SELECT last_insert_rowid();
```

- [ ] **Step 3: Run SQLDelight code generation and check build**

Run: `$env:ANDROID_HOME="C:\Users\thanh\AppData\Local\Android\Sdk"; ./gradlew :shared:generateCommonMainMoneTrackaDatabaseInterface`
Expected: BUILD SUCCESSFUL with generated `AccountEntity` and updated `TransactionEntity`.

- [ ] **Step 4: Commit schema changes**

```bash
git add shared/src/commonMain/sqldelight/com/monetracka/db/MoneTrackaDatabase.sq shared/src/commonMain/kotlin/com/monetracka/shared/domain/model/Models.kt
git commit -m "feat(db): add AccountEntity and update TransactionEntity with transfer accounts"
```

---

### Task 2: Repository Layer Implementation

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/domain/repository/Repositories.kt`
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/data/repository/RepositoryImpl.kt`
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/di/SharedModule.kt`
- Test: `shared/src/commonTest/kotlin/com/monetracka/shared/data/repository/RepositoryTest.kt` (or verify via unit tests)

**Interfaces:**
- Consumes: `AccountEntity`, `TransactionEntity`, `Account`, `Transaction`, `TransactionType`
- Produces:
  - `interface AccountRepository { fun getAllAccounts(): Flow<List<Account>>; suspend fun insertAccount(account: Account): Long; suspend fun deleteAccount(id: Long); suspend fun insertDefaultAccounts() }`
  - `class AccountRepositoryImpl(private val database: MoneTrackaDatabase) : AccountRepository`
  - Updated `TransactionRepositoryImpl` mapping `accountId` and `toAccountId`

- [ ] **Step 1: Define AccountRepository interface in Repositories.kt**

Add `AccountRepository` in `shared/src/commonMain/kotlin/com/monetracka/shared/domain/repository/Repositories.kt`:
```kotlin
package com.monetracka.shared.domain.repository

import com.monetracka.shared.domain.model.Account
import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.Transaction
import kotlinx.coroutines.flow.Flow

interface TransactionRepository {
    fun getAllTransactions(): Flow<List<Transaction>>
    suspend fun insertTransaction(transaction: Transaction): Long
    suspend fun deleteTransaction(id: Long)
}

interface CategoryRepository {
    fun getAllCategories(): Flow<List<Category>>
    suspend fun insertCategory(category: Category): Long
    suspend fun insertDefaultCategories()
}

interface AccountRepository {
    fun getAllAccounts(): Flow<List<Account>>
    suspend fun insertAccount(account: Account): Long
    suspend fun deleteAccount(id: Long)
    suspend fun insertDefaultAccounts()
}
```

- [ ] **Step 2: Implement AccountRepositoryImpl and update TransactionRepositoryImpl**

Update `shared/src/commonMain/kotlin/com/monetracka/shared/data/repository/RepositoryImpl.kt`:
- In `TransactionRepositoryImpl.insertTransaction`: pass `accountId = transaction.accountId` and `toAccountId = transaction.toAccountId`.
- In `TransactionEntity.toDomain()`: map `accountId = accountId` and `toAccountId = toAccountId`.
- Implement `AccountRepositoryImpl`:
  - `getAllAccounts()` flows from `queries.getAllAccounts()` mapped to `Account`.
  - `insertAccount()` queries `insertAccount(name, emoji, initialBalance, description)`.
  - `deleteAccount()` queries `deleteAccount(id)`.
  - `insertDefaultAccounts()`: checks `getAccountCount()`. If 0, inserts `Account(name = "Main Checking", emoji = "🏦", initialBalance = 0.0)` and `Account(name = "Cash Wallet", emoji = "💵", initialBalance = 0.0)`.
- In `CategoryRepositoryImpl.insertDefaultCategories()`: add default category `Category(name = "Transfer", emoji = "🔁", colorIndex = 0, type = TransactionType.TRANSFER)`.

- [ ] **Step 3: Register AccountRepository in SharedModule.kt**

Add to `shared/src/commonMain/kotlin/com/monetracka/shared/di/SharedModule.kt`:
```kotlin
single<AccountRepository> { AccountRepositoryImpl(get()) }
```
And pass `AccountRepository` into `HomeScreenModel` constructor.

- [ ] **Step 4: Run unit tests to verify repository compile**

Run: `$env:ANDROID_HOME="C:\Users\thanh\AppData\Local\Android\Sdk"; ./gradlew :shared:testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit repository layer changes**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/domain/repository/Repositories.kt shared/src/commonMain/kotlin/com/monetracka/shared/data/repository/RepositoryImpl.kt shared/src/commonMain/kotlin/com/monetracka/shared/di/SharedModule.kt
git commit -m "feat(repo): add AccountRepository and support account fields in transactions"
```

---

### Task 3: Account Balance & Transfer Calculations Unit Tests (TDD)

**Files:**
- Create: `shared/src/commonTest/kotlin/com/monetracka/shared/domain/account/AccountBalanceCalculationTest.kt`
- Modify: `shared/src/commonTest/kotlin/com/monetracka/shared/ui/home/HomeScreenModelTest.kt`
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreenModel.kt`
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeUiState.kt`

**Interfaces:**
- Consumes: `Account`, `Transaction`, `TransactionType`
- Produces:
  - `HomeUiState.accounts: List<AccountUiModel>` where `data class AccountUiModel(val account: Account, val balance: Double)`
  - Correct net portfolio computation: `totalBalance = accounts.sumOf { it.balance }`
  - Inflows & Outflows logic for internal transfers: source account decreases by amount, destination account increases by amount, total wealth unchanged.

- [ ] **Step 1: Write failing unit test for Account Balance and Transfer Math**

Create `shared/src/commonTest/kotlin/com/monetracka/shared/domain/account/AccountBalanceCalculationTest.kt`:
```kotlin
package com.monetracka.shared.domain.account

import com.monetracka.shared.domain.model.Account
import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.domain.repository.AccountRepository
import com.monetracka.shared.domain.repository.CategoryRepository
import com.monetracka.shared.domain.repository.TransactionRepository
import com.monetracka.shared.ui.home.HomeScreenModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeAccountRepository(private val accounts: List<Account>) : AccountRepository {
    override fun getAllAccounts(): Flow<List<Account>> = flowOf(accounts)
    override suspend fun insertAccount(account: Account): Long = 1L
    override suspend fun deleteAccount(id: Long) {}
    override suspend fun insertDefaultAccounts() {}
}

class AccountBalanceCalculationTest {
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() = Dispatchers.setMain(testDispatcher)

    @AfterTest
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun testAccountBalancesAndNetZeroTransfer() = runTest(testDispatcher) {
        val bankAccount = Account(id = 1L, name = "Bank", emoji = "🏦", initialBalance = 1000.0)
        val cashAccount = Account(id = 2L, name = "Cash", emoji = "💵", initialBalance = 200.0)
        val accounts = listOf(bankAccount, cashAccount)

        val txs = listOf(
            // Income 300 to Bank
            Transaction(id = 1L, amount = 300.0, type = TransactionType.INCOME, categoryId = 1L, accountId = 1L, dateMillis = 1000L),
            // Expense 50 from Cash
            Transaction(id = 2L, amount = 50.0, type = TransactionType.EXPENSE, categoryId = 2L, accountId = 2L, dateMillis = 2000L),
            // Transfer 150 from Bank to Cash
            Transaction(id = 3L, amount = 150.0, type = TransactionType.TRANSFER, categoryId = 3L, accountId = 1L, toAccountId = 2L, dateMillis = 3000L)
        )

        val fakeTxRepo = object : TransactionRepository {
            override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(txs)
            override suspend fun insertTransaction(transaction: Transaction): Long = 1L
            override suspend fun deleteTransaction(id: Long) {}
        }
        val fakeCatRepo = object : CategoryRepository {
            override fun getAllCategories(): Flow<List<Category>> = flowOf(emptyList())
            override suspend fun insertCategory(category: Category): Long = 1L
            override suspend fun insertDefaultCategories() {}
        }

        val viewModel = HomeScreenModel(
            transactionRepository = fakeTxRepo,
            categoryRepository = fakeCatRepo,
            accountRepository = FakeAccountRepository(accounts)
        )

        testScheduler.advanceUntilIdle()

        val state = viewModel.state.value
        // Bank: 1000 (initial) + 300 (income) - 150 (transfer out) = 1150
        val bankUi = state.accounts.first { it.account.id == 1L }
        assertEquals(1150.0, bankUi.balance, 0.01)

        // Cash: 200 (initial) - 50 (expense) + 150 (transfer in) = 300
        val cashUi = state.accounts.first { it.account.id == 2L }
        assertEquals(300.0, cashUi.balance, 0.01)

        // Total Net Portfolio: 1150 + 300 = 1450 (which equals initial 1200 + 300 income - 50 expense)
        assertEquals(1450.0, state.totalBalance, 0.01)
    }
}
```

- [ ] **Step 2: Run test to verify it fails (Red)**

Run: `$env:ANDROID_HOME="C:\Users\thanh\AppData\Local\Android\Sdk"; ./gradlew :shared:testDebugUnitTest --tests "com.monetracka.shared.domain.account.AccountBalanceCalculationTest"`
Expected: FAIL (compilation errors: `accountRepository` missing in `HomeScreenModel`, `accounts` missing in `HomeUiState`).

- [ ] **Step 3: Update HomeUiState and HomeScreenModel (Green)**

1. Update `HomeUiState.kt`:
```kotlin
data class AccountUiModel(
    val account: Account,
    val balance: Double
)

@Immutable
data class HomeUiState(
    val totalBalance: Double = 0.0,
    val monthlyTrendPercent: Double = 0.0,
    val monthlyTrendAmount: Double = 0.0,
    val sparklinePoints: List<Float> = emptyList(),
    val categorySpends: List<CategorySpend> = emptyList(),
    val recentTransactions: List<Transaction> = emptyList(),
    val categories: Map<Long, Category> = emptyMap(),
    val accounts: List<AccountUiModel> = emptyList(),
    val coachInsight: com.monetracka.shared.domain.coach.CoachInsight? = null,
    val financialQuote: com.monetracka.shared.domain.quote.FinancialQuote? = null,
    val isAddSheetOpen: Boolean = false,
    val addSheetInitialType: TransactionType = TransactionType.EXPENSE,
    val isTransferSheetOpen: Boolean = false,
    val transferSourceAccount: Account? = null,
    val transferTargetAccount: Account? = null,
    val isAddAccountSheetOpen: Boolean = false,
    val isLoading: Boolean = false
)
```
Add Intents in `HomeIntent`:
```kotlin
sealed interface HomeIntent {
    data class OpenAddTransaction(val initialType: TransactionType = TransactionType.EXPENSE) : HomeIntent
    data object DismissAddTransaction : HomeIntent
    data object RefreshQuote : HomeIntent
    data class CreateTransaction(
        val amount: Double,
        val type: TransactionType,
        val categoryId: Long,
        val note: String,
        val accountId: Long = 1L
    ) : HomeIntent
    data class DeleteTransaction(val id: Long) : HomeIntent
    // Account & Transfer Intents
    data class InitiateTransfer(val fromAccount: Account, val toAccount: Account) : HomeIntent
    data object DismissTransfer : HomeIntent
    data class ExecuteTransfer(val fromAccountId: Long, val toAccountId: Long, val amount: Double, val note: String = "") : HomeIntent
    data object OpenAddAccount : HomeIntent
    data object DismissAddAccount : HomeIntent
    data class CreateAccount(val name: String, val emoji: String, val initialBalance: Double, val description: String = "") : HomeIntent
    data class DeleteAccount(val id: Long) : HomeIntent
}
```

2. Update `HomeScreenModel.kt`:
- Combine `transactionRepository.getAllTransactions()`, `categoryRepository.getAllCategories()`, and `accountRepository.getAllAccounts()`.
- Calculate per-account balances:
  ```kotlin
  val accountUiList = accounts.map { acc ->
      val inflows = transactions.filter { it.type == TransactionType.INCOME && it.accountId == acc.id }.sumOf { it.amount } +
                    transactions.filter { it.type == TransactionType.TRANSFER && it.toAccountId == acc.id }.sumOf { it.amount }
      val outflows = transactions.filter { it.type == TransactionType.EXPENSE && it.accountId == acc.id }.sumOf { it.amount } +
                     transactions.filter { it.type == TransactionType.TRANSFER && it.accountId == acc.id }.sumOf { it.amount }
      AccountUiModel(account = acc, balance = acc.initialBalance + inflows - outflows)
  }
  val netBalance = accountUiList.sumOf { it.balance }
  ```
- Exclude `TransactionType.TRANSFER` from `expensesByCategory` and `monthlyTrendPercent`.
- Handle `ExecuteTransfer`, `CreateAccount`, `DeleteAccount`, `InitiateTransfer` intents.

- [ ] **Step 4: Run tests to verify they pass (Green)**

Run: `$env:ANDROID_HOME="C:\Users\thanh\AppData\Local\Android\Sdk"; ./gradlew :shared:testDebugUnitTest`
Expected: PASS with all tests green.

- [ ] **Step 5: Commit calculation logic**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreenModel.kt shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeUiState.kt shared/src/commonTest/
git commit -m "test(accounts): verify account balances and net-zero transfer aggregation"
```

---

### Task 4: Tactile Accounts Horizontal Strip with Drag & Drop

**Files:**
- Create: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/AccountStrip.kt`
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreen.kt`

**Interfaces:**
- Consumes: `List<AccountUiModel>`, `onInitiateTransfer: (Account, Account) -> Unit`, `onAddAccount: () -> Unit`
- Produces: Compose component `AccountStrip` with drag gesture detection, target highlight, and transfer trigger.

- [ ] **Step 1: Implement AccountStrip Composable**

Create `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/AccountStrip.kt`:
- Displays horizontal `LazyRow` of account cards plus an "+ Add" button.
- Card design:
  - Dark container `Color(0xFF131F2E)` with 16.dp rounded corners and subtle border `Color.White.copy(alpha = 0.08f)`.
  - Emoji badge, account name, and formatted balance using `CurrencyFormatter.format(item.balance)`.
- Drag & Drop interaction:
  - Long press gesture detection on each card.
  - Track card layout positions using `onGloballyPositioned`.
  - While dragging: card renders with elevated shadow, slight rotation tilt (-3f), and cyan glow.
  - When drag position overlaps another card's bounds, target card displays highlighted cyan border (`Color(0xFF00D09C)`).
  - When released over a target card (where `fromAccount.id != toAccount.id`): invokes `onInitiateTransfer(fromAccount, toAccount)`.

- [ ] **Step 2: Wire AccountStrip into HomeScreen.kt OverviewView**

In `HomeScreen.kt`:
- Add `AccountStrip` immediately after `BalanceHeroCard` inside `LazyColumn`.
- Pass `state.accounts`, `onInitiateTransfer = { from, to -> screenModel.onIntent(HomeIntent.InitiateTransfer(from, to)) }`, and `onAddAccount = { screenModel.onIntent(HomeIntent.OpenAddAccount) }`.

- [ ] **Step 3: Run unit tests and compilation check**

Run: `$env:ANDROID_HOME="C:\Users\thanh\AppData\Local\Android\Sdk"; ./gradlew :shared:testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit AccountStrip component**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/AccountStrip.kt shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreen.kt
git commit -m "feat(ui): implement tactile drag-and-drop AccountStrip on HomeScreen"
```

---

### Task 5: Spring Transfer Bottom Sheet with "Slide to Transfer" Slider

**Files:**
- Create: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/TransferBottomSheet.kt`
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreen.kt`

**Interfaces:**
- Consumes: `sourceAccount: Account`, `targetAccount: Account`, `sourceBalance: Double`, `targetBalance: Double`, `onDismiss: () -> Unit`, `onConfirmTransfer: (Double, String) -> Unit`
- Produces: Compose `TransferBottomSheet` with live balance projections, quick preset chips ($20, $50, $100, All), and tactile slider `SlideToTransferSlider`.

- [ ] **Step 1: Implement TransferBottomSheet with Slide-to-Transfer**

Create `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/TransferBottomSheet.kt`:
- **Header**: Shows `FROM [emoji] Name` ➔ `TO [emoji] Name` with dark pill badges.
- **Amount Section**:
  - Formatted amount display with clean numeric keyboard/input or preset incrementers.
  - Quick preset chips: `+$20`, `+$50`, `+$100`, `All` (which fills `sourceBalance`).
- **Live Projected Balances**:
  - Shows dynamic preview:
    - From: `$sourceBalance` ➔ `$newSourceBalance` (`-$amount`)
    - To: `$targetBalance` ➔ `$newTargetBalance` (`+$amount`)
- **Slide to Transfer Slider**:
  - Horizontal pill track with track text "Slide to Transfer ➔".
  - Draggable circular thumb with arrow `➔`.
  - Spring snap-back animation if released before reaching threshold (85%).
  - If dragged past 85%: snaps to 100%, triggers `onConfirmTransfer(amount, note)`, and dismisses sheet.

- [ ] **Step 2: Wire TransferBottomSheet in HomeScreen.kt**

In `HomeScreen.kt`:
```kotlin
if (state.isTransferSheetOpen && state.transferSourceAccount != null && state.transferTargetAccount != null) {
    val sourceBalance = state.accounts.firstOrNull { it.account.id == state.transferSourceAccount?.id }?.balance ?: 0.0
    val targetBalance = state.accounts.firstOrNull { it.account.id == state.transferTargetAccount?.id }?.balance ?: 0.0
    TransferBottomSheet(
        sourceAccount = state.transferSourceAccount!!,
        targetAccount = state.transferTargetAccount!!,
        sourceBalance = sourceBalance,
        targetBalance = targetBalance,
        onDismiss = { screenModel.onIntent(HomeIntent.DismissTransfer) },
        onConfirmTransfer = { amount, note ->
            screenModel.onIntent(
                HomeIntent.ExecuteTransfer(
                    fromAccountId = state.transferSourceAccount!!.id,
                    toAccountId = state.transferTargetAccount!!.id,
                    amount = amount,
                    note = note
                )
            )
        }
    )
}
```

- [ ] **Step 3: Run unit tests and compilation check**

Run: `$env:ANDROID_HOME="C:\Users\thanh\AppData\Local\Android\Sdk"; ./gradlew :shared:testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit TransferBottomSheet**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/TransferBottomSheet.kt shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreen.kt
git commit -m "feat(ui): implement TransferBottomSheet with live balance projection and slide to confirm"
```

---

### Task 6: Add Account Bottom Sheet & Transaction Feed Transfer Rendering

**Files:**
- Create: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/AddAccountBottomSheet.kt`
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/TransactionFeed.kt`
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreen.kt`

**Interfaces:**
- Consumes: `onDismiss: () -> Unit`, `onCreateAccount: (name: String, emoji: String, initialBalance: Double, description: String) -> Unit`
- Produces: `AddAccountBottomSheet` modal and neutral `TRANSFER` row rendering in `TransactionFeed`.

- [ ] **Step 1: Implement AddAccountBottomSheet**

Create `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/AddAccountBottomSheet.kt`:
- Emoji selection row: `🏦`, `💵`, `💳`, `🪙`, `🎯`, `💼`, `📈`, `🏝️`.
- Name text field with autofocus and clear label.
- Starting Balance numeric field with currency prefix (default: `0.00`).
- Optional description field.
- "Create Account" primary button calling `onCreateAccount(name, emoji, initialBalance, description)`.

- [ ] **Step 2: Update TransactionFeed to render TRANSFER transactions**

In `TransactionFeed.kt`:
- Pass `accounts: Map<Long, Account> = emptyMap()`.
- If `tx.type == TransactionType.TRANSFER`:
  - Category icon / emoji: `🔁`
  - Color: Neutral soft slate/cyan `Color(0xFFD1DBE6)` with `0.15f` alpha background.
  - Title: if `tx.note.isNotBlank()`, use `tx.note`, else `"${sourceAccount?.name ?: "Account"} ➔ ${targetAccount?.name ?: "Account"}"`.
  - Category label: "Internal Transfer".
  - Amount display: formatted without `+` or `-` prefix, neutral color `Color(0xFFD1DBE6)`.

- [ ] **Step 3: Wire AddAccountBottomSheet in HomeScreen.kt**

In `HomeScreen.kt`:
```kotlin
if (state.isAddAccountSheetOpen) {
    AddAccountBottomSheet(
        onDismiss = { screenModel.onIntent(HomeIntent.DismissAddAccount) },
        onCreateAccount = { name, emoji, balance, desc ->
            screenModel.onIntent(HomeIntent.CreateAccount(name, emoji, balance, desc))
        }
    )
}
```
And pass `accounts = state.accounts.associate { it.account.id to it.account }` to `TransactionFeed`.

- [ ] **Step 4: Run unit tests and compilation check**

Run: `$env:ANDROID_HOME="C:\Users\thanh\AppData\Local\Android\Sdk"; ./gradlew :shared:testDebugUnitTest`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit AddAccountBottomSheet and TransactionFeed update**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/AddAccountBottomSheet.kt shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/TransactionFeed.kt shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreen.kt
git commit -m "feat(ui): add AddAccountBottomSheet and render internal transfers in TransactionFeed"
```

---

### Task 7: Physical Android Device Build, Install & Tactile Verification

**Files:**
- Test / Verify on physical Android device `CPH2065` via ADB.

- [ ] **Step 1: Build and install debug APK to physical device**

Run:
```powershell
$env:ANDROID_HOME="C:\Users\thanh\AppData\Local\Android\Sdk"; ./gradlew :androidApp:installDebug
```
Expected: `BUILD SUCCESSFUL`, app installed on `CPH2065`.

- [ ] **Step 2: Launch app via ADB and verify initial state**

Run:
```powershell
adb shell am start -n com.monetracka.android/.MainActivity
adb exec-out screencap -p > C:\Users\thanh\.gemini\antigravity-ide\brain\13c49a1d-f7d8-496e-b3c0-37caeaa200c9\device_accounts_initial.png
```
Verify: Accounts strip displays default accounts (`🏦 Main Checking`, `💵 Cash Wallet`) and `+ Add` button.

- [ ] **Step 3: Test Account Creation via UI**

Tap `+ Add` account card, type account name (e.g. "Crypto Fund"), set initial balance (e.g. 500.00), tap "Create Account".
Verify via ADB screenshot that "Crypto Fund ($500.00)" appears in the accounts strip and total net wealth reflects the starting balance.

- [ ] **Step 4: Test Drag-and-Drop Transfer & Slide Confirmation**

Simulate drag from Bank account to Cash account (or open transfer modal), input transfer amount ($50.00), verify live delta preview:
- Bank: `-$50`
- Cash: `+$50`
Slide the confirmation slider to 100%.
Verify:
- Bank balance decreases by $50.
- Cash balance increases by $50.
- Total portfolio wealth remains unchanged (net-zero).
- Transaction feed displays `🔁 Main Checking ➔ Cash Wallet` with neutral amount `$50.00`.

- [ ] **Step 5: Capture final verification screenshot and commit walkthrough**

```bash
git commit --allow-empty -m "chore: verified accounts and drag-and-drop transfers on physical Android device"
```
