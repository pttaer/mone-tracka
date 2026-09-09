# MoneTracka UI/UX Design Overhaul Specification

**Date:** 2026-09-09  
**Product Type:** Personal Finance Tracker / Fintech Mobile App  
**Design Theme:** Glassmorphism + Dark Mode (OLED)  
**Target Platform:** Kotlin Multiplatform (Compose Multiplatform Android & iOS)

---

## 1. Problem Statement & Executive Summary

MoneTracka's core multiplatform architecture (KMP, Compose Multiplatform, SQLDelight, Voyager, Koin) is robust and functional. However, the current visual and interaction design suffers from:
1. **Generic & Flat Aesthetics**: Solid dark rectangles without elevation, depth, or glassmorphic illumination.
2. **Text-Based Unicode Navigation**: Bottom navigation uses raw text characters (`⊞`, `📊`, `+`, `◎`, `⚙`) with default Material3 navigation bar styling instead of a bespoke floating glassmorphic shell.
3. **Rough Graphics & Static Charts**: Sparkline uses jagged straight segments without cubic smoothing; Donut Chart is a non-interactive 76dp circle without slice selection or center metrics.
4. **Hardcoded Budgets**: Monthly budget is hardcoded to $2,500.00 and category limits to $500.00 in UI components without user control or DB persistence.
5. **Lack of Tabular Micro-Interactions & Transitions**: Abrupt tab changes, static numbers that do not animate when values update, and bare text empty states without graphic guidance.

This overhaul transforms MoneTracka into a **state-of-the-art, high-end fintech experience** on par with modern apps like Revolut, CashApp, and Robinhood.

---

## 2. Design System & Visual Foundation

### 2.1 Color Palette (OLED Dark + Glassmorphic Accents)
- **Background Root**: `#060B11` (Deep OLED black)
- **Surface Tier 1 (Cards & Feed)**: `#0D1824` with 1dp border `Color.White.copy(alpha = 0.08f)`
- **Surface Tier 2 (Inner Containers & Inputs)**: `#132232` with border `Color.White.copy(alpha = 0.06f)`
- **Surface Elevated (Floating Nav & Dialogs)**: `#17283C` with 1dp luminous rim
- **Glassmorphism Overlay**: `Color(0xCC0D1824)` with subtle frosted sheen
- **Accents**:
  - **Mint Emerald (Primary/Success)**: `#00D09C` (gradients to `#00A87E`)
  - **Electric Cyan (Inflows/Active)**: `#00B2FF`
  - **Coral Red (Expenses/Alerts)**: `#FF5A79`
  - **Amber Gold (Warnings/Limits)**: `#FFB300`
  - **Violet Purple (Insights/Coaching)**: `#9D65FF`
- **Typography Colors**:
  - **Primary Text**: `Color(0xFFFFFFFF)` (100% white, 14:1+ contrast on root)
  - **Secondary / Subtitles**: `Color(0xFF8FA2B6)` (5.8:1 contrast)
  - **Muted / Hints**: `Color(0xFF54687F)` (4.5:1 contrast on card surfaces)

### 2.2 Corner Radii & Elevation Tokens
- **Pill / Circular**: `999.dp` (buttons, tags, thumb sliders)
- **Card Large**: `24.dp` (Balance Hero, Smart Coach, Analytics Cards)
- **Card Medium**: `16.dp` (Account cards, Transaction items, Budget progress rows)
- **Input & Small**: `12.dp` (text fields, action chips, dialog controls)

---

## 3. Core Screen & Component Specifications

