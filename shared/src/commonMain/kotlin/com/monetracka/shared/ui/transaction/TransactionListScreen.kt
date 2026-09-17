package com.monetracka.shared.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.monetracka.shared.ui.home.components.TransactionDetailBottomSheet
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.domain.util.CurrencyFormatter
import com.monetracka.shared.ui.home.components.TransactionRow
import com.monetracka.shared.ui.theme.MoneTrackaColors

enum class TransactionSortOption(val label: String) {
    DATE_DESC("Newest First"),
    DATE_ASC("Oldest First"),
    AMOUNT_DESC("Highest Amount"),
    AMOUNT_ASC("Lowest Amount")
}

class TransactionListScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = getScreenModel<TransactionListScreenModel>()
        val state by screenModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        var filterType by remember { mutableStateOf<TransactionType?>(null) }
        var selectedCategoryId by remember { mutableStateOf<Long?>(null) }
        var sortOption by remember { mutableStateOf(TransactionSortOption.DATE_DESC) }
        var showSortDialog by remember { mutableStateOf(false) }
        var searchQuery by remember { mutableStateOf("") }
        var selectedTxForDetail by remember { mutableStateOf<Transaction?>(null) }

        val filteredTransactions = remember(
            state.transactions,
            state.categories,
            state.accounts,
            filterType,
            selectedCategoryId,
            sortOption,
            searchQuery
        ) {
            state.transactions
                .filter { tx ->
                    val matchesType = filterType == null || tx.type == filterType
                    val matchesCat = selectedCategoryId == null || tx.categoryId == selectedCategoryId
                    val catName = state.categories[tx.categoryId]?.name ?: ""
                    val accName = state.accounts[tx.accountId]?.name ?: ""
                    val matchesQuery = searchQuery.isBlank() ||
                        tx.note.contains(searchQuery, ignoreCase = true) ||
                        catName.contains(searchQuery, ignoreCase = true) ||
                        accName.contains(searchQuery, ignoreCase = true)
                    matchesType && matchesCat && matchesQuery
                }
                .sortedWith { a, b ->
                    when (sortOption) {
                        TransactionSortOption.DATE_DESC -> b.dateMillis.compareTo(a.dateMillis)
                        TransactionSortOption.DATE_ASC -> a.dateMillis.compareTo(b.dateMillis)
                        TransactionSortOption.AMOUNT_DESC -> b.amount.compareTo(a.amount)
                        TransactionSortOption.AMOUNT_ASC -> a.amount.compareTo(b.amount)
                    }
                }
        }

        val totalIncome = remember(filteredTransactions) {
            filteredTransactions.filter { it.type == TransactionType.INCOME }.sumOf { it.amount }
        }
        val totalExpense = remember(filteredTransactions) {
            filteredTransactions.filter { it.type == TransactionType.EXPENSE }.sumOf { it.amount }
        }
        val netDiff = remember(totalIncome, totalExpense) { totalIncome - totalExpense }

        TransactionDetailBottomSheet(
            isOpen = selectedTxForDetail != null,
            tx = selectedTxForDetail,
            category = selectedTxForDetail?.let { state.categories[it.categoryId] },
            accounts = state.accounts,
            categories = state.categories.values.toList(),
            currency = state.currency,
            onDismiss = { selectedTxForDetail = null },
            onDelete = {
                screenModel.deleteTransaction(it)
                selectedTxForDetail = null
            },
            onUpdate = {
                screenModel.updateTransaction(it)
                selectedTxForDetail = null
            }
        )

        if (showSortDialog) {
            AlertDialog(
                onDismissRequest = { showSortDialog = false },
                title = {
                    Text("Sort Transactions", color = MoneTrackaColors.TextDark, fontWeight = FontWeight.Bold, fontSize = 18.sp)
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        TransactionSortOption.values().forEach { opt ->
                            val isSel = sortOption == opt
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSel) MoneTrackaColors.MintLight else MoneTrackaColors.SurfaceSecondary)
                                    .clickable {
                                        sortOption = opt
                                        showSortDialog = false
                                    }
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = opt.label,
                                    color = if (isSel) MoneTrackaColors.MintDark else MoneTrackaColors.TextDark,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    fontSize = 14.sp
                                )
                                if (isSel) {
                                    Text("✓", color = MoneTrackaColors.MintDark, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { showSortDialog = false }) {
                        Text("Cancel", color = MoneTrackaColors.TextDark, fontWeight = FontWeight.SemiBold)
                    }
                },
                containerColor = MoneTrackaColors.CardWhite
            )
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "All Transactions",
                            color = MoneTrackaColors.TextDark,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigator.pop() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = MoneTrackaColors.TextDark
                            )
                        }
                    },
                    actions = {
                        Box(
                            modifier = Modifier
                                .padding(end = 12.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(MoneTrackaColors.SurfaceSecondary)
                                .clickable { showSortDialog = true }
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = "Sort",
                                    tint = MoneTrackaColors.MintDark,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = sortOption.label.split(" ").first(),
                                    color = MoneTrackaColors.MintDark,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = MoneTrackaColors.CardWhite)
                )
            },
            containerColor = MoneTrackaColors.BackgroundLight
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(horizontal = 18.dp)
            ) {
                // Search Input Field with Clear Action
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search note, category, account...", color = MoneTrackaColors.TextLight, fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = "Search", tint = MoneTrackaColors.TextGray, modifier = Modifier.size(20.dp))
                    },
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = MoneTrackaColors.TextGray, modifier = Modifier.size(18.dp))
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = MoneTrackaColors.TextDark,
                        unfocusedTextColor = MoneTrackaColors.TextDark,
                        focusedBorderColor = MoneTrackaColors.MintPrimary,
                        unfocusedBorderColor = MoneTrackaColors.ProgressTrack,
                        focusedContainerColor = MoneTrackaColors.CardWhite,
                        unfocusedContainerColor = MoneTrackaColors.CardWhite
                    ),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 4.dp)
                )

                // Segmented Filter Tabs: All, Expense, Income, Transfer
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp, bottom = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MoneTrackaColors.SurfaceSecondary)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val filters = listOf(
                        null to "All (${state.transactions.size})",
                        TransactionType.EXPENSE to "Expense",
                        TransactionType.INCOME to "Income",
                        TransactionType.TRANSFER to "Transfer"
                    )

                    filters.forEach { (type, label) ->
                        val isSelected = filterType == type
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .weight(1f)
                                .then(
                                    if (isSelected) {
                                        Modifier.shadow(2.dp, RoundedCornerShape(10.dp), spotColor = MoneTrackaColors.CardShadowColor)
                                    } else Modifier
                                )
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isSelected) MoneTrackaColors.CardWhite else Color.Transparent)
                                .clickable { filterType = type }
                                .padding(vertical = 8.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) MoneTrackaColors.MintDark else MoneTrackaColors.TextGray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Horizontal Category Filter Strip
                if (state.categories.isNotEmpty()) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
                    ) {
                        item(key = "all_cat") {
                            val isAllSel = selectedCategoryId == null
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isAllSel) MoneTrackaColors.MintLight else MoneTrackaColors.SurfaceSecondary)
                                    .border(1.dp, if (isAllSel) MoneTrackaColors.MintPrimary else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable { selectedCategoryId = null }
                                    .padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "All Categories",
                                    fontSize = 11.sp,
                                    color = if (isAllSel) MoneTrackaColors.MintDark else MoneTrackaColors.TextGray,
                                    fontWeight = if (isAllSel) FontWeight.Bold else FontWeight.Medium
                                )
                            }
                        }

                        items(state.categories.values.toList(), key = { it.id }) { cat ->
                            val isCatSel = selectedCategoryId == cat.id
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isCatSel) MoneTrackaColors.MintLight else MoneTrackaColors.SurfaceSecondary)
                                    .border(1.dp, if (isCatSel) MoneTrackaColors.MintPrimary else Color.Transparent, RoundedCornerShape(8.dp))
                                    .clickable {
                                        selectedCategoryId = if (isCatSel) null else cat.id
                                    }
                                    .padding(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(text = cat.emoji, fontSize = 12.sp)
                                    Text(
                                        text = cat.name,
                                        fontSize = 11.sp,
                                        color = if (isCatSel) MoneTrackaColors.MintDark else MoneTrackaColors.TextDark,
                                        fontWeight = if (isCatSel) FontWeight.Bold else FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Running Totals Summary Card
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(MoneTrackaColors.CardWhite)
                        .border(1.dp, MoneTrackaColors.ProgressTrack, RoundedCornerShape(12.dp))
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Total In: +${CurrencyFormatter.format(totalIncome, state.currency)}",
                        color = MoneTrackaColors.MintDark,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Total Out: -${CurrencyFormatter.format(totalExpense, state.currency)}",
                        color = MoneTrackaColors.CoralDanger,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Net: ${if (netDiff >= 0) "+" else ""}${CurrencyFormatter.format(netDiff, state.currency)}",
                        color = if (netDiff >= 0) MoneTrackaColors.MintDark else MoneTrackaColors.CoralDanger,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }

                Spacer(Modifier.height(8.dp))

                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MoneTrackaColors.MintPrimary)
                    }
                } else if (filteredTransactions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.padding(24.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(MoneTrackaColors.SurfaceSecondary)
                            ) {
                                Icon(
                                    imageVector = if (searchQuery.isNotBlank() || filterType != null) Icons.Default.Search else Icons.Default.Receipt,
                                    contentDescription = null,
                                    tint = MoneTrackaColors.TextGray,
                                    modifier = Modifier.size(28.dp)
                                )
                            }
                            Text(
                                text = if (searchQuery.isNotBlank() || filterType != null) "No matching transactions" else "No transactions recorded yet",
                                color = MoneTrackaColors.TextDark,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (searchQuery.isNotBlank() || filterType != null)
                                    "Try adjusting your search query or tab filters."
                                else
                                    "Your transactions will appear here once logged.",
                                color = MoneTrackaColors.TextGray,
                                fontSize = 13.sp,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            if (searchQuery.isNotBlank() || filterType != null) {
                                Spacer(Modifier.height(4.dp))
                                OutlinedButton(
                                    onClick = {
                                        searchQuery = ""
                                        filterType = null
                                    },
                                    shape = RoundedCornerShape(10.dp),
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MoneTrackaColors.MintDark)
                                ) {
                                    Text("Reset Filters", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 40.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredTransactions, key = { it.id }) { tx ->
                            TransactionRow(
                                tx = tx,
                                category = state.categories[tx.categoryId],
                                accounts = state.accounts,
                                currency = state.currency,
                                onClick = { selectedTxForDetail = tx },
                                onDelete = { screenModel.deleteTransaction(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
