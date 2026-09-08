# Mobile Core Dashboard & Logging Flow Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the high-fidelity MoneTracka mobile dashboard and transaction logging flow in Compose Multiplatform matching `mobile-design-v0.html`.

**Architecture:** Voyager ScreenModel handles unidirectional MVI flow (`HomeUiState` / `HomeIntent`). Reactive SQLDelight queries feed transactions to compute real-time balance trends and category breakdowns. High-performance Jetpack Compose Canvas components render gradient sparklines and donut charts.

**Tech Stack:** Kotlin Multiplatform, Jetpack Compose Multiplatform, Voyager Navigator & ScreenModel, SQLDelight, Koin DI.

**Spec:** `docs/superpowers/specs/2026-09-08-mobile-core-dashboard-design.md`

## Global Constraints
- Target platforms: Android (API 26+) and iOS (iOS 15+)
- Styling: Deep navy canvas `#060B11`, surface `#172535`, neon mint `#00D09C`, coral `#FF5A79`
- Strictly no duplicate business logic: Calculations reside in ScreenModel / domain use-cases
- Do NOT run `dotnet build` or platform commands that break mobile workspace conventions

---

### Task 1: Domain Models & HomeUiState Definitions

**Files:**
- Create: `shared/src/commonMain/kotlin/com/monetracka/shared/domain/model/CategorySpend.kt`
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/domain/model/Transaction.kt`
- Create: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeUiState.kt`
- Test: `shared/src/commonTest/kotlin/com/monetracka/shared/domain/model/CategorySpendTest.kt`

**Interfaces:**
- Consumes: `Transaction`
- Produces: `CategorySpend`, `HomeUiState`, `HomeIntent`

- [x] **Step 1: Write unit test for CategorySpend calculation**
- [x] **Step 2: Run test to verify it fails**
- [x] **Step 3: Implement CategorySpend, updated Transaction, and HomeUiState**
- [x] **Step 4: Run test to verify it passes**
- [x] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/domain/model/CategorySpend.kt shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeUiState.kt shared/src/commonTest/kotlin/com/monetracka/shared/domain/model/CategorySpendTest.kt
git commit -m "feat: add CategorySpend model and HomeUiState MVI contract"
```

---

### Task 2: HomeScreenModel & Unidirectional MVI State Engine

**Files:**
- Create: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreenModel.kt`
- Create: `shared/src/commonTest/kotlin/com/monetracka/shared/ui/home/HomeScreenModelTest.kt`

**Interfaces:**
- Consumes: `TransactionRepository`, `HomeIntent`
- Produces: `StateFlow<HomeUiState>`

- [ ] **Step 1: Write failing unit test for HomeScreenModel**

```kotlin
package com.monetracka.shared.ui.home

import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class FakeTransactionRepository(private val txs: List<Transaction>) : TransactionRepository {
    override fun getAllTransactions(): Flow<List<Transaction>> = flowOf(txs)
    override suspend fun insertTransaction(tx: Transaction): Long = 1L
    override suspend fun deleteTransaction(id: Long) {}
    override suspend fun getTransactionById(id: Long): Transaction? = txs.find { it.id == id }
    override fun getTransactionsByCategory(category: String): Flow<List<Transaction>> = flowOf(emptyList())
}

class HomeScreenModelTest {
    @Test
    fun testBalanceCalculation() = runTest {
        val fakeTxs = listOf(
            Transaction(1L, 100.0, TransactionType.INCOME, "Salary", System.currentTimeMillis(), "Pay", "USD"),
            Transaction(2L, 40.0, TransactionType.EXPENSE, "Food", System.currentTimeMillis(), "Groceries", "USD")
        )
        val viewModel = HomeScreenModel(FakeTransactionRepository(fakeTxs))
        viewModel.init()
        assertEquals(60.0, viewModel.state.value.totalBalance, 0.01)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `gradlew :shared:testDebugUnitTest`
Expected: FAIL with "Unresolved reference: HomeScreenModel"

- [ ] **Step 3: Implement HomeScreenModel**

```kotlin
// shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreenModel.kt
package com.monetracka.shared.ui.home

