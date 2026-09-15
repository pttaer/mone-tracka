# MoneTracka - Project Memory

## Overview
- **Branch**: `develop`
- **Tech Stack**: Kotlin Multiplatform (KMP), Compose Multiplatform, Voyager navigation, SQLDelight, Koin DI.
- **Recent Progress**:
  - `6f50008`: Single-pass O(N) aggregation in `HomeScreenModel` & onboarding thread safety (0% idle CPU, minimal RAM allocation).
  - `5c3aa26`: Added `monthlyBudgetLimit` to `UserProfile` entity, SQLDelight schema, and repository.
  - `45d2e1d`: Implemented glassmorphic OLED design tokens in `MoneTrackaColors`.
  - `fd1cd29`: Upgraded `SparklineChart` with smooth cubic Bezier splines and glow pulse.
  - `48eb9d1`: Interactive category donut chart with tap slice selection and center details.
  - `3c39693`: Implemented `FloatingNavBar` glassmorphic dock with center action FAB.
  - `bd1db12`: Dynamic monthly budget management and `AdjustBudgetBottomSheet`.
  - `e3e1ef4`: Refined `BalanceHeroCard` with glass depth, dynamic currency tags, and growth indicators.
  - `30119aa`: Complete end-to-end integration of floating nav, dynamic budgets, and glassmorphic overhaul in `HomeScreen`.
  - `12-month-roadmap`: Established comprehensive 12-month engineering superplan across 4 phases.
  - `execution-tracklist`: Created granular step-by-step checklist in `docs/superpowers/plans/execution-tracklist.md`.
  - `exchange-rate-engine`: Added `ExchangeRate` model, SQLDelight table & queries, repository layer, currency conversion helper, dynamic currency picker in Settings, and unit test suite.
  - `export-import-engine`: Added `SimpleCsvExporter` for roundtrip transaction export/import with sanitization, test suite, and SettingsView integration.
  - `category-budget-engine`: Added `CategoryBudget` model, `CategoryBudgetEntity` SQLDelight schema with cascade deletion, `CategoryBudgetRepository`, DI binding, and unit tests.
  - `recurring-transactions`: Added `RecurringTransactionEntity`, `RecurringTransactionRepository`, auto-posting check on app launch, interval selectors, and quick-add toggle.
  - `deep-analytics-networth`: Added comparative MoM flow visual, net savings rate calculation, and Net Worth timeline sparkline in `AnalyticsView`.
  - `security-and-auth`: Added `BiometricAuthManager` multiplatform expect/actual engine and UI toggle in `SettingsView`.
  - `ui-ux-pro-max`: Migrated to bright fintech design system (Wallet by BudgetBakers aesthetic); eliminated all emoji chrome in favor of vector icons; upgraded touch targets to 48dp+ with tactile ripple feedback; elevated Overview, Analytics, Budgets, and Settings cards; updated TransactionListScreen with search clear and segmented pills; verified 100% test pass.
  - `ponytail-simplifications`: Purged dead OLED color tokens from `Color.kt`, compressed `SimpleCsvExporter` to 29 lines using stdlib `buildString` and `lineSequence`, and simplified `QuoteRepositoryImpl` with compact expressions; verified with 100% test pass.


## Key Rules & Architectural Guardrails
- Performance: Single-pass O(N) data transformations; avoid nested traversals or re-sorting lists.
- Compose Color handling: Use `colorIndex: Int` instead of ULong bit shifting.
- Coroutine safety: Wrap SQLite blocking queries in `withContext(Dispatchers.IO)`.
- Never run `dotnet build`. Run `.\gradlew.bat :shared:testDebugUnitTest` for verification.
- Private fields `m_TitleCase`, public `TitleCase`.
