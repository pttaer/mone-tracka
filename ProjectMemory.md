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
  - `ui-ux-critique-overhaul`: Resolved 7 core user-friction points identified by adversarial review: (1) added destructive deletion confirmation dialog in `TransactionFeed`; (2) replaced dead/no-op stubs with interactive CSV Export (preview + copy), CSV Import (parse + validation), active Notification Center with unread badge clearing, and OCR scanner preview; (3) eliminated currency amnesia in `TransactionListScreen` and `AddTransactionBottomSheet` by injecting UserProfileRepository/AccountRepository and dynamic symbols; (4) added `navigationBarsPadding()` to `FloatingNavBar`; (5) converted glyph strings to Material vector icons in `QuickActionBar`; (6) added tap-to-view/transfer dialog to `AccountCard` removing long-press dependency; (7) added currency-aware quick increment chips with integer cent rounding; verified with 100% test pass.
  - `play-store-100k-critic-overhaul`: Resolved 7 critical Play Store rating hazards: (1) Added interactive account selector chip strip in `AddTransactionBottomSheet`, routing transactions to user-chosen accounts instead of hardcoded Account 1; (2) Added discrete eye toggle in `BalanceHeroCard` masking balance to `••••••` for public privacy; (3) Dynamic currency symbols via `CurrencyFormatter.symbol(currency)`; (4) Cleaned up `TransactionRow`, replaced inline "X" button clutter with rich `TransactionDetailBottomSheet` displaying full metadata and safe deletion; (5) Wired real CSV batch import in `SettingsView` inserting parsed transactions directly to repository; (6) Enhanced `TransactionListScreen` search to match notes, categories, and account names, added Transfer tab filter, and created illustrated empty state; (7) Wired interactive category budget adjustments in `BudgetsView` with custom limit dialogs. Verified with 100% test pass.
  - `play-store-100k-critic-overhaul-phase3`: Applied full suite of high-impact Play Store UI/UX fixes: (1) Seamless privacy masking across `BalanceHeroCard` and `AccountStrip` (`••••••` masking); (2) Input ergonomics in `AddTransactionBottomSheet` with auto-focus, IME Next/Done progression, decimal sanitization, and haptic feedback; (3) Advanced transaction management in `TransactionListScreen` with 4-way sorting (Newest, Oldest, Highest, Lowest), horizontal Category filter chips, and running totals summary card (In/Out/Net); (4) Proactive category budget warnings at 80% threshold with warning color indicators and percentage tracking. Verified with 100% test pass.
  - `biometric-app-lock`: End-to-end fintech biometric security: (1) Added `isBiometricEnabled` to `UserProfileEntity` SQLDelight schema, `UserProfile` model, and `UserProfileRepository` with `Dispatchers.IO` safety; (2) Registered `BiometricAuthManager` in Android/iOS platform DI; (3) Created dedicated `LockScreen` with pulsating biometric shield, user avatar initials, and tactile unlock button; (4) Wired cold-start routing in `RootScreen`; (5) Guarded Settings biometric deactivation and CSV export with authentication prompts; (6) Added interactive biometric lock screen overlay in `preview.html`. Verified 100% test pass.
  - `custom-categories`: Added user-defined custom categories: (1) Added `HomeIntent.CreateCategory` to `HomeUiState` and wired into `HomeScreenModel` with `CategoryRepository.insertCategory`; (2) Added `+ Custom` action chip and `AlertDialog` in `AddTransactionBottomSheet` supporting custom emojis, category names, and auto-selection; (3) Added interactive `+ Custom` modal and category management to `preview.html`; (4) Added `testCreateCustomCategory` unit test in `HomeScreenModelTest`. Verified 100% test pass.



## Key Rules & Architectural Guardrails
- Performance: Single-pass O(N) data transformations; avoid nested traversals or re-sorting lists.
- Compose Color handling: Use `colorIndex: Int` instead of ULong bit shifting.
- Coroutine safety: Wrap SQLite blocking queries in `withContext(Dispatchers.IO)`.
- Never run `dotnet build`. Run `.\gradlew.bat :shared:testDebugUnitTest` for verification.
- Private fields `m_TitleCase`, public `TitleCase`.
