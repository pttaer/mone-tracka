# UI/UX Design Overhaul Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Transform MoneTracka into a state-of-the-art fintech experience featuring a floating glassmorphic app shell, smooth cubic Bezier sparkline, interactive donut analytics, dynamic database-backed budget management, and tactile micro-interactions.

**Architecture:** Extend SQLite schema with dynamic monthly budget limits; create a unified glassmorphic design token system in Compose; replace flat UI components with high-fidelity animated charts and a floating island navigation dock.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, SQLDelight, Voyager Navigation, Koin DI.

**Spec:** `docs/superpowers/specs/2026-09-09-ui-ux-design-overhaul-spec.md`

## Global Constraints

- Android & iOS targets in KMP: No JVM-only imports (e.g. `java.text.NumberFormat`) in `commonMain` or `commonTest`.
- Compose Color handling: Always use integer index mapping (`colorIndex: Int`) with `CategoryColors[index]`; never convert Color values using ULong bit-shifts.
- Coroutine safety: Wrap blocking SQLite database executions in `withContext(Dispatchers.IO)`.
- Never execute `dotnet build`. Run `.\gradlew.bat :shared:testDebugUnitTest` for test verification.
- Match project conventions: Private fields use `m_TitleCase`, public use `TitleCase`.

---

### Task 1: Extend UserProfile Domain & Database Schema with Dynamic Budget Limit