### 3.1 Floating Glassmorphic App Shell & Navigation
- **Floating Island Bar**:
  - Replaces default Material3 `NavigationBar`.
  - Floating capsule elevated 16dp above system navigation bar with 20dp horizontal padding.
  - Height: 68dp, radius: 28dp, background: `Color(0xDD0D1824)` with 1dp border `Color.White.copy(alpha = 0.12f)`.
  - 4 Navigation destinations with Material Vector Icons:
    1. **Overview**: `Icons.Default.Dashboard`
    2. **Analytics**: `Icons.Default.PieChart`
    3. **Budgets**: `Icons.Default.AccountBalanceWallet`
    4. **Settings**: `Icons.Default.Settings`
  - Floating Center Action FAB:
    - Anchored in the center or floating above the dock.
    - Size: 52dp circle, gradient `#00D09C` to `#00A87E`, shadow elevation 8dp with glowing green ambient tint.
    - Icon: Bold `+` opening `AddTransactionBottomSheet` with smooth scale animation.

### 3.2 Balance Hero Card & Cubic Smooth Sparkline
- **Balance Presentation**:
  - Animated counting transition on portfolio total (`animateDoubleAsState` or formatted whole/cents transition).
  - Monthly trend badge: pill with dynamic background (green 16% alpha for positive, red for deficit) with trend percentage and directional arrow (`↑` / `↓`).
- **Cubic Bezier Sparkline Chart**:
  - Replaces jagged `lineTo` segments with smooth cubic Hermite splines (`cubicTo`).
  - Area gradient from `lineColor.copy(alpha = 0.40f)` fading to `Color.Transparent`.
  - Pulsing glowing terminal point with dual concentric rings indicating the most recent data point.

### 3.3 Interactive Hero Donut Chart & Category Insights
- **Donut Chart**:
  - Center metric displaying total spend or selected category details on tap.
  - Slices highlighted and enlarged on tap with active segment indicator.
  - Multi-color palette mapped cleanly through `CategoryColors`.
- **Category Spend Rows**:
  - Category icon badge with gradient background.
  - Dual-tone progress bar with rounded ends.
  - Percentage of total spend and formatted localized currency.

### 3.4 Dynamic Budgets Engine & Interactive Management
- **Budget Storage in Database**:
  - Add `monthlyBudgetLimit: Double` to `UserProfileEntity` (default $2,500.00).
  - Add `budgetLimit: Double?` support in categories.
- **Budgets View**:
  - Monthly spending gauge card with remaining days in month and daily burn rate allowance.
  - Warning tier states:
    - `< 70%`: Emerald Green (`On Track`)
    - `70% - 90%`: Amber Gold (`Careful`)
    - `> 90%`: Coral Red (`Near Limit` / `Over Budget`)
  - "Adjust Budget" button opening a bottom sheet to modify monthly target limit directly.

### 3.5 Accounts Strip & Tactile Transfers
- **Account Cards**:
  - Bank card inspired gradient backdrop with subtle gloss lines.
  - Clear balance display in user currency.
  - Tactile long-press drag with spring lift (`1.08f` scale, drop shadow with green glow).
  - Hover target highlight (`Drop here to transfer`) with active pulsating border.

### 3.6 Transaction Feed & List Polish
- **Row Styling**:
  - Clean card container per transaction or unified segmented card feed.
  - Type badges: Expense (`-`), Income (`+`), Transfer (`🔁`).
  - Account flow indicator for transfers: `Checking ➔ Cash`.
  - Swipe to delete with red background reveal and haptic confirmation.

### 3.7 Empty States & Onboarding
- **Empty States**:
  - Glowing circular icon backdrop with themed emoji or vector illustration.
  - Actionable prompt button ("Add your first transaction", "Set a monthly budget").
- **Onboarding Polish**:
  - Animated step progress indicator with glowing line segments.
  - Tactile numeric keypad or clean formatted input cards.

---

## 4. Verification & Falsifiable Criteria
1. **Automated Unit Tests**:
   - Verify `UserProfile` includes `monthlyBudgetLimit`.
   - Verify budget burn rate calculation helper.
   - Verify Bezier spline math produces valid finite coordinate bounds.
2. **Visual & Runtime Verification**:
   - Shared module compiles cleanly (`.\gradlew.bat :shared:assembleDebug`).
   - Android application compiles (`.\gradlew.bat :androidApp:assembleDebug`).
   - Zero crash on all tab switches, bottom sheet launches, and drag interactions.