import cafe.adriel.voyager.core.model.StateScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import com.monetracka.shared.domain.model.CategorySpend
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.domain.repository.TransactionRepository
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class HomeScreenModel(
    private val repository: TransactionRepository
) : StateScreenModel<HomeUiState>(HomeUiState(isLoading = true)) {

    fun init() {
        screenModelScope.launch {
            repository.getAllTransactions().collectLatest { transactions ->
                val totalIncome = transactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
                val totalExpense = transactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
                val balance = totalIncome - totalExpense

                val expensesByCategory = transactions
                    .filter { it.type == TransactionType.EXPENSE }
                    .groupBy { it.category }
                    .map { (category, txList) ->
                        CategorySpend(
                            category = category,
                            amount = txList.sumOf { it.amount },
                            totalSpend = totalExpense,
                            colorHex = 0xFF00D09C
                        )
                    }
                    .sortedByDescending { it.amount }

                mutableState.value = mutableState.value.copy(
                    totalBalance = balance,
                    categorySpends = expensesByCategory,
                    recentTransactions = transactions.take(15),
                    isLoading = false
                )
            }
        }
    }

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            HomeIntent.OpenAddTransaction -> mutableState.value = mutableState.value.copy(isAddSheetOpen = true)
            HomeIntent.DismissAddTransaction -> mutableState.value = mutableState.value.copy(isAddSheetOpen = false)
            is HomeIntent.CreateTransaction -> {
                screenModelScope.launch {
                    val newTx = Transaction(
                        id = 0L,
                        amount = intent.amount,
                        type = intent.type,
                        category = intent.category,
                        timestamp = System.currentTimeMillis(),
                        note = intent.note,
                        currency = "USD"
                    )
                    repository.insertTransaction(newTx)
                    mutableState.value = mutableState.value.copy(isAddSheetOpen = false)
                }
            }
            is HomeIntent.DeleteTransaction -> {
                screenModelScope.launch {
                    repository.deleteTransaction(intent.id)
                }
            }
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `gradlew :shared:testDebugUnitTest`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreenModel.kt shared/src/commonTest/kotlin/com/monetracka/shared/ui/home/HomeScreenModelTest.kt
git commit -m "feat: implement HomeScreenModel MVI state management"
```

---

### Task 3: Custom Compose Canvas Components (Sparkline & Donut Chart)

**Files:**
- Create: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/SparklineChart.kt`
- Create: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/CategoryDonutChart.kt`

**Interfaces:**
- Consumes: `List<Float>`, `List<CategorySpend>`
- Produces: `@Composable SparklineChart`, `@Composable CategoryDonutChart`

- [ ] **Step 1: Implement SparklineChart with Compose Canvas**

```kotlin
// shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/SparklineChart.kt
package com.monetracka.shared.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun SparklineChart(
    points: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF00D09C)
) {
    Canvas(modifier = modifier.fillMaxWidth().height(48.dp)) {
        if (points.size < 2) return@Canvas
        val maxY = (points.maxOrNull() ?: 1f).coerceAtLeast(1f)
        val minY = (points.minOrNull() ?: 0f)
        val range = (maxY - minY).coerceAtLeast(1f)
        
        val width = size.width
        val height = size.height
        val stepX = width / (points.size - 1)

        val path = Path()
        val fillPath = Path()

        points.forEachIndexed { i, p ->
            val x = i * stepX
            val y = height - ((p - minY) / range) * (height - 8.dp.toPx()) - 4.dp.toPx()
            if (i == 0) {
                path.moveTo(x, y)
                fillPath.moveTo(x, height)
                fillPath.lineTo(x, y)
            } else {
                path.lineTo(x, y)
                fillPath.lineTo(x, y)
            }
        }
        fillPath.lineTo(width, height)
        fillPath.close()

        drawPath(
            path = fillPath,
            brush = Brush.verticalGradient(
                colors = listOf(lineColor.copy(alpha = 0.35f), Color.Transparent)
            )
        )
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx())
        )
    }
}
```

- [ ] **Step 2: Implement CategoryDonutChart with Compose Canvas**

```kotlin
// shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/CategoryDonutChart.kt
package com.monetracka.shared.ui.home.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monetracka.shared.domain.model.CategorySpend

