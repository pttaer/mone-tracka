# MoneTracka MVP Specification

## 1. Overview
MoneTracka is a minimal, dark-mode-first personal finance tracker built with Compose Multiplatform (Android & iOS), SQLDelight, Voyager, and Koin. This specification defines the Minimum Viable Product (MVP) requirements to deliver an end-to-end, functional, offline-first personal accounting experience.

## 2. Core MVP Functional Requirements

### FR-1: Default Category & Seed System
- When database initializes, if `CategoryEntity` is empty, automatically seed default categories:
  - **Expense**: Food & Dining (🍔), Transportation (🚗), Shopping (🛍️), Housing & Bills (🏠), Entertainment (🎮), Healthcare (💊)
  - **Income**: Salary (💼), Freelance & Investments (📈), Other Income (💰)
- Each category has assigned color tokens matching the design system (`#00D09C`, `#FF5A79`, `#4E95FF`, `#FFB347`, `#A78BFA`, `#F472B6`).

### FR-2: Full Transaction Lifecycle (CRUD)
- **Create**: Add transaction bottom sheet with:
  - Numeric amount input with active decimal formatting.
  - Expense / Income type toggle.
  - Interactive horizontal/grid category chip picker.
  - Note field (optional).
  - Date picker (defaulting to today).
- **Read**: Reactive `StateFlow` updates home dashboard, balance card, and transaction timeline immediately upon insertion.
- **Delete**: Support transaction deletion with instant balance and chart recalculation.

### FR-3: Category Budget Limits & Progress
- Extend `CategorySpend` model to include optional monthly budget limit (`budgetLimit: Double?`).
- Compute consumption ratio: `consumedRatio = totalSpend / budgetLimit`.
- Display status indicators: Normal (<80%), Warning (80-100%), Exceeded (>100%).

### FR-4: Multi-Currency & Number Formatting
- Store active currency code (USD `$`, EUR `€`, GBP `£`, VND `₫`).
- Centralize currency formatting helper `CurrencyFormatter.format(amount, currencyCode)`.

## 3. Architecture & Tech Stack
- **UI**: Compose Multiplatform with Material 3, custom Canvas charts, and Voyager Navigation.
- **State Management**: Voyager `ScreenModel` + Kotlin Coroutines `StateFlow` (unidirectional MVI).
- **Persistence**: SQLDelight SQLite driver (Android + iOS).
- **DI**: Koin.

## 4. Design Tokens & Visual Language
- Background Canvas: `#060B11`
- Surface Card: `#172535`
- Primary Neon Mint: `#00D09C`
- Negative Coral: `#FF5A79`
- Subtle Border: `rgba(255, 255, 255, 0.08)`
