# MoneTracka Execution Tracklist (Step-by-Step)

Status: `[x]` Done | `[-]` In Progress | `[ ]` Pending

---

## Phase 1: Q1 (Data Integrity, Multi-Currency, Backups & Budgets)

### Sprint 1.1: Multi-Currency & FX Engine
- [x] **Step 1.1.1**: Define `ExchangeRate` model and unit tests (`ExchangeRateTest.kt`).
- [x] **Step 1.1.2**: Add `ExchangeRateEntity` table, queries, and `ExchangeRateRepositoryImpl` with Koin bindings.
- [x] **Step 1.1.3**: Implement offline-first FX seed defaults (`USD`, `EUR`, `GBP`, `VND`, `JPY`) on DB init.
- [x] **Step 1.1.4**: Add helper function `Double.convert(fromRate: Double, toRate: Double)` for cross-currency calculations.
- [x] **Step 1.1.5**: Wire multi-currency conversions into `HomeScreenModel` total net balance calculation.
- [x] **Step 1.1.6**: Add currency selector dialog in `SettingsView` with dynamic recalculation of displayed balances.


### Sprint 1.2: Export / Import & Backups
- [x] **Step 1.2.1**: Write CSV/JSON export serializer tests for transactions (`SimpleCsvExporterTest.kt`).
- [x] **Step 1.2.2**: Implement `SimpleCsvExporter` export & import parser with duplicate prevention and note sanitization.
- [ ] **Step 1.2.3**: Create platform file save/load integration (Android SAF / iOS document picker).
- [x] **Step 1.2.4**: Add "Backup & Export Data" and "Restore Data" buttons in `SettingsView`.

### Sprint 1.3: Granular Category Budgets & Rollover
- [x] **Step 1.3.1**: Add `CategoryBudgetEntity(categoryId, monthlyLimit, rolloverEnabled)` in SQLDelight.
- [x] **Step 1.3.2**: Implement `CategoryBudgetRepository` with Flow observation.
- [x] **Step 1.3.3**: Implement budget rollover calculation (previous month surplus/deficit applied to current target).
- [x] **Step 1.3.4**: Build category budget card with progress bar, burn velocity, and alert thresholds.
- [x] **Step 1.3.5**: Add category budget management bottom sheet to `BudgetsView`.


---

## Phase 2: Q2 (Recurring Transactions, Search & Deep Analytics)

### Sprint 2.1: Recurring Transactions & Subscriptions Engine
- [x] **Step 2.1.1**: Define `RecurringTransactionEntity(intervalType, intervalCount, nextDueDateMillis, autoPost)`.
- [x] **Step 2.1.2**: Implement auto-posting check on app launch for due recurring items.
- [x] **Step 2.1.3**: Add recurring toggle and frequency selector in `AddTransactionSheet`.
- [x] **Step 2.1.4**: Build "Subscriptions & Bills" tab showing upcoming due dates and monthly/annual burn cost.

### Sprint 2.2: Search & Filter
- [x] **Step 2.2.1**: Add text search query in SQLDelight using `LIKE '%query%'` over note and merchant.
- [x] **Step 2.2.2**: Add comma-separated tag column to `TransactionEntity`.
- [x] **Step 2.2.3**: Build top search bar in `TransactionListScreen` with live typing debounce (250ms).
- [x] **Step 2.2.4**: Create filter sheet (Date Range, Min/Max Amount, Account, Category, Tag).

### Sprint 2.3: Deep Analytics & Visualizations
- [x] **Step 2.3.1**: Add Month-over-Month (MoM) and Year-over-Year (YoY) delta calculation in `AnalyticsView`.
- [x] **Step 2.3.2**: Build multi-bar comparative Canvas chart for MoM income vs. expenses.
- [x] **Step 2.3.3**: Build predictive cash flow projection curve for the upcoming 30 days.

---

## Phase 3: Q3 (Biometrics, Cloud Backup & Receipt Attachment)

### Sprint 3.1: Biometric Authentication & Privacy Veil
- [x] **Step 3.1.1**: Create expect/actual `BiometricAuthManager` for Android BiometricPrompt and iOS LocalAuthentication.
- [x] **Step 3.1.2**: Implement app lifecycle observer triggering PIN/Biometric lock after configurable idle timeout.
- [x] **Step 3.1.3**: Add privacy veil (blur/obscure screen content when app moves to background app switcher).
- [x] **Step 3.1.4**: Add biometric unlock toggle and timeout settings in `SettingsView`.

### Sprint 3.2: Cloud Backup & Sync
- [x] **Step 3.2.1**: Implement cloud drive export/restore (Google Drive / iCloud document file sync).
- [x] **Step 3.2.2**: Add automatic backup schedule toggle in `SettingsView`.

### Sprint 3.3: Receipt Capture
- [x] **Step 3.3.1**: Add receipt photo attachment to transaction via platform image picker.
- [x] **Step 3.3.2**: Display attached receipt image thumbnail in transaction details.

---

## Phase 4: Q4 (Savings Goals, Net Worth & Desktop Expansion)

### Sprint 4.1: Savings Goals & Sinking Funds
- [x] **Step 4.1.1**: Define `SavingsGoalEntity(title, targetAmount, savedAmount, deadlineMillis, targetAccountId)`.
- [x] **Step 4.1.2**: Implement auto-deposit transfer logic from designated accounts.
- [x] **Step 4.1.3**: Build interactive goal rings and progress cards in `BudgetsView`.

### Sprint 4.2: Net Worth & Asset / Liability Ledger
- [x] **Step 4.2.1**: Support account classification (`ASSET`, `LIABILITY`, `INVESTMENT`, `CASH`).
- [x] **Step 4.2.2**: Compute real-time Net Worth (`Total Assets - Total Liabilities`).
- [x] **Step 4.2.3**: Build historical Net Worth timeline sparkline in `AnalyticsView`.

### Sprint 4.3: Multiplatform Desktop & Tablet Scaffold
- [x] **Step 4.3.1**: Implement adaptive two-pane navigation rail for screen widths > 840dp.
- [x] **Step 4.3.2**: Add desktop keyboard shortcuts (`Ctrl/Cmd + N` new transaction, `Ctrl/Cmd + F` search).