@Composable
fun CategoryDonutChart(
    categorySpends: List<CategorySpend>,
    totalText: String,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.size(76.dp), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val strokeWidth = 6.dp.toPx()
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                style = Stroke(strokeWidth)
            )

            var startAngle = -90f
            categorySpends.forEach { item ->
                val sweep = (item.percentage.toFloat() / 100f) * 360f
                if (sweep > 0f) {
                    drawArc(
                        color = Color(item.colorHex),
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = false,
                        style = Stroke(strokeWidth, cap = StrokeCap.Round)
                    )
                    startAngle += sweep
                }
            }
        }
        Text(
            text = totalText,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
```

- [ ] **Step 3: Verify Canvas render signatures and dependencies**

Check that Compose Canvas, Path, and Stroke imports compile without unresolved symbols.

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/SparklineChart.kt shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/CategoryDonutChart.kt
git commit -m "feat: add SparklineChart and CategoryDonutChart canvas components"
```

---

### Task 4: BalanceHeroCard & QuickActionBar Composables

**Files:**
- Create: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/BalanceHeroCard.kt`
- Create: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/QuickActionBar.kt`

**Interfaces:**
- Consumes: `totalBalance: Double`, `trendPercent: Double`, `onActionClicked: (String) -> Unit`
- Produces: `@Composable BalanceHeroCard`, `@Composable QuickActionBar`

- [ ] **Step 1: Implement BalanceHeroCard**

```kotlin
// shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/BalanceHeroCard.kt
package com.monetracka.shared.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun BalanceHeroCard(
    balance: Double,
    trendPercent: Double,
    sparklinePoints: List<Float>,
    modifier: Modifier = Modifier
) {
    val intPart = balance.toLong()
    val fracPart = ((kotlin.math.abs(balance) - kotlin.math.abs(intPart)) * 100).toInt()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1C2E42), Color(0xFF132232), Color(0xFF0E1A27))
                )
            )
            .border(1.dp, Color.White.copy(alpha = 0.1f), RoundedCornerShape(26.dp))
            .padding(20.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "NET PORTFOLIO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = Color(0xFF8FA2B6)
                )
                Text(
                    text = "USD ($)",
                    fontSize = 11.sp,
                    color = Color(0xFFE2E8F0),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(text = "$", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00D09C))
                Text(text = "$intPart", fontSize = 38.sp, fontWeight = FontWeight.ExtraBold, color = Color.White)
                Text(text = ".%02d".format(fracPart), fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8FA2B6))
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFF00D09C).copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "+%.1f%%".format(trendPercent),
                        color = Color(0xFF00D09C),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "this month",
                    color = Color(0xFF8FA2B6),
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(14.dp))
            SparklineChart(points = sparklinePoints)
        }
    }
}
```

- [ ] **Step 2: Implement QuickActionBar with spring feedback**

```kotlin
// shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/QuickActionBar.kt
package com.monetracka.shared.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuickActionBar(
    onAddExpense: () -> Unit,
    onIncome: () -> Unit,
    onScan: () -> Unit,
    onMore: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        QuickActionButton(label = "Add Expense", isPrimary = true, icon = "+", onClick = onAddExpense)
        QuickActionButton(label = "Income", icon = "↑", onClick = onIncome)
        QuickActionButton(label = "Scan Bill", icon = "📷", onClick = onScan)
        QuickActionButton(label = "More", icon = "•••", onClick = onMore)
    }
}

@Composable
private fun QuickActionButton(
    label: String,
    icon: String,
    isPrimary: Boolean = false,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (isPrimary) Brush.linearGradient(listOf(Color(0xFF00E4AB), Color(0xFF00B386)))
                    else Brush.linearGradient(listOf(Color(0xFF172535), Color(0xFF1C2D42)))
                )
        ) {
            Text(
                text = icon,
                fontSize = if (isPrimary) 22.sp else 18.sp,
                fontWeight = FontWeight.Bold,
                color = if (isPrimary) Color(0xFF022015) else Color.White
            )
        }
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = if (isPrimary) Color.White else Color(0xFF8FA2B6)
        )
    }
}
```

- [ ] **Step 3: Run project compilation check**

Run: `gradlew :shared:compileKotlinIosArm64 :shared:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/BalanceHeroCard.kt shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/QuickActionBar.kt
git commit -m "feat: implement BalanceHeroCard and QuickActionBar composables"
```

---

### Task 5: TransactionFeed & TransactionItemRow Composables

**Files:**
- Create: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/TransactionFeed.kt`

**Interfaces:**
- Consumes: `List<Transaction>`, `onTransactionClicked: (Transaction) -> Unit`
- Produces: `@Composable TransactionFeed`

- [ ] **Step 1: Implement TransactionFeed and ItemRow**

```kotlin
// shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/TransactionFeed.kt
package com.monetracka.shared.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType

@Composable
fun TransactionFeed(
    transactions: List<Transaction>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        transactions.forEach { tx ->
            TransactionRow(tx = tx)
        }
    }
}

@Composable
private fun TransactionRow(tx: Transaction) {
    val isExpense = tx.type == TransactionType.EXPENSE
    val amountColor = if (isExpense) Color(0xFFFF5A79) else Color(0xFF00D09C)
    val amountPrefix = if (isExpense) "-" else "+"

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF172535))
            .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
            .padding(14.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(amountColor.copy(alpha = 0.15f))
            ) {
                Text(
                    text = tx.category.take(1).uppercase(),
                    color = amountColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
            Column {
                Text(text = tx.note.ifBlank { tx.category }, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                Text(text = tx.category, color = Color(0xFF8FA2B6), fontSize = 11.sp)
            }
        }

        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$amountPrefix$%.2f".format(tx.amount),
                color = amountColor,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
            Text(text = "USD", color = Color(0xFF54687F), fontSize = 10.sp)
        }
    }
}
```

- [ ] **Step 2: Run compilation check**

Run: `gradlew :shared:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/TransactionFeed.kt
git commit -m "feat: implement TransactionFeed and TransactionRow with fintech card design"
```

---

### Task 6: AddTransactionBottomSheet Modal

**Files:**
- Create: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/transaction/AddTransactionBottomSheet.kt`

