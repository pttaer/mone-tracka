# MoneTracka Failures & Bug Fixes Log

## [2026-09-08] Post-MVP Code Review & Physical Device Testing
1. **Compose ULong Color Bit-Shift Corruption**:
   - *Cause*: Attempting to convert `Color.value` to Long via `(catColor.value.toLong() shr 32) or (catColor.value.toLong() shl 32)` swapped high/low 32-bit words, corrupting color space and alpha, causing invisible arcs and dots.
   - *Fix*: Pass `colorIndex: Int` directly in `CategorySpend` and resolve with `CategoryColors.getOrElse(item.colorIndex)`.
2. **Category Filter & Selection Broken in Quick Add Sheet**:
   - *Cause*: Hardcoded `categories.take(4)` only rendered first 4 default categories (all Expense). Switching to Income showed 0 income categories, making Income logging impossible.
   - *Fix*: Filter categories by `selectedType`, auto-select valid category on type change, and render full list in `LazyRow`.
3. **Dead Bottom Navigation & Dead Action Buttons**:
   - *Cause*: NavigationBar items ("Analytics", "Budgets", "Settings"), "Report →", and "See All" had empty click handlers.
   - *Fix*: Implement complete tab views (`OverviewView`, `AnalyticsView`, `BudgetsView`, `SettingsView`), wire `selectedTab` state, and create `TransactionListScreen`.
4. **Coroutine Leak in AddTransactionScreenModel**:
   - *Cause*: `loadCategories()` called on every type change launched a new unmanaged coroutine on `screenModelScope`, creating multiple active collectors.
   - *Fix*: Manage single `categoriesJob?.cancel()` before launching new collection.
5. **Hardcoded Metric and Format Glitch**:
   - *Cause*: `monthlyTrendPercent` hardcoded to 18.4, and `+%.1f%%` produced `+-5.0%` for negative values.
   - *Fix*: Dynamically calculate savings/trend percentage, apply signed prefix and dynamic green/red color.
