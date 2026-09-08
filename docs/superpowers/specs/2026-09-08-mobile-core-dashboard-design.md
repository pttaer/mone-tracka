# Mobile Core Dashboard & Logging Flow Design Spec

## Overview
Port the validated high-fidelity mobile prototype (`mobile-design-v0.html`) into Compose Multiplatform for Android and iOS. This includes the complete home dashboard, interactive canvas-drawn financial data visualizations (balance sparkline & category donut), sticky transaction timeline, and quick expense logging bottom sheet.

## Architecture

### 1. UI Layer (Compose Multiplatform)
- **Navigation**: Voyager `Screen` (`HomeScreen.kt`) integrated into `App.kt` `Navigator`.
- **State Management**: `HomeScreenModel : StateScreenModel<HomeUiState>` using Voyager ScreenModel and Kotlin Coroutines `StateFlow`.
- **Theme & Design Tokens**: 
  - Canvas: `#060B11` / Phone Base: `#0C1622`
  - Elevated Surfaces: `#172535` with subtle border `rgba(255, 255, 255, 0.08)`
  - Primary Accent: `#00D09C` (Mint Neon) with glow effect
  - Expense Accent: `#FF5A79` (Coral)
  - Secondary Accents: `#4E95FF` (Sky), `#FFB347` (Amber)

### 2. Components Breakdown
- `UserHeaderComposable`: Monogram avatar with online status badge, total portfolio subtitle, account title, and notification icon button.
- `BalanceHeroCard`:
  - Main balance with tabular numeral formatting (large integer + smaller decimals).
  - Monthly percentage trend pill (`+18.4%`).
  - Jetpack Compose `Canvas` sparkline drawing smooth cubic Bézier curve with vertical gradient fill under the curve and pulsing current-value dot.
- `QuickActionBar`:
  - 4-item horizontal grid (`Add Expense` with mint gradient bubble, `Income`, `Scan Bill`, `More`).
  - Haptic feedback and spring touch interaction.
- `CategoryBreakdownCard`:
  - Compose `Canvas` Donut Chart with arc strokes, centered total spend text.
  - Multi-category progress bars with percentage and monetary breakdown.
- `TransactionFeed`:
  - Grouped by date dividers (`Today, Sep 8`, `Yesterday, Sep 7`).
  - `TransactionRow`: Category icon container with soft tinted background, merchant/note, payment method, amount with positive/negative color coding, and timestamp.
- `AddTransactionBottomSheet`:
  - Modal bottom sheet with large keypad/amount input, category chip selector, date picker trigger, and primary action button.
- `DockedBottomBar`:
  - Custom bottom bar with Overview, Analytics, elevated center Add button, Budgets, and Settings.

### 3. Data & Domain Layer
- **Entity**: `Transaction(id, amount, type, category, timestamp, note, currency)`
- **Category Summary**: `CategorySpend(category, totalAmount, percentage, colorHex)`
- **Repository**: `TransactionRepository` backed by SQLDelight database driver on Android and iOS.
- **State Definition**:
  ```kotlin
  data class HomeUiState(
      val netBalance: Double = 0.0,
      val monthlyTrendPercent: Double = 0.0,
      val monthlyTrendDiff: Double = 0.0,
      val sparklinePoints: List<Float> = emptyList(),
      val categorySpends: List<CategorySpend> = emptyList(),
      val recentTransactions: List<Transaction> = emptyList(),
      val isAddSheetOpen: Boolean = false,
      val isLoading: Boolean = false
  )
  ```
- **Intents**:
  ```kotlin
  sealed interface HomeIntent {
      data object OpenAddExpense : HomeIntent
      data object CloseAddExpense : HomeIntent
      data class SaveTransaction(val amount: Double, val type: TransactionType, val category: String, val note: String) : HomeIntent
      data class DeleteTransaction(val id: Long) : HomeIntent
  }
  ```

## Verification & Testing
- **Unit Tests**:
  - `HomeScreenModelTest`: Tests state mutations upon initial load and receiving intents.
  - Balance calculations: Verifies income minus expense math and percentage trends.
  - Category grouping: Verifies correct distribution percentages summing to 100%.
- **Manual Visual Acceptance**:
  - Android emulator verification for correct edge-to-edge layout, typography scaling, and smooth sparkline curve rendering.
