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

## Key Rules & Architectural Guardrails
- Performance: Single-pass O(N) data transformations; avoid nested traversals or re-sorting lists.
- Compose Color handling: Use `colorIndex: Int` instead of ULong bit shifting.
- Coroutine safety: Wrap SQLite blocking queries in `withContext(Dispatchers.IO)`.
- Never run `dotnet build`. Run `.\gradlew.bat :shared:testDebugUnitTest` for verification.
- Private fields `m_TitleCase`, public `TitleCase`.
