# Design: Multi-Account Management & Tactile Drag-and-Drop Transfers

## 1. Problem & Objectives

Currently, MoneTracka assumes a single global pool for all transactions. This creates two critical accounting gaps:
1. **Starting Balances**: Users cannot record existing bank or cash balances without artificially inflating monthly income.
2. **Internal Transfers**: Withdrawing ATM cash or moving money between checking and savings registers as an expense or requires fake dual entries, distorting monthly cash flow.

### Objectives
- Support dynamic, user-defined accounts (Cash, Checking, Savings, etc.) with initial starting balances.
- Support a first-class `TRANSFER` transaction type that moves money between accounts with net-zero effect on total portfolio wealth.
- Provide a tactile, gesture-first interaction: drag-and-drop one account card onto another to open a spring transfer modal with a "Slide to Transfer" confirmation slider.

---

## 2. Architecture & Data Layer

### 2.1 Database Schema (`MoneTrackaDatabase.sq`)

#### New Table: `AccountEntity`
```sql
CREATE TABLE IF NOT EXISTS AccountEntity (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    name TEXT NOT NULL,
    emoji TEXT NOT NULL,
    initialBalance REAL NOT NULL DEFAULT 0.0,
    description TEXT NOT NULL DEFAULT ''
);

getAllAccounts:
SELECT * FROM AccountEntity ORDER BY id ASC;

insertAccount:
INSERT INTO AccountEntity(name, emoji, initialBalance, description)
VALUES (?, ?, ?, ?);

deleteAccount:
DELETE FROM AccountEntity WHERE id = ?;
```

#### Updated Table: `TransactionEntity`
Add source `accountId` and destination `toAccountId`:
```sql
CREATE TABLE IF NOT EXISTS TransactionEntity (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    amount REAL NOT NULL,
    type TEXT NOT NULL, -- 'INCOME', 'EXPENSE', 'TRANSFER'
    categoryId INTEGER NOT NULL,
    accountId INTEGER NOT NULL DEFAULT 1,
    toAccountId INTEGER, -- NULL for INCOME/EXPENSE, destination account ID for TRANSFER
    note TEXT NOT NULL DEFAULT '',
    dateMillis INTEGER NOT NULL,
    createdAtMillis INTEGER NOT NULL,
    FOREIGN KEY (categoryId) REFERENCES CategoryEntity(id) ON DELETE CASCADE,
    FOREIGN KEY (accountId) REFERENCES AccountEntity(id) ON DELETE CASCADE
);
```

### 2.2 Domain Models (`Models.kt`)

```kotlin
data class Account(
    val id: Long = 0,
    val name: String,
    val emoji: String,
    val initialBalance: Double = 0.0,
    val description: String = ""
)

data class Transaction(
    val id: Long = 0,
    val amount: Double,
    val type: TransactionType,
    val categoryId: Long,
    val accountId: Long = 1L,
    val toAccountId: Long? = null,
    val note: String = "",
    val dateMillis: Long,
    val createdAtMillis: Long = 0,
)

enum class TransactionType {
    INCOME,
    EXPENSE,
    TRANSFER
}
```

### 2.3 Mathematical Aggregation (`HomeScreenModel`)

For each account:
$$\text{Inflows} = \sum_{\text{type=INCOME, accountId}=A} \text{amount} + \sum_{\text{type=TRANSFER, toAccountId}=A} \text{amount}$$
$$\text{Outflows} = \sum_{\text{type=EXPENSE, accountId}=A} \text{amount} + \sum_{\text{type=TRANSFER, accountId}=A} \text{amount}$$
$$\text{Account Balance} = \text{initialBalance} + \text{Inflows} - \text{Outflows}$$

Total Net Portfolio:
$$\text{Total Net Wealth} = \sum_{A} \text{Account Balance}_A$$

Since every `TRANSFER` of amount $X$ adds $X$ to `toAccountId` and subtracts $X$ from `accountId`, the sum of transfer deltas across all accounts is identically zero.

---

## 3. User Interface & Interaction Specifications

### 3.1 Accounts Strip (`HomeScreen`)
- Placed directly under the Net Portfolio hero card.
- Displays horizontal list of account cards:
  - Account icon/emoji + name + computed balance.
  - "+ Add" button at end to create custom accounts.
- **Drag & Drop Gesture**:
  - Long press on an account card (e.g. Bank) lifts the card into a dragging state (slight tilt, elevated shadow, glowing border).
  - Dragging over a target account card (e.g. Cash) magnetically highlights the target with a cyan border and haptic tick.
  - Releasing over the target opens the **Spring Transfer Modal**.

### 3.2 Spring Transfer Modal (`TransferBottomSheet`)
- **Header**: Shows source and target accounts: `FROM 🏦 Bank ($2,450)` ➔ `TO 💵 Cash ($120)`.
- **Amount Section**:
  - Formatted amount display.
  - Fast preset chips: `[$20]`, `[$50]`, `[$100]`, `[All]`.
  - Live projected balance preview:
    - Source: `$2,450` ➔ `$2,400 (-$50)`
    - Target: `$120` ➔ `$170 (+$50)`
- **Slide to Transfer Slider**:
  - Horizontal track with drag thumb `➔`.
  - Swiping the thumb to the end executes the transfer, dismisses the sheet, and plays a success haptic.

### 3.3 Create Account Sheet (`AddAccountBottomSheet`)
- Fields:
  - Emoji selector (🏦, 💵, 💳, 🪙, 🎯, etc.)
  - Name (required, e.g. "Main Checking")
  - Starting Balance (default: $0.00)
  - Description / Note (optional)
- "Create Account" button inserts account into database and updates the accounts row.

### 3.4 Transaction Feed Display
- Normal income shows `+` with green amount.
- Normal expense shows `-` with red/coral amount.
- Transfer shows:
  - Icon: `🔁`
  - Title: Note if present, otherwise `${sourceAccount.name} ➔ ${targetAccount.name}`
  - Amount: Neutral color (`0xFFD1DBE6`), no negative or positive sign.

---

## 4. Edge Cases & Error Handling

1. **Transfer to Same Account**: Target account cannot be equal to source account; drop onto self does nothing.
2. **Transfer Exceeding Balance**: If transfer amount exceeds source account balance, show warning text in amber but allow overdraft if intentional.
3. **Empty Account List on First Launch**: Seed default accounts on first app run (`🏦 Checking` with $0 and `💵 Cash` with $0).
4. **Deleting Account**: If an account is deleted, associated transactions cascade or prompt confirmation.

---

## 5. Verification & Testing Strategy

- **Unit Tests (`commonTest`)**:
  - `AccountBalanceCalculationTest`: Verify initial balance + inflows - outflows across income, expense, and transfer.
  - `TransferNetZeroTest`: Verify transfers do not alter total net wealth or monthly expense trend.
- **ADB Physical Device Verification**:
  - Verify dragging Bank card over Cash card triggers transfer modal.
  - Verify "Slide to Transfer" confirms transaction and updates both account balances and transaction feed.