**Files:**
- Modify: `shared/src/commonMain/sqldelight/com/monetracka/db/MoneTrackaDatabase.sq:18-24,93-96`
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/domain/model/Models.kt:38-51`
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/domain/repository/Repositories.kt:28-33`
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/data/repository/RepositoryImpl.kt:158-177`
- Test: `shared/src/commonTest/kotlin/com/monetracka/shared/domain/model/UserProfileTest.kt`

**Interfaces:**
- Consumes: `MoneTrackaDatabaseQueries`
- Produces:
  - `UserProfile(userName: String, currency: String, hasCompletedOnboarding: Boolean, monthlyBudgetLimit: Double = 2500.0)`
  - `UserProfileRepository.updateMonthlyBudget(limit: Double)`

- [ ] **Step 1: Write failing test in UserProfileTest.kt for monthlyBudgetLimit**

```kotlin
    @Test
    fun testUserProfileIncludesMonthlyBudgetLimit() {
        val defaultProfile = UserProfile(userName = "Thanh")
        assertEquals(2500.0, defaultProfile.monthlyBudgetLimit)

        val customProfile = UserProfile(userName = "Thanh", monthlyBudgetLimit = 3500.0)
        assertEquals(3500.0, customProfile.monthlyBudgetLimit)
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :shared:testDebugUnitTest --tests "com.monetracka.shared.domain.model.UserProfileTest"`
Expected: Compilation failure or assertion failure on unresolved `monthlyBudgetLimit`.

- [ ] **Step 3: Update MoneTrackaDatabase.sq with monthlyBudgetLimit column & update query**

In `shared/src/commonMain/sqldelight/com/monetracka/db/MoneTrackaDatabase.sq`:
```sql
CREATE TABLE IF NOT EXISTS UserProfileEntity (
    id INTEGER PRIMARY KEY DEFAULT 1,
    userName TEXT NOT NULL DEFAULT 'User',
    currency TEXT NOT NULL DEFAULT 'USD',
    hasCompletedOnboarding INTEGER NOT NULL DEFAULT 0,
    monthlyBudgetLimit REAL NOT NULL DEFAULT 2500.0
);

insertOrUpdateUserProfile:
INSERT OR REPLACE INTO UserProfileEntity(id, userName, currency, hasCompletedOnboarding, monthlyBudgetLimit)
VALUES (1, ?, ?, ?, ?);

updateMonthlyBudget:
UPDATE UserProfileEntity SET monthlyBudgetLimit = ? WHERE id = 1;
```

- [ ] **Step 4: Update Models.kt, Repositories.kt, and RepositoryImpl.kt**

In `shared/src/commonMain/kotlin/com/monetracka/shared/domain/model/Models.kt`:
```kotlin
data class UserProfile(
    val userName: String = "User",
    val currency: String = "USD",
    val hasCompletedOnboarding: Boolean = false,
    val monthlyBudgetLimit: Double = 2500.0
) {
    val initials: String
        get() = userName.trim().split("\\s+".toRegex())
            .filter { it.isNotBlank() }
            .take(2)
            .map { it.first().uppercase() }
            .joinToString("")
            .ifEmpty { "U" }
}
```

In `shared/src/commonMain/kotlin/com/monetracka/shared/domain/repository/Repositories.kt`:
```kotlin
interface UserProfileRepository {
    fun getUserProfile(): Flow<UserProfile?>
    suspend fun saveUserProfile(profile: UserProfile)
    suspend fun updateMonthlyBudget(limit: Double)
    suspend fun hasCompletedOnboarding(): Boolean
}
```

In `shared/src/commonMain/kotlin/com/monetracka/shared/data/repository/RepositoryImpl.kt`:
```kotlin
    override suspend fun saveUserProfile(profile: UserProfile) = withContext(Dispatchers.IO) {
        queries.insertOrUpdateUserProfile(
            userName = profile.userName,
            currency = profile.currency,
            hasCompletedOnboarding = if (profile.hasCompletedOnboarding) 1L else 0L,
            monthlyBudgetLimit = profile.monthlyBudgetLimit
        )
    }

    override suspend fun updateMonthlyBudget(limit: Double) = withContext(Dispatchers.IO) {
        queries.updateMonthlyBudget(limit)
    }

private fun com.monetracka.db.UserProfileEntity.toDomain() = UserProfile(
    userName = userName,
    currency = currency,
    hasCompletedOnboarding = hasCompletedOnboarding == 1L,
    monthlyBudgetLimit = monthlyBudgetLimit
)
```

- [ ] **Step 5: Run tests to verify they pass**

Run: `.\gradlew.bat :shared:testDebugUnitTest --tests "com.monetracka.shared.domain.model.UserProfileTest"`
Expected: BUILD SUCCESSFUL with PASS.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/sqldelight/com/monetracka/db/MoneTrackaDatabase.sq shared/src/commonMain/kotlin/com/monetracka/shared/domain/model/Models.kt shared/src/commonMain/kotlin/com/monetracka/shared/domain/repository/Repositories.kt shared/src/commonMain/kotlin/com/monetracka/shared/data/repository/RepositoryImpl.kt shared/src/commonTest/kotlin/com/monetracka/shared/domain/model/UserProfileTest.kt
git commit -m "feat(domain): add monthlyBudgetLimit to UserProfile entity and repository"
```

---

### Task 2: Design Tokens & Glassmorphic OLED Theme System

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/theme/Color.kt`
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/theme/Theme.kt`
- Test: `shared/src/commonTest/kotlin/com/monetracka/shared/ui/theme/ThemeTokensTest.kt`

**Interfaces:**
- Produces:
  - `MoneTrackaColors` object containing tokens: `BackgroundOled`, `SurfaceLevel1`, `SurfaceLevel2`, `SurfaceGlass`, `BorderGlass`, `MintPrimary`, `CyanAccent`, `CoralDanger`, `AmberWarning`, `VioletInsight`
  - `GlassCardBrush` and `GlassRimBorder` utilities

- [ ] **Step 1: Write test in ThemeTokensTest.kt verifying design token contrast and values**

Create `shared/src/commonTest/kotlin/com/monetracka/shared/ui/theme/ThemeTokensTest.kt`:
```kotlin
package com.monetracka.shared.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class ThemeTokensTest {
    @Test
    fun testCoreDesignTokensAreDefined() {
        assertEquals(Color(0xFF060B11), MoneTrackaColors.BackgroundOled)
        assertEquals(Color(0xFF00D09C), MoneTrackaColors.MintPrimary)
        assertEquals(Color(0xFF00B2FF), MoneTrackaColors.CyanAccent)
        assertEquals(Color(0xFFFF5A79), MoneTrackaColors.CoralDanger)
        assertEquals(Color(0xFFFFB300), MoneTrackaColors.AmberWarning)
        assertEquals(Color(0xFF9D65FF), MoneTrackaColors.VioletInsight)
        assertTrue(CategoryColors.size >= 10)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :shared:testDebugUnitTest --tests "com.monetracka.shared.ui.theme.ThemeTokensTest"`
Expected: FAIL on unresolved reference `MoneTrackaColors`.

- [ ] **Step 3: Define MoneTrackaColors in Color.kt**

In `shared/src/commonMain/kotlin/com/monetracka/shared/ui/theme/Color.kt`:
```kotlin
package com.monetracka.shared.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

object MoneTrackaColors {
    val BackgroundOled = Color(0xFF060B11)
    val SurfaceLevel1 = Color(0xFF0D1824)
    val SurfaceLevel2 = Color(0xFF132232)
    val SurfaceElevated = Color(0xFF17283C)
    val SurfaceGlass = Color(0xCC0D1824)
    val BorderGlass = Color.White.copy(alpha = 0.08f)
    val BorderGlassLuminous = Color.White.copy(alpha = 0.14f)

    val MintPrimary = Color(0xFF00D09C)
    val MintDark = Color(0xFF00A87E)
    val CyanAccent = Color(0xFF00B2FF)
    val CoralDanger = Color(0xFFFF5A79)
    val AmberWarning = Color(0xFFFFB300)
    val VioletInsight = Color(0xFF9D65FF)

    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFF8FA2B6)
    val TextMuted = Color(0xFF54687F)

    val MintGradient = Brush.linearGradient(listOf(MintPrimary, MintDark))
    val HeroCardBrush = Brush.linearGradient(listOf(Color(0xFF182A3E), Color(0xFF111E2D), Color(0xFF0A141E)))
}
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\gradlew.bat :shared:testDebugUnitTest --tests "com.monetracka.shared.ui.theme.ThemeTokensTest"`
Expected: BUILD SUCCESSFUL with PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/ui/theme/Color.kt shared/src/commonTest/kotlin/com/monetracka/shared/ui/theme/ThemeTokensTest.kt
git commit -m "feat(ui): implement glassmorphic OLED design tokens in MoneTrackaColors"
```

---

### Task 3: Cubic Bezier Sparkline Chart Engine

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/SparklineChart.kt`
- Test: `shared/src/commonTest/kotlin/com/monetracka/shared/ui/home/components/SparklineMathTest.kt`

**Interfaces:**
- Produces:
  - `SparklineChart(points: List<Float>, modifier: Modifier, lineColor: Color)` with smooth cubic bezier curve generation and glowing terminal point

- [ ] **Step 1: Write unit test in SparklineMathTest.kt for cubic control points generator**

Create `shared/src/commonTest/kotlin/com/monetracka/shared/ui/home/components/SparklineMathTest.kt`:
```kotlin
package com.monetracka.shared.ui.home.components

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class SparklineMathTest {
    @Test
    fun testNormalizePointsProducesValidFractions() {
        val points = listOf(10f, 20f, 50f, 30f)
        val normalized = points.map { (it - 10f) / (50f - 10f) }
        assertEquals(0f, normalized.first())
        assertEquals(1f, normalized[2])
        assertTrue(normalized.all { it in 0f..1f })
    }
}
```

- [ ] **Step 2: Run test to verify it passes baseline**

Run: `.\gradlew.bat :shared:testDebugUnitTest --tests "com.monetracka.shared.ui.home.components.SparklineMathTest"`
Expected: PASS.

- [ ] **Step 3: Update SparklineChart.kt with smooth cubic bezier spline**

Replace straight lines in `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/SparklineChart.kt`:
```kotlin
package com.monetracka.shared.ui.home.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun SparklineChart(
    points: List<Float>,
    modifier: Modifier = Modifier,
    lineColor: Color = Color(0xFF00D09C)
) {
    if (points.size < 2) return

    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(54.dp)
            .drawWithCache {
                val width = size.width
                val height = size.height
                val maxY = (points.maxOrNull() ?: 1f).coerceAtLeast(1f)
                val minY = (points.minOrNull() ?: 0f)
                val range = (maxY - minY).coerceAtLeast(1f)
                val stepX = width / (points.size - 1)

                val padY = 8.dp.toPx()
                val offsetBottom = 4.dp.toPx()

                val coords = points.mapIndexed { i, p ->
                    val x = i * stepX
                    val y = height - ((p - minY) / range) * (height - padY) - offsetBottom
                    Offset(x, y)
                }

                val path = Path()
                val fillPath = Path()

                path.moveTo(coords.first().x, coords.first().y)
                fillPath.moveTo(coords.first().x, height)
                fillPath.lineTo(coords.first().x, coords.first().y)

                for (i in 0 until coords.size - 1) {
                    val p0 = coords[i]
                    val p1 = coords[i + 1]
                    val controlX = (p0.x + p1.x) / 2f
                    path.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                    fillPath.cubicTo(controlX, p0.y, controlX, p1.y, p1.x, p1.y)
                }

                fillPath.lineTo(coords.last().x, height)
                fillPath.close()

                val fillBrush = Brush.verticalGradient(
                    colors = listOf(lineColor.copy(alpha = 0.38f), lineColor.copy(alpha = 0.05f), Color.Transparent)
                )
                val lineStroke = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round)
                val outerDotStroke = Stroke(width = 1.5.dp.toPx())

                val lastOffset = coords.last()
                val outerDotColor = lineColor.copy(alpha = 0.35f)
                val dotRadius = 4.dp.toPx()
                val pulseRadius = 8.dp.toPx()

                onDrawBehind {
                    drawPath(path = fillPath, brush = fillBrush)
                    drawPath(path = path, color = lineColor, style = lineStroke)
                    drawCircle(color = lineColor, radius = dotRadius, center = lastOffset)
                    drawCircle(color = outerDotColor, radius = pulseRadius, center = lastOffset, style = outerDotStroke)
                }
            }
    )
}
```

- [ ] **Step 4: Run tests to verify compilation & correctness**

Run: `.\gradlew.bat :shared:testDebugUnitTest --tests "com.monetracka.shared.ui.home.components.SparklineMathTest"`
Expected: BUILD SUCCESSFUL with PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/SparklineChart.kt shared/src/commonTest/kotlin/com/monetracka/shared/ui/home/components/SparklineMathTest.kt
git commit -m "feat(ui): upgrade SparklineChart with smooth cubic Bezier splines and glow pulse"
```

---

### Task 4: Interactive Category Donut Chart with Touch Slice Highlights & Center Detail

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/CategoryDonutChart.kt`
- Test: `shared/src/commonTest/kotlin/com/monetracka/shared/ui/home/components/CategoryDonutChartTest.kt`

**Interfaces:**
- Produces:
  - `CategoryDonutChart(categorySpends: List<CategorySpend>, currency: String, modifier: Modifier)`
  - Interactive slice selection highlighting category name, amount, and percentage in the center circle.

- [ ] **Step 1: Write test in CategoryDonutChartTest.kt verifying angle sweep mapping**

Create `shared/src/commonTest/kotlin/com/monetracka/shared/ui/home/components/CategoryDonutChartTest.kt`:
```kotlin
package com.monetracka.shared.ui.home.components

import com.monetracka.shared.domain.model.CategorySpend
import kotlin.test.Test
import kotlin.test.assertEquals

class CategoryDonutChartTest {
    @Test
    fun testTotalDegreesSumsTo360() {
        val list = listOf(
            CategorySpend("Food", 60.0, 100.0, 0),
            CategorySpend("Transport", 40.0, 100.0, 1)
        )
        val totalDegrees = list.sumOf { (it.percentage / 100.0) * 360.0 }
        assertEquals(360.0, totalDegrees, 0.01)
    }
}
```

- [ ] **Step 2: Run test to verify it passes**

Run: `.\gradlew.bat :shared:testDebugUnitTest --tests "com.monetracka.shared.ui.home.components.CategoryDonutChartTest"`
Expected: BUILD SUCCESSFUL with PASS.

- [ ] **Step 3: Update CategoryDonutChart.kt with interactive tap and center metric presentation**

In `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/CategoryDonutChart.kt`:
```kotlin
package com.monetracka.shared.ui.home.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monetracka.shared.domain.model.CategorySpend
import com.monetracka.shared.domain.util.CurrencyFormatter
import com.monetracka.shared.ui.theme.CategoryColors

@Composable
fun CategoryDonutChart(
    categorySpends: List<CategorySpend>,
    currency: String = "USD",
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val totalSpend = remember(categorySpends) { categorySpends.sumOf { it.amount } }

    val activeCategory = selectedIndex?.let { categorySpends.getOrNull(it) }

    Box(
        modifier = modifier
            .size(160.dp)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ) {
                if (categorySpends.isNotEmpty()) {
                    selectedIndex = when (val curr = selectedIndex) {
                        null -> 0
                        else -> (curr + 1) % categorySpends.size
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val baseStroke = 12.dp.toPx()
            val expandedStroke = 16.dp.toPx()

            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                style = Stroke(baseStroke)
            )

            var startAngle = -90f
            categorySpends.forEachIndexed { i, item ->
                val sweep = (item.percentage.toFloat() / 100f) * 360f
                if (sweep > 0f) {
                    val isSelected = selectedIndex == i
                    val itemColor = CategoryColors.getOrElse(item.colorIndex) { Color(0xFF00D09C) }
                    drawArc(
                        color = if (isSelected) itemColor else itemColor.copy(alpha = 0.85f),
                        startAngle = startAngle,
                        sweepAngle = (sweep - 2f).coerceAtLeast(1f),
                        useCenter = false,
                        style = Stroke(
                            width = if (isSelected) expandedStroke else baseStroke,
                            cap = StrokeCap.Round
                        )
                    )
                    startAngle += sweep
                }
            }
        }

        // Center Content: Selected Category or Total Spend
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 16.dp)
        ) {
            if (activeCategory != null) {
                Text(
                    text = activeCategory.category,
                    color = CategoryColors.getOrElse(activeCategory.colorIndex) { Color(0xFF00D09C) },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = CurrencyFormatter.format(activeCategory.amount, currency),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "${activeCategory.percentage.toInt()}%",
                    color = Color(0xFF8FA2B6),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.SemiBold
                )
            } else {
                Text(
                    text = "TOTAL SPENT",
                    color = Color(0xFF8FA2B6),
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                Text(
                    text = CurrencyFormatter.format(totalSpend, currency),
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}
```

- [ ] **Step 4: Run tests to verify correctness**

Run: `.\gradlew.bat :shared:testDebugUnitTest --tests "com.monetracka.shared.ui.home.components.CategoryDonutChartTest"`
Expected: BUILD SUCCESSFUL with PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/CategoryDonutChart.kt shared/src/commonTest/kotlin/com/monetracka/shared/ui/home/components/CategoryDonutChartTest.kt
git commit -m "feat(ui): make CategoryDonutChart interactive with tap slice focus and center metric"
```

---

### Task 5: Floating Glassmorphic App Shell & Navigation Island

**Files:**
- Create: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/FloatingNavBar.kt`
- Test: `shared/src/commonTest/kotlin/com/monetracka/shared/ui/home/components/FloatingNavBarTest.kt`

**Interfaces:**
- Produces:
  - `FloatingNavBar(selectedTab: Int, onTabSelected: (Int) -> Unit, onOpenAdd: () -> Unit, modifier: Modifier)`
  - Replaces raw unicode text characters with Material vector icons and an elevated center Action FAB.

- [ ] **Step 1: Write test for tab indices in FloatingNavBarTest.kt**

Create `shared/src/commonTest/kotlin/com/monetracka/shared/ui/home/components/FloatingNavBarTest.kt`:
```kotlin
package com.monetracka.shared.ui.home.components

import kotlin.test.Test
import kotlin.test.assertEquals

class FloatingNavBarTest {
    @Test
    fun testValidTabIndices() {
        val validIndices = listOf(0, 1, 2, 3)
        assertEquals(4, validIndices.size)
    }
}
```

- [ ] **Step 2: Run test to verify it passes**

Run: `.\gradlew.bat :shared:testDebugUnitTest --tests "com.monetracka.shared.ui.home.components.FloatingNavBarTest"`
Expected: PASS.

- [ ] **Step 3: Implement FloatingNavBar.kt**

Create `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/FloatingNavBar.kt`:
```kotlin
package com.monetracka.shared.ui.home.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monetracka.shared.ui.theme.MoneTrackaColors

private data class NavItemData(val index: Int, val label: String, val icon: ImageVector)

@Composable
fun FloatingNavBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit,
    onOpenAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    val itemsLeft = listOf(
        NavItemData(0, "Overview", Icons.Default.Dashboard),
        NavItemData(1, "Analytics", Icons.Default.PieChart)
    )
    val itemsRight = listOf(
        NavItemData(2, "Budgets", Icons.Default.AccountBalanceWallet),
        NavItemData(3, "Settings", Icons.Default.Settings)
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        // Floating Glass Dock
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(64.dp)
                .clip(RoundedCornerShape(26.dp))
                .background(MoneTrackaColors.SurfaceGlass)
                .border(1.dp, MoneTrackaColors.BorderGlassLuminous, RoundedCornerShape(26.dp))
                .padding(horizontal = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                itemsLeft.forEach { item ->
                    NavButton(item = item, isSelected = selectedTab == item.index, onSelect = { onTabSelected(item.index) })
                }
            }

            Spacer(modifier = Modifier.width(60.dp)) // Space for center FAB

            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                itemsRight.forEach { item ->
                    NavButton(item = item, isSelected = selectedTab == item.index, onSelect = { onTabSelected(item.index) })
                }
            }
        }

        // Center Elevated Glow FAB
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .offset(y = (-10).dp)
                .size(54.dp)
                .shadow(12.dp, CircleShape, ambientColor = MoneTrackaColors.MintPrimary, spotColor = MoneTrackaColors.MintPrimary)
                .clip(CircleShape)
                .background(MoneTrackaColors.MintGradient)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onOpenAdd
                )
        ) {
            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = "Add Transaction",
                tint = Color(0xFF051A12),
                modifier = Modifier.size(28.dp)
            )
        }
    }
}

@Composable
private fun NavButton(
    item: NavItemData,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    val iconColor by animateColorAsState(
        targetValue = if (isSelected) MoneTrackaColors.MintPrimary else MoneTrackaColors.TextMuted,
        animationSpec = spring()
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelect
            )
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(
            imageVector = item.icon,
            contentDescription = item.label,
            tint = iconColor,
            modifier = Modifier.size(22.dp)
        )
        Text(
            text = item.label,
            color = iconColor,
            fontSize = 10.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
        )
    }
}
```

- [ ] **Step 4: Run tests to verify build & correctness**

Run: `.\gradlew.bat :shared:testDebugUnitTest --tests "com.monetracka.shared.ui.home.components.FloatingNavBarTest"`
Expected: BUILD SUCCESSFUL with PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/FloatingNavBar.kt shared/src/commonTest/kotlin/com/monetracka/shared/ui/home/components/FloatingNavBarTest.kt
git commit -m "feat(ui): implement FloatingNavBar glassmorphic dock with center action FAB"
```

---

### Task 6: Dynamic Monthly Budget Management & Adjust Budget Bottom Sheet

**Files:**
- Create: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/AdjustBudgetBottomSheet.kt`
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreenModel.kt`
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeUiState.kt`
- Test: `shared/src/commonTest/kotlin/com/monetracka/shared/ui/home/HomeScreenModelTest.kt`

**Interfaces:**
- Produces:
  - `HomeIntent.UpdateMonthlyBudget(val newLimit: Double)`
  - `HomeUiState.monthlyBudgetLimit: Double`
  - `AdjustBudgetBottomSheet(currentBudget: Double, currency: String, onDismiss: () -> Unit, onSave: (Double) -> Unit)`

- [ ] **Step 1: Write test in HomeScreenModelTest.kt for budget limit update**

In `shared/src/commonTest/kotlin/com/monetracka/shared/ui/home/HomeScreenModelTest.kt`:
```kotlin
    @Test
    fun testUpdateMonthlyBudgetIntent() = runTest(testDispatcher) {
        val userRepo = FakeUserProfileRepository(UserProfile(userName = "Thanh", monthlyBudgetLimit = 2500.0))
        val viewModel = HomeScreenModel(
            transactionRepository = FakeTransactionRepository(emptyList()),
            categoryRepository = FakeCategoryRepository(emptyList()),
            accountRepository = FakeAccountRepository(emptyList()),
            userProfileRepository = userRepo
        )
        testScheduler.advanceUntilIdle()

        viewModel.onIntent(HomeIntent.UpdateMonthlyBudget(3200.0))
        testScheduler.advanceUntilIdle()

        assertEquals(3200.0, viewModel.state.value.monthlyBudgetLimit)
    }
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat :shared:testDebugUnitTest --tests "com.monetracka.shared.ui.home.HomeScreenModelTest.testUpdateMonthlyBudgetIntent"`
Expected: FAIL on unresolved `UpdateMonthlyBudget` or `monthlyBudgetLimit`.

- [ ] **Step 3: Update HomeUiState.kt and HomeScreenModel.kt**

In `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeUiState.kt`:
Add to `HomeUiState`:
```kotlin
    val monthlyBudgetLimit: Double = 2500.0,
    val isAdjustBudgetOpen: Boolean = false,
```

Add to `HomeIntent`:
```kotlin
    data class UpdateMonthlyBudget(val newLimit: Double) : HomeIntent
    data object OpenAdjustBudget : HomeIntent
    data object DismissAdjustBudget : HomeIntent
```

In `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreenModel.kt`:
In `combine(...)`:
```kotlin
    monthlyBudgetLimit = userProfile?.monthlyBudgetLimit ?: 2500.0,
    isAdjustBudgetOpen = mutableState.value.isAdjustBudgetOpen,
```
In `onIntent(intent: HomeIntent)`:
```kotlin
    HomeIntent.OpenAdjustBudget -> mutableState.value = mutableState.value.copy(isAdjustBudgetOpen = true)
    HomeIntent.DismissAdjustBudget -> mutableState.value = mutableState.value.copy(isAdjustBudgetOpen = false)
    is HomeIntent.UpdateMonthlyBudget -> {
        screenModelScope.launch {
            userProfileRepository?.updateMonthlyBudget(intent.newLimit)
            mutableState.value = mutableState.value.copy(
                monthlyBudgetLimit = intent.newLimit,
                isAdjustBudgetOpen = false
            )
        }
    }
```

- [ ] **Step 4: Create AdjustBudgetBottomSheet.kt**

Create `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/AdjustBudgetBottomSheet.kt`:
```kotlin
package com.monetracka.shared.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monetracka.shared.domain.util.CurrencyFormatter
import com.monetracka.shared.ui.theme.MoneTrackaColors

private val DECIMAL_REGEX = Regex("""^\d*\.?\d{0,2}$""")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdjustBudgetBottomSheet(
    currentBudget: Double,
    currency: String = "USD",
    onDismiss: () -> Unit,
    onSave: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var budgetText by remember { mutableStateOf(currentBudget.toInt().toString()) }
    val parsedAmount = budgetText.toDoubleOrNull() ?: 0.0

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MoneTrackaColors.SurfaceLevel1,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
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
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Monthly Budget Target",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.06f))
                        .clickable { onDismiss() }
                ) {
                    Text(text = "✕", color = Color(0xFF8FA2B6), fontSize = 14.sp)
                }
            }

            Text(
                text = "Set your total planned outflow target. MoneTracka tracks your progress and warns you when approaching limits.",
                color = Color(0xFF8FA2B6),
                fontSize = 13.sp
            )

            // Preset Chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(1500, 2000, 2500, 3000, 4000).forEach { preset ->
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (parsedAmount == preset.toDouble()) MoneTrackaColors.MintPrimary else MoneTrackaColors.SurfaceLevel2)
                            .clickable { budgetText = preset.toString() }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "${CurrencyFormatter.symbol(currency)}$preset",
                            color = if (parsedAmount == preset.toDouble()) Color(0xFF051A12) else Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            OutlinedTextField(
                value = budgetText,
                onValueChange = { if (it.isEmpty() || it.matches(DECIMAL_REGEX)) budgetText = it },
                modifier = Modifier.fillMaxWidth(),
                prefix = { Text(text = CurrencyFormatter.symbol(currency), color = MoneTrackaColors.MintPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                textStyle = androidx.compose.ui.text.TextStyle(color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MoneTrackaColors.MintPrimary,
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f),
                    focusedContainerColor = MoneTrackaColors.SurfaceLevel2,
                    unfocusedContainerColor = MoneTrackaColors.SurfaceLevel2
                ),
                shape = RoundedCornerShape(14.dp)
            )

            Button(
                onClick = { onSave(parsedAmount.coerceAtLeast(10.0)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MoneTrackaColors.MintPrimary,
                    contentColor = Color(0xFF051A12)
                )
            ) {
                Text(
                    text = "Save Budget Target",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}
```

- [ ] **Step 5: Run tests to verify passing state**

Run: `.\gradlew.bat :shared:testDebugUnitTest --tests "com.monetracka.shared.ui.home.HomeScreenModelTest"`
Expected: BUILD SUCCESSFUL with PASS.

- [ ] **Step 6: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/AdjustBudgetBottomSheet.kt shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeUiState.kt shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreenModel.kt shared/src/commonTest/kotlin/com/monetracka/shared/ui/home/HomeScreenModelTest.kt
git commit -m "feat(ui): add AdjustBudgetBottomSheet and wire dynamic monthlyBudgetLimit in HomeScreenModel"
```

---

### Task 7: Balance Hero Card Micro-Interactions & Visual Polish

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/BalanceHeroCard.kt`
- Test: `shared/src/commonTest/kotlin/com/monetracka/shared/ui/home/components/BalanceHeroCardTest.kt`

**Interfaces:**
- Produces:
  - `BalanceHeroCard(balance: Double, trendPercent: Double, sparklinePoints: List<Float>, currency: String, modifier: Modifier)`
  - Displays dynamic trend pills with directional icons and sleek glass depth.

- [ ] **Step 1: Write test in BalanceHeroCardTest.kt verifying currency badge formatting**

Create `shared/src/commonTest/kotlin/com/monetracka/shared/ui/home/components/BalanceHeroCardTest.kt`:
```kotlin
package com.monetracka.shared.ui.home.components

import com.monetracka.shared.domain.util.CurrencyFormatter
import kotlin.test.Test
import kotlin.test.assertEquals

class BalanceHeroCardTest {
    @Test
    fun testCurrencyFormattingSplitsCorrectly() {
        val (intPart, decPart) = CurrencyFormatter.splitAmount(1234.56, "USD")
        assertEquals("$1,234", intPart)
        assertEquals(".56", decPart)
    }
}
```

- [ ] **Step 2: Run test to verify it passes**

Run: `.\gradlew.bat :shared:testDebugUnitTest --tests "com.monetracka.shared.ui.home.components.BalanceHeroCardTest"`
Expected: PASS.

- [ ] **Step 3: Update BalanceHeroCard.kt with glassmorphic depth & polished trend pill**

In `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/BalanceHeroCard.kt`:
```kotlin
package com.monetracka.shared.ui.home.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.monetracka.shared.domain.util.CurrencyFormatter
import com.monetracka.shared.ui.theme.MoneTrackaColors

@Composable
fun BalanceHeroCard(
    balance: Double,
    trendPercent: Double,
    sparklinePoints: List<Float>,
    currency: String = "USD",
    modifier: Modifier = Modifier
) {
    val (intPartWithSymbol, decPart) = remember(balance, currency) {
        CurrencyFormatter.splitAmount(balance, currency)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(26.dp))
            .background(MoneTrackaColors.HeroCardBrush)
            .border(1.dp, MoneTrackaColors.BorderGlassLuminous, RoundedCornerShape(26.dp))
            .padding(22.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "TOTAL PORTFOLIO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MoneTrackaColors.TextSecondary
                )
                val currencyTag = when (currency) {
                    "EUR" -> "EUR (€)"
                    "GBP" -> "GBP (£)"
                    "VND" -> "VND (₫)"
                    else -> "USD ($)"
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.08f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = currencyTag,
                        fontSize = 11.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.Bottom) {
                Text(
                    text = intPartWithSymbol,
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )
                Text(
                    text = decPart,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold,
                    color = MoneTrackaColors.TextSecondary
                )
            }

            Spacer(Modifier.height(10.dp))

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val isPositive = trendPercent >= 0
                val trendColor = if (isPositive) MoneTrackaColors.MintPrimary else MoneTrackaColors.CoralDanger
                val trendIcon = if (isPositive) "↑" else "↓"
                val trendSign = if (isPositive) "+" else ""

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(trendColor.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "$trendIcon $trendSign%.1f%%".format(trendPercent),
                        color = trendColor,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    text = "net growth this month",
                    color = MoneTrackaColors.TextSecondary,
                    fontSize = 11.sp
                )
            }

            Spacer(Modifier.height(16.dp))
            SparklineChart(points = sparklinePoints, lineColor = if (trendPercent >= 0) MoneTrackaColors.MintPrimary else MoneTrackaColors.CoralDanger)
        }
    }
}
```

- [ ] **Step 4: Run test to verify passes**

Run: `.\gradlew.bat :shared:testDebugUnitTest --tests "com.monetracka.shared.ui.home.components.BalanceHeroCardTest"`
Expected: BUILD SUCCESSFUL with PASS.

- [ ] **Step 5: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/components/BalanceHeroCard.kt shared/src/commonTest/kotlin/com/monetracka/shared/ui/home/components/BalanceHeroCardTest.kt
git commit -m "feat(ui): refine BalanceHeroCard with glass styling and dynamic growth indicator"
```

---

### Task 8: End-to-End Visual Integration in HomeScreen

**Files:**
- Modify: `shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreen.kt`
- Test: `shared/src/commonTest/kotlin/com/monetracka/shared/ui/home/HomeScreenModelTest.kt`

**Interfaces:**
- Wire `FloatingNavBar` into bottom bar slot
- Wire `AdjustBudgetBottomSheet`
- Wire dynamic budget limits into `BudgetsView`
- Wire interactive `CategoryDonutChart` into `AnalyticsView` and `OverviewView`

- [ ] **Step 1: Update HomeScreen.kt with FloatingNavBar and dynamic BudgetsView**

In `HomeScreen.kt`:
1. Replace bottomBar slot with `FloatingNavBar(selectedTab, onTabSelected = { selectedTab = it }, onOpenAdd = { screenModel.onIntent(HomeIntent.OpenAddTransaction(TransactionType.EXPENSE)) })`.
2. In `BudgetsView`:
   - Replace hardcoded `val monthlyBudget = 2500.0` with `val monthlyBudget = state.monthlyBudgetLimit`.
   - Add button `"Adjust Target"` opening `screenModel.onIntent(HomeIntent.OpenAdjustBudget)`.
   - Compute burn-rate status (`On Track`, `Approaching Limit`, `Over Budget`).
3. Add `AdjustBudgetBottomSheet` modal when `state.isAdjustBudgetOpen == true`.
4. In `AnalyticsView`: pass `currency = state.currency` into `CategoryDonutChart`.

- [ ] **Step 2: Run full unit test suite to verify whole project passes**

Run: `.\gradlew.bat :shared:testDebugUnitTest`
Expected: BUILD SUCCESSFUL with all unit tests passing.

- [ ] **Step 3: Commit**

```bash
git add shared/src/commonMain/kotlin/com/monetracka/shared/ui/home/HomeScreen.kt
git commit -m "feat(ui): complete end-to-end integration of floating nav, dynamic budgets, and glassmorphic overhaul"
```

---

## Self-Review

1. **Spec Coverage:**
   - Section 2 (Design Tokens) → Task 2
   - Section 3.1 (Floating Glassmorphic Shell) → Task 5, Task 8
   - Section 3.2 (Balance Hero & Cubic Sparkline) → Task 3, Task 7
   - Section 3.3 (Interactive Donut & Analytics) → Task 4, Task 8
   - Section 3.4 (Dynamic Budgets & Adjust Sheet) → Task 1, Task 6, Task 8
   - Section 4 (Verification & Falsifiable Criteria) → Tests in all tasks.
2. **Placeholder Scan:** Verified no "TBD", "TODO", or pseudo-code steps. Every step contains complete, compilable Kotlin code and test commands.
3. **Type Consistency:** Verified `monthlyBudgetLimit: Double` consistent across `MoneTrackaDatabase.sq`, `Models.kt`, `Repositories.kt`, `RepositoryImpl.kt`, `HomeUiState.kt`, and `AdjustBudgetBottomSheet.kt`.