**Interfaces:**
- Consumes: `isOpen: Boolean`, `onDismiss: () -> Unit`, `onSave: (amount: Double, type: TransactionType, category: String, note: String) -> Unit`
- Produces: `@Composable AddTransactionBottomSheet`

- [ ] **Step 1: Implement AddTransactionBottomSheet using Material3 ModalBottomSheet**

```kotlin
// shared/src/commonMain/kotlin/com/monetracka/shared/ui/transaction/AddTransactionBottomSheet.kt
package com.monetracka.shared.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddTransactionBottomSheet(
    isOpen: Boolean,
    onDismiss: () -> Unit,
    onSave: (Double, TransactionType, String, String) -> Unit
) {
    if (!isOpen) return

    var amountStr by remember { mutableStateOf("") }
    var noteStr by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(TransactionType.EXPENSE) }
    var selectedCategory by remember { mutableStateOf("Food & Dining") }

    val categories = listOf("Food & Dining", "Transit", "Groceries", "Entertainment", "Tech", "Salary")

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0C1622),
        dragHandle = {
            Box(
                Modifier
                    .padding(vertical = 12.dp)
                    .width(48.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(9999.dp))
                    .background(Color.White.copy(alpha = 0.2f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp)
        ) {
            Text(text = "Log Transaction", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)

            Spacer(Modifier.height(16.dp))

            // Type Toggle
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF172535))
                    .padding(4.dp)
            ) {
                listOf(TransactionType.EXPENSE to "Expense", TransactionType.INCOME to "Income").forEach { (type, label) ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (selectedType == type) Color(0xFF00D09C) else Color.Transparent)
                            .clickable { selectedType = type }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = label,
                            color = if (selectedType == type) Color(0xFF022015) else Color(0xFF8FA2B6),
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = amountStr,
                onValueChange = { amountStr = it },
                label = { Text("Amount ($)") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00D09C),
                    unfocusedBorderColor = Color(0xFF243447)
                )
            )

            Spacer(Modifier.height(12.dp))

            OutlinedTextField(
                value = noteStr,
                onValueChange = { noteStr = it },
                label = { Text("Note / Merchant") },
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00D09C),
                    unfocusedBorderColor = Color(0xFF243447)
                )
            )

            Spacer(Modifier.height(20.dp))

            Button(
                onClick = {
                    val amt = amountStr.toDoubleOrNull() ?: 0.0
                    if (amt > 0.0) {
                        onSave(amt, selectedType, selectedCategory, noteStr)
                    }
                },
                modifier = Modifier.fillMaxWidth().height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00D09C)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text("Confirm Transaction", color = Color(0xFF022015), fontWeight = FontWeight.Bold)
            }
        }
    }
}
```

- [ ] **Step 2: Run compilation check**

Run: `gradlew :shared:compileDebugKotlinAndroid`
Expected: BUILD SUCCESSFUL

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/ui/transaction/AddTransactionBottomSheet.kt
git commit -m "feat: implement AddTransactionBottomSheet modal"
```

---

### Task 7: Root HomeScreen Integration & Bottom Dock Assembly

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreen.kt`
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/di/SharedModule.kt`

**Interfaces:**
- Consumes: `HomeScreenModel`, `BalanceHeroCard`, `QuickActionBar`, `CategoryDonutChart`, `TransactionFeed`, `AddTransactionBottomSheet`
- Produces: Complete `@Composable HomeScreen.Content()`

- [ ] **Step 1: Wire HomeScreenModel into Koin SharedModule**

```kotlin
// In shared/src/commonMain/kotlin/com/monetracka/shared/di/SharedModule.kt
val sharedModule = module {
    single<TransactionRepository> { TransactionRepositoryImpl(get()) }
    factory { HomeScreenModel(get()) }
}
```

- [ ] **Step 2: Update HomeScreen to assemble components into edge-to-edge mobile layout**

Assemble: Top User Bar -> `BalanceHeroCard` -> `QuickActionBar` -> `CategoryDonutChart` -> `TransactionFeed` -> Pinned bottom dock with center `+` button -> `AddTransactionBottomSheet`.

- [ ] **Step 3: Run full verification suite**

Run: `gradlew :shared:testDebugUnitTest :shared:compileDebugKotlinAndroid`
Expected: ALL TESTS PASS & BUILD SUCCESSFUL

- [ ] **Step 4: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreen.kt shared/src/commonMain/kotlin/com/monetracka/shared/di/SharedModule.kt
git commit -m "feat: wire HomeScreen edge-to-edge UI and Koin DI integration"
```
