# Smart Saving Coach & Financial Wisdom Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a dynamic Smart Saving Coach that evaluates user transactions to deliver real-time warnings, positive reinforcement, and habit insights, paired with an inspirational Financial Wisdom Quotes engine.

**Architecture:** Pure Kotlin reactive domain engines (`SmartSavingCoachEngine` and `QuoteRepository`) integrated into `HomeScreenModel`. Dynamic insights and quotes update strictly when the database emits transaction changes—maintaining 0% CPU on idle and instant UI responsiveness.

**Tech Stack:** Compose Multiplatform, Kotlin Coroutines & Flow, Koin DI, Kotlinx DateTime.

**Spec:** Dynamic savings analysis engine, contextual warnings/celebrations, curated financial advice cards.

## Global Constraints
- Target platform: Android & iOS via Compose Multiplatform.
- 0% idle CPU: pure reactive evaluation on SQLite Flow changes; no periodic polling or battery-draining tickers.
- Clean MVVM architecture: domain logic in `domain/coach/` and `domain/quote/`, decoupled from Compose UI.

---

### Task 1: Financial Wisdom Quotes Domain & Repository

**Files:**
- Create: `shared/src/commonMain/kotlin/com/monetracka/shared/domain/quote/FinancialQuote.kt`
- Create: `shared/src/commonMain/kotlin/com/monetracka/shared/domain/quote/QuoteRepository.kt`
- Test: `shared/src/commonTest/kotlin/com/monetracka/shared/domain/quote/QuoteRepositoryTest.kt`

