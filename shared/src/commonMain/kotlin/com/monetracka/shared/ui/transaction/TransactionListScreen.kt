package com.monetracka.shared.ui.transaction

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.ui.home.components.TransactionRow
import com.monetracka.shared.ui.theme.MoneTrackaColors

class TransactionListScreen : Screen {

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun Content() {
        val screenModel = getScreenModel<TransactionListScreenModel>()
        val state by screenModel.uiState.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        var filterType by remember { mutableStateOf<TransactionType?>(null) }
        var searchQuery by remember { mutableStateOf("") }

        val filteredTransactions = remember(state.transactions, filterType, searchQuery) {
            state.transactions.filter { tx ->
                val matchesType = filterType == null || tx.type == filterType
                val matchesQuery = searchQuery.isBlank() || tx.note.contains(searchQuery, ignoreCase = true)
                matchesType && matchesQuery
            }
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
                    placeholder = { Text("Search by note...", color = MoneTrackaColors.TextLight, fontSize = 14.sp) },
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
                        .padding(top = 10.dp, bottom = 4.dp)
                )

                // Segmented Filter Tabs: All, Expense, Income
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 12.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(MoneTrackaColors.SurfaceSecondary)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val filters = listOf(
                        null to "All (${state.transactions.size})",
                        TransactionType.EXPENSE to "Expense",
                        TransactionType.INCOME to "Income"
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
                                .padding(vertical = 10.dp)
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) MoneTrackaColors.MintDark else MoneTrackaColors.TextGray,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                fontSize = 13.sp
                            )
                        }
                    }
                }

                if (state.isLoading) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MoneTrackaColors.MintPrimary)
                    }
                } else if (filteredTransactions.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = if (searchQuery.isNotBlank()) "No matching transactions found" else "No transactions recorded yet",
                                color = MoneTrackaColors.TextGray,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
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
                                onDelete = { screenModel.deleteTransaction(it) }
                            )
                        }
                    }
                }
            }
        }
    }
}
