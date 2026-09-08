package com.monetracka.shared.ui.onboarding

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.getScreenModel
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import com.monetracka.shared.domain.util.CurrencyFormatter
import com.monetracka.shared.ui.home.HomeScreen

class OnboardingScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel = getScreenModel<OnboardingScreenModel>()
        val state by screenModel.state.collectAsState()
        val navigator = LocalNavigator.currentOrThrow

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF060B11))
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(24.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Top Progress Indicator
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        for (i in 0..3) {
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .height(4.dp)
                                    .clip(RoundedCornerShape(999.dp))
                                    .background(
                                        if (i <= state.step) Color(0xFF00D09C)
                                        else Color.White.copy(alpha = 0.12f)
                                    )
                            )
                        }
                    }

                    if (state.step > 0) {
                        IconButton(
                            onClick = { screenModel.prevStep() },
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF172535))
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(36.dp))
                    }
                }

                // Step Content
                AnimatedContent(
                    targetState = state.step,
                    transitionSpec = { fadeIn() togetherWith fadeOut() },
                    modifier = Modifier.weight(1f, fill = false).padding(vertical = 24.dp)
                ) { step ->
                    when (step) {
                        0 -> StepIdentity(
                            userName = state.userName,
                            initials = state.userInitials,
                            onNameChange = { screenModel.onNameChanged(it) },
                            onNext = { if (state.userName.isNotBlank()) screenModel.nextStep() }
                        )
                        1 -> StepCurrency(
                            selectedCurrency = state.selectedCurrency,
                            onSelectCurrency = { screenModel.onCurrencySelected(it) }
                        )
                        2 -> StepOpeningBalances(
                            currency = state.selectedCurrency,
                            checkingBalance = state.checkingBalance,
                            cashBalance = state.cashBalance,
                            onCheckingChange = { screenModel.onCheckingBalanceChanged(it) },
                            onCashChange = { screenModel.onCashBalanceChanged(it) }
                        )
                        3 -> StepReview(
                            userName = state.userName,
                            initials = state.userInitials,
                            currency = state.selectedCurrency,
                            checkingBalance = state.checkingBalance.toDoubleOrNull() ?: 0.0,
                            cashBalance = state.cashBalance.toDoubleOrNull() ?: 0.0
                        )
                    }
                }

                // Bottom Action Buttons
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (state.step < 3) {
                        Button(
                            onClick = { screenModel.nextStep() },
                            enabled = state.step != 0 || state.userName.isNotBlank(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00D09C),
                                contentColor = Color(0xFF051A12),
                                disabledContainerColor = Color(0xFF172535),
                                disabledContentColor = Color(0xFF54687F)
                            )
                        ) {
                            Text(
                                text = "Continue ➔",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = {
                                screenModel.completeOnboarding {
                                    navigator.replaceAll(HomeScreen())
                                }
                            },
                            enabled = !state.isLoading,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF00D09C),
                                contentColor = Color(0xFF051A12)
                            )
                        ) {
                            if (state.isLoading) {
                                CircularProgressIndicator(
                                    color = Color(0xFF051A12),
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Text(
                                    text = "Get Started ➔",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun StepIdentity(
        userName: String,
        initials: String,
        onNameChange: (String) -> Unit,
        onNext: () -> Unit
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // Real-time Avatar Badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF00D09C), Color(0xFF009C75))
                        )
                    )
            ) {
                Text(
                    text = initials,
                    color = Color(0xFF051A12),
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 32.sp
                )
            }

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "Welcome to MoneTracka",
                    fontSize = 26.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "What should we call you?",
                    fontSize = 15.sp,
                    color = Color(0xFF8FA2B6),
                    textAlign = TextAlign.Center
                )
            }

            OutlinedTextField(
                value = userName,
                onValueChange = onNameChange,
                placeholder = { Text("Your name (e.g. Thanh Pham)", color = Color(0xFF54687F)) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Done
                ),
                keyboardActions = KeyboardActions(onDone = { onNext() }),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedContainerColor = Color(0xFF172535),
                    unfocusedContainerColor = Color(0xFF172535),
                    focusedBorderColor = Color(0xFF00D09C),
                    unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    @Composable
    private fun StepCurrency(
        selectedCurrency: String,
        onSelectCurrency: (String) -> Unit
    ) {
        val currencies = listOf(
            Triple("USD", "$", "United States Dollar"),
            Triple("EUR", "€", "Euro"),
            Triple("GBP", "£", "British Pound"),
            Triple("VND", "₫", "Vietnamese Dong")
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Choose Your Currency",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Select the base currency for all accounts & analytics.",
                    fontSize = 14.sp,
                    color = Color(0xFF8FA2B6)
                )
            }

            currencies.forEach { (code, symbol, name) ->
                val isSelected = selectedCurrency == code
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(if (isSelected) Color(0xFF102822) else Color(0xFF172535))
                        .border(
                            width = if (isSelected) 2.dp else 1.dp,
                            color = if (isSelected) Color(0xFF00D09C) else Color.White.copy(alpha = 0.08f),
                            shape = RoundedCornerShape(18.dp)
                        )
                        .clickable { onSelectCurrency(code) }
                        .padding(18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(
                                        if (isSelected) Color(0xFF00D09C)
                                        else Color.White.copy(alpha = 0.08f)
                                    )
                            ) {
                                Text(
                                    text = symbol,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF051A12) else Color.White
                                )
                            }
                            Column {
                                Text(
                                    text = code,
                                    color = Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = name,
                                    color = Color(0xFF8FA2B6),
                                    fontSize = 12.sp
                                )
                            }
                        }

                        if (isSelected) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF00D09C))
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected",
                                    tint = Color(0xFF051A12),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    @Composable
    private fun StepOpeningBalances(
        currency: String,
        checkingBalance: String,
        cashBalance: String,
        onCheckingChange: (String) -> Unit,
        onCashChange: (String) -> Unit
    ) {
        val symbol = when (currency) {
            "EUR" -> "€"
            "GBP" -> "£"
            "VND" -> "₫"
            else -> "$"
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "Initial Accounts",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Enter current balances to initialize your net worth.",
                    fontSize = 14.sp,
                    color = Color(0xFF8FA2B6)
                )
            }

            // Checking Account
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF172535))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("🏦", fontSize = 20.sp)
                    Text("Main Checking", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                OutlinedTextField(
                    value = checkingBalance,
                    onValueChange = onCheckingChange,
                    prefix = { Text("$symbol ", color = Color(0xFF00D09C), fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0C1622),
                        unfocusedContainerColor = Color(0xFF0C1622),
                        focusedBorderColor = Color(0xFF00D09C),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Cash Wallet
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color(0xFF172535))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("💵", fontSize = 20.sp)
                    Text("Cash Wallet", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                OutlinedTextField(
                    value = cashBalance,
                    onValueChange = onCashChange,
                    prefix = { Text("$symbol ", color = Color(0xFF00D09C), fontWeight = FontWeight.Bold) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF0C1622),
                        unfocusedContainerColor = Color(0xFF0C1622),
                        focusedBorderColor = Color(0xFF00D09C),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }

    @Composable
    private fun StepReview(
        userName: String,
        initials: String,
        currency: String,
        checkingBalance: Double,
        cashBalance: Double
    ) {
        val totalStartingBalance = checkingBalance + cashBalance

        Column(
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column {
                Text(
                    text = "You're All Set!",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Here's your starting financial profile.",
                    fontSize = 14.sp,
                    color = Color(0xFF8FA2B6)
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(22.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(Color(0xFF1A2B3D), Color(0xFF101C2A))
                        )
                    )
                    .border(1.dp, Color(0xFF00D09C).copy(alpha = 0.3f), RoundedCornerShape(22.dp))
                    .padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF00D09C))
                    ) {
                        Text(
                            text = initials,
                            color = Color(0xFF051A12),
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                    }
                    Column {
                        Text(
                            text = userName,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Base Currency: $currency",
                            fontSize = 13.sp,
                            color = Color(0xFF8FA2B6)
                        )
                    }
                }

                HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "STARTING NET WORTH",
                        fontSize = 11.sp,
                        color = Color(0xFF8FA2B6),
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = CurrencyFormatter.format(totalStartingBalance, currency),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF00D09C)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "🏦 Checking: ${CurrencyFormatter.format(checkingBalance, currency)}",
                        color = Color(0xFF8FA2B6),
                        fontSize = 12.sp
                    )
                    Text(
                        text = "💵 Cash: ${CurrencyFormatter.format(cashBalance, currency)}",
                        color = Color(0xFF8FA2B6),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}