**Interfaces:**
- Produces: `FinancialQuote(id: String, quote: String, author: String, category: QuoteCategory)`
- Produces: `QuoteRepository.getDailyQuote(): FinancialQuote`
- Produces: `QuoteRepository.getRandomQuote(excludeId: String?): FinancialQuote`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.monetracka.shared.domain.quote

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class QuoteRepositoryTest {
    private val repo = QuoteRepositoryImpl()

    @Test
    fun testGetDailyQuoteReturnsValidQuote() {
        val quote = repo.getDailyQuote()
        assertNotNull(quote.quote)
        assertTrue(quote.quote.isNotBlank())
        assertNotNull(quote.author)
    }

    @Test
    fun testGetRandomQuoteDifferentFromPrevious() {
        val first = repo.getDailyQuote()
        val next = repo.getRandomQuote(excludeId = first.id)
        assertTrue(next.id != first.id || repo.allQuotes.size <= 1)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :shared:testDebugUnitTest --tests "com.monetracka.shared.domain.quote.QuoteRepositoryTest"`
Expected: FAIL with compilation error (Unresolved reference: QuoteRepositoryImpl).

- [ ] **Step 3: Implement FinancialQuote and QuoteRepositoryImpl**

Create `shared/src/commonMain/kotlin/com/monetracka/shared/domain/quote/FinancialQuote.kt`:
```kotlin
package com.monetracka.shared.domain.quote

enum class QuoteCategory {
    SAVING,
    DISCIPLINE,
    INVESTING,
    MINDSET
}

data class FinancialQuote(
    val id: String,
    val quote: String,
    val author: String,
    val category: QuoteCategory
)

interface QuoteRepository {
    val allQuotes: List<FinancialQuote>
    fun getDailyQuote(): FinancialQuote
    fun getRandomQuote(excludeId: String? = null): FinancialQuote
}
```

Create `shared/src/commonMain/kotlin/com/monetracka/shared/domain/quote/QuoteRepository.kt`:
```kotlin
package com.monetracka.shared.domain.quote

import kotlinx.datetime.Clock
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

class QuoteRepositoryImpl : QuoteRepository {
    override val allQuotes = listOf(
        FinancialQuote("1", "Do not save what is left after spending, but spend what is left after saving.", "Warren Buffett", QuoteCategory.SAVING),
        FinancialQuote("2", "A budget is telling your money where to go instead of wondering where it went.", "Dave Ramsey", QuoteCategory.DISCIPLINE),
        FinancialQuote("3", "Beware of little expenses; a small leak will sink a great ship.", "Benjamin Franklin", QuoteCategory.SAVING),
        FinancialQuote("4", "Wealth is what you don't see. It's the cars not purchased, the watches not worn.", "Morgan Housel", QuoteCategory.MINDSET),
        FinancialQuote("5", "The goal isn't more money. The goal is living life on your terms.", "Chris Brogan", QuoteCategory.MINDSET),
        FinancialQuote("6", "Simplicity is the key to financial security and peace of mind.", "Charlie Munger", QuoteCategory.DISCIPLINE),
        FinancialQuote("7", "Financial freedom is available to those who learn about it and work for it.", "Robert Kiyosaki", QuoteCategory.INVESTING),
        FinancialQuote("8", "Spend less than you make, always be saving, and let compound interest do the heavy lifting.", "Naval Ravikant", QuoteCategory.INVESTING),
        FinancialQuote("9", "Discipline is choosing between what you want now and what you want most.", "Abraham Lincoln", QuoteCategory.DISCIPLINE),
        FinancialQuote("10", "It's not your salary that makes you rich, it's your spending habits.", "Charles A. Jaffe", QuoteCategory.SAVING)
    )

    override fun getDailyQuote(): FinancialQuote {
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        val dayOfYear = now.dayOfYear
        return allQuotes[dayOfYear % allQuotes.size]
    }

    override fun getRandomQuote(excludeId: String?): FinancialQuote {
        val candidates = if (excludeId != null && allQuotes.size > 1) {
            allQuotes.filter { it.id != excludeId }
        } else {
            allQuotes
        }
        return candidates.random()
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :shared:testDebugUnitTest --tests "com.monetracka.shared.domain.quote.QuoteRepositoryTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/domain/quote/ shared/src/commonTest/kotlin/com/monetracka/shared/domain/quote/
git commit -m "feat(domain): add FinancialQuote model and QuoteRepository"
```

---

### Task 2: Dynamic Smart Saving Coach Engine

**Files:**
- Create: `shared/src/commonMain/kotlin/com/monetracka/shared/domain/coach/CoachInsight.kt`
- Create: `shared/src/commonMain/kotlin/com/monetracka/shared/domain/coach/SmartSavingCoachEngine.kt`
- Test: `shared/src/commonTest/kotlin/com/monetracka/shared/domain/coach/SmartSavingCoachEngineTest.kt`

**Interfaces:**
- Produces: `CoachInsight(type: InsightType, title: String, message: String, metricText: String?, emoji: String)`
- Produces: `SmartSavingCoachEngine.evaluate(transactions: List<Transaction>, categories: Map<Long, Category>): CoachInsight`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.monetracka.shared.domain.coach

import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class SmartSavingCoachEngineTest {
    private val engine = SmartSavingCoachEngine()
    private val categories = mapOf(
        1L to Category(1L, "Dining Out", "🍔", 0, TransactionType.EXPENSE),
        2L to Category(2L, "Salary", "💰", 1, TransactionType.INCOME)
    )

    @Test
    fun testEmptyTransactionsGivesWelcomeInsight() {
        val insight = engine.evaluate(emptyList(), categories)
        assertEquals(InsightType.PRAISE, insight.type)
        assertEquals("🌱 Ready to Save", insight.title)
    }

    @Test
    fun testHighSavingsRateTriggersCelebration() {
        val txs = listOf(
            Transaction(1, 2000.0, TransactionType.INCOME, 2, "Salary", 1000L),
            Transaction(2, 400.0, TransactionType.EXPENSE, 1, "Dinner", 1001L)
        )
        val insight = engine.evaluate(txs, categories)
        assertEquals(InsightType.PRAISE, insight.type)
        assertEquals("🎉 Super Saver Streak", insight.title)
    }

    @Test
    fun testHighSingleCategorySpendTriggersWarning() {
        val txs = listOf(
            Transaction(1, 1000.0, TransactionType.INCOME, 2, "Salary", 1000L),
            Transaction(2, 700.0, TransactionType.EXPENSE, 1, "Fancy Meals", 1001L)
        )
        val insight = engine.evaluate(txs, categories)
        assertEquals(InsightType.WARNING, insight.type)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :shared:testDebugUnitTest --tests "com.monetracka.shared.domain.coach.SmartSavingCoachEngineTest"`
Expected: FAIL with compilation error (Unresolved reference: SmartSavingCoachEngine).

- [ ] **Step 3: Implement CoachInsight and SmartSavingCoachEngine**

Create `shared/src/commonMain/kotlin/com/monetracka/shared/domain/coach/CoachInsight.kt`:
```kotlin
package com.monetracka.shared.domain.coach

enum class InsightType {
    PRAISE,     // Green - good saving habits, high savings rate
    WARNING,    // Coral - overspending, high single category burn
    TIP         // Blue - budgeting encouragement
}

data class CoachInsight(
    val type: InsightType,
    val title: String,
    val message: String,
    val metricText: String? = null,
    val emoji: String = "💡"
)
```

Create `shared/src/commonMain/kotlin/com/monetracka/shared/domain/coach/SmartSavingCoachEngine.kt`:
```kotlin
package com.monetracka.shared.domain.coach

import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.domain.util.CurrencyFormatter

class SmartSavingCoachEngine {

    fun evaluate(
        transactions: List<Transaction>,
        categories: Map<Long, Category>
    ): CoachInsight {
        if (transactions.isEmpty()) {
            return CoachInsight(
                type = InsightType.PRAISE,
                title = "🌱 Ready to Save",
                message = "Log your first expense or income to unlock dynamic financial insights and savings analysis.",
                emoji = "🌱"
            )
        }

        val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }

        // Condition 1: Deficit Warning
        if (totalExpense > totalIncome && totalIncome > 0) {
            val deficit = totalExpense - totalIncome
            return CoachInsight(
                type = InsightType.WARNING,
                title = "⚠️ Spending Exceeds Income",
                message = "You're spending $${CurrencyFormatter.format(deficit)} more than you've earned. Review discretionary categories.",
                metricText = "-$${CurrencyFormatter.format(deficit)} Net",
                emoji = "⚠️"
            )
        }

        // Condition 2: High Category Dominance Warning (>50% of expenses in one category)
        if (totalExpense > 0) {
            val topCategorySpend = transactions
                .filter { it.type == TransactionType.EXPENSE }
                .groupBy { it.categoryId }
                .maxByOrNull { entry -> entry.value.sumOf { it.amount } }

            if (topCategorySpend != null) {
                val catAmount = topCategorySpend.value.sumOf { it.amount }
                val ratio = catAmount / totalExpense
                if (ratio >= 0.50 && totalExpense >= 100.0) {
                    val catName = categories[topCategorySpend.key]?.name ?: "One category"
                    val percent = (ratio * 100).toInt()
                    return CoachInsight(
                        type = InsightType.WARNING,
                        title = "📊 High Category Concentration",
                        message = "$catName takes up $percent% of all your expenses. Consider setting a sub-budget.",
                        metricText = "$percent% on $catName",
                        emoji = "⚡"
                    )
                }
            }
        }

        // Condition 3: Excellent Savings Rate Celebration (>= 40%)
        if (totalIncome > 0) {
            val savingsRate = ((totalIncome - totalExpense) / totalIncome) * 100.0
            if (savingsRate >= 40.0) {
                return CoachInsight(
                    type = InsightType.PRAISE,
                    title = "🎉 Super Saver Streak",
                    message = "You've saved ${savingsRate.toInt()}% of your income. You are on track for accelerated financial independence!",
                    metricText = "${savingsRate.toInt()}% Saved",
                    emoji = "🎯"
                )
            } else if (savingsRate in 15.0..39.9) {
                return CoachInsight(
                    type = InsightType.PRAISE,
                    title = "✨ Healthy Cash Flow",
                    message = "Positive savings rate of ${savingsRate.toInt()}%. You are living well below your means.",
                    metricText = "+${savingsRate.toInt()}% Flow",
                    emoji = "📈"
                )
            }
        }

        // Condition 4: Default Balanced Advice
        return CoachInsight(
            type = InsightType.TIP,
            title = "💡 Steady Progress",
            message = "Every transaction logged brings clarity to your wealth journey. Keep tracking consistently.",
            emoji = "🛡️"
        )
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat :shared:testDebugUnitTest --tests "com.monetracka.shared.domain.coach.SmartSavingCoachEngineTest"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/domain/coach/ shared/src/commonTest/kotlin/com/monetracka/shared/domain/coach/
git commit -m "feat(domain): implement dynamic SmartSavingCoachEngine"
```

---

### Task 3: Integrate Coach & Quotes in DI, State, and Model

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeUiState.kt`
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/di/SharedModule.kt`
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreenModel.kt`

**Interfaces:**
- Consumes: `SmartSavingCoachEngine.evaluate(...)`, `QuoteRepository.getDailyQuote()`, `QuoteRepository.getRandomQuote(...)`
- Produces: `HomeUiState.coachInsight: CoachInsight?`, `HomeUiState.financialQuote: FinancialQuote`
- Produces: `HomeIntent.RefreshQuote: HomeIntent`

- [ ] **Step 1: Update HomeUiState**

In `HomeUiState.kt`, add `coachInsight: CoachInsight? = null`, `financialQuote: FinancialQuote? = null`, and `data object RefreshQuote : HomeIntent`.

- [ ] **Step 2: Register in SharedModule**

In `SharedModule.kt`, provide singletons:
```kotlin
single<QuoteRepository> { QuoteRepositoryImpl() }
single { SmartSavingCoachEngine() }
factory { HomeScreenModel(get(), get(), get(), get()) }
```

- [ ] **Step 3: Update HomeScreenModel**

In `HomeScreenModel.kt`:
Inject `coachEngine: SmartSavingCoachEngine` and `quoteRepository: QuoteRepository`.
Compute `coachInsight = coachEngine.evaluate(transactions, categoryMap)` inside `combine`.
Handle `HomeIntent.RefreshQuote` to get a fresh random quote.

- [ ] **Step 4: Compile & test shared module**

Run: `.\gradlew.bat :shared:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeUiState.kt shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreenModel.kt shared/src/commonMain/kotlin/com/monetracka/shared/di/SharedModule.kt
git commit -m "feat(state): integrate CoachInsight and QuoteRepository into HomeScreenModel"
```

---

### Task 4: UI Components: SmartCoachCard & DailyQuoteCard

**Files:**
- Create: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/SmartCoachCard.kt`
- Create: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/DailyQuoteCard.kt`

**Interfaces:**
- Produces: `SmartCoachCard(insight: CoachInsight, modifier: Modifier = Modifier)`
- Produces: `DailyQuoteCard(quote: FinancialQuote, onRefresh: () -> Unit, modifier: Modifier = Modifier)`

- [ ] **Step 1: Implement SmartCoachCard**

Create `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/SmartCoachCard.kt`:
- Dynamic border and glow based on `InsightType` (Mint Green for PRAISE, Coral Red for WARNING, Sky Blue for TIP).
- Header row with icon, title, and optional metric pill.
- Descriptive message advising the user on their exact saving habit.

- [ ] **Step 2: Implement DailyQuoteCard**

Create `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/DailyQuoteCard.kt`:
- Subtle dark card with quotation mark accent.
- Italicized inspiring wisdom quote and author badge.
- Interactive refresh button (`↻`) to cycle through wise financial advice.

- [ ] **Step 3: Compile shared module**

Run: `.\gradlew.bat :shared:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/SmartCoachCard.kt shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/DailyQuoteCard.kt
git commit -m "feat(ui): create SmartCoachCard and DailyQuoteCard composables"
```

---

### Task 5: Integrate Cards into HomeScreen Dashboard

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreen.kt`

**Interfaces:**
- Consumes: `SmartCoachCard`, `DailyQuoteCard`, `HomeIntent.RefreshQuote`

- [ ] **Step 1: Add SmartCoachCard & DailyQuoteCard to OverviewView**

In `HomeScreen.kt` inside `OverviewView`:
- Position `SmartCoachCard` immediately under `BalanceHeroCard` so the user is greeted with actionable dynamic feedback first thing.
- Position `DailyQuoteCard` right above or below `Spending Categories` to provide continuous financial mindset reinforcement.
- Wire quote refresh button to `screenModel.onIntent(HomeIntent.RefreshQuote)`.

- [ ] **Step 2: Compile & verify clean build**

Run: `.\gradlew.bat :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL.

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreen.kt
git commit -m "feat(ui): display SmartCoachCard and DailyQuoteCard in HomeScreen"
```

---

### Task 6: Full Verification & Device Deployment

- [ ] **Step 1: Run all unit tests**
Run: `.\gradlew.bat testDebugUnitTest`
Expected: All tests PASS.

- [ ] **Step 2: Build final APK**
Run: `.\gradlew.bat :androidApp:assembleDebug`
Expected: BUILD SUCCESSFUL in <45s.

- [ ] **Step 3: Push APK to connected device**
Command: `adb install -r androidApp/build/outputs/apk/debug/androidApp-debug.apk`
Expected: Success.
