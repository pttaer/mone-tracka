# MoneTracka 12-Month Development Superplan (Roadmap)

## Context & Baseline
- **Tech Stack**: Kotlin Multiplatform (KMP), Compose Multiplatform, SQLDelight, Voyager, Koin.
- **Current State**: MVP + Polish completed (Home, Analytics donut/sparkline, floating nav dock, dynamic budgets, multi-account transfers).
- **Core Guardrails**: O(N) single-pass transforms, zero JVM-only leaks in commonMain, Compose color index safety, SQLite IO dispatcher isolation.

---

## Phase 1: Q1 (Months 1–3) — Data Integrity, Multi-Currency & Full Export/Import
> **Theme: Fortress Foundations & User Freedom**

### 1. Multi-Currency Engine & FX Rates
- [ ] Schema: `ExchangeRateEntity(fromCurrency, toCurrency, rate, updatedAtMillis)`.
- [ ] Offline-first FX rate caching with background refresh worker.
- [ ] Real-time cross-currency account balances and transaction conversions.

### 2. Full Data Export / Import & Encrypted Local Backups
- [ ] JSON & CSV export/import engine (Transactions, Categories, Accounts, Budgets).
- [ ] Password-protected AES-256 encrypted SQLite backup file generation.
- [ ] File picker integrations (Android SAF / iOS document picker) via expect/actual.

### 3. Category Budgets & Rollovers
- [ ] Per-category dynamic monthly budgets (`CategoryBudgetEntity`).
- [ ] Optional monthly budget rollover (surplus/deficit rolling into next month).
- [ ] Progress bars and burn-rate warnings on Category detail cards.

---

## Phase 2: Q2 (Months 4–6) — Recurring Operations, Search & Deep Analytics
> **Theme: Automation & Granular Insights**

### 1. Recurring Transactions & Subscriptions Engine
- [ ] Schema: `RecurringTransactionEntity(frequency, interval, nextDueDateMillis, autoPost)`.
- [ ] Background notification scheduler and upcoming bill reminders.
- [ ] Subscription tracking tab with renewal alerts and annual cost projections.

### 2. Global Search & Advanced Multi-Filter
- [ ] Full-Text Search (FTS5 / SQLite indexed queries) across notes, merchants, tags.
- [ ] Filter by date range, account, type, category, and amount thresholds.
- [ ] Custom tags support (`TagEntity`, `TransactionTagJoinEntity`).

### 3. Extended Analytics & Comparative Reporting
- [ ] Month-over-month (MoM) and Year-over-year (YoY) comparative financial heatmaps.
- [ ] Cash flow trajectory projections (predictive balance based on recurring trends).
- [ ] Interactive multi-axis breakdown charts using Canvas rendering.

---

## Phase 3: Q3 (Months 7–9) — Biometrics, Cloud Sync & Receipt OCR
> **Theme: Security, Cloud Continuity & Frictionless Capture**

### 1. Biometric Lock & App Privacy
- [ ] Expect/actual BiometricPrompt (Android BiometricManager / iOS LocalAuthentication).
- [ ] Auto-lock on app backgrounding with customizable timeout (immediate, 1m, 5m).
- [ ] Privacy veil (blur screen preview in OS recent apps task switcher).

### 2. End-to-End Encrypted Cloud Sync (Optional / Zero-Knowledge)
- [ ] Client-side encrypted delta sync via WebSockets/REST (CRDT or vector-clock conflict resolution).
- [ ] Multi-device sync without storing plaintext data on server.
- [ ] Offline queue with automatic retry and merge reconciliation.

### 3. Smart Receipt Scanner (On-Device OCR)
- [ ] Camera capture sheet + on-device text recognition (ML Kit / Vision framework).
- [ ] Regex parser for total amount, date, and merchant name extraction.
- [ ] Auto-match to nearest category.

---

## Phase 4: Q4 (Months 10–12) — Goals, Net Worth & Platform Expansion
> **Theme: Wealth Strategy & Ecosystem**

### 1. Savings Goals & Sinking Funds
- [ ] Schema: `SavingsGoalEntity(targetAmount, currentAmount, deadlineMillis, targetAccountId)`.
- [ ] Auto-allocation rules from incoming transactions or manual transfers.
- [ ] Visual progress milestones and milestone celebration micro-animations.

### 2. Net Worth & Asset / Liability Tracking
- [ ] Support for investment accounts, loans, mortgages, and fixed assets.
- [ ] Real-time Net Worth calculation and historical asset/debt chart.

### 3. Desktop (macOS / Windows) & Tablet Adaptive Layouts
- [ ] Two-pane navigation drawer and responsive wide-screen dashboard for Compose Multiplatform Desktop / Tablet.
- [ ] Keyboard shortcuts for rapid transaction logging.
- [ ] System tray quick-add widget.

---

## Verification & Guardrail Pipeline
1. `.\gradlew.bat :shared:testDebugUnitTest` mandatory after each task.
2. Zero memory leaks in ScreenModels; coroutine cancel on lifecycle transitions.
3. Strict offline capability for 100% of core tracking flows.
