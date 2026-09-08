package com.monetracka.shared.domain.coach

import com.monetracka.shared.domain.model.Category
import com.monetracka.shared.domain.model.Transaction
import com.monetracka.shared.domain.model.TransactionType
import com.monetracka.shared.domain.util.CurrencyFormatter

class SmartSavingCoachEngine {

    /**
     * Evaluates transaction data dynamically to provide personalized warnings or celebrations.
     * Computations are pure and fast (O(N) single-pass aggregation).
     */
    fun evaluate(
        transactions: List<Transaction>,
        categories: List<Category> = emptyList()
    ): CoachInsight {
        if (transactions.isEmpty()) {
            return CoachInsight(
                title = "Welcome to Your Saving Journey",
                description = "Log your first income and expenses to unlock dynamic saving coaching.",
                severity = InsightSeverity.INFO,
                actionSuggestion = "Tap '+' below to add a transaction"
            )
        }

        var totalIncome = 0.0
        var totalExpense = 0.0
        val expenseByCategory = mutableMapOf<Long, Double>()

        for (tx in transactions) {
            when (tx.type) {
                TransactionType.INCOME -> totalIncome += tx.amount
                TransactionType.EXPENSE -> {
                    totalExpense += tx.amount
                    expenseByCategory[tx.categoryId] = (expenseByCategory[tx.categoryId] ?: 0.0) + tx.amount
                }
                TransactionType.TRANSFER -> {
                    // Internal transfers do not affect income or expense totals
                }
            }
        }

        // Rule 1: High Deficit Warning
        if (totalExpense > totalIncome && totalIncome > 0) {
            val deficit = totalExpense - totalIncome
            val overspendPct = ((deficit / totalIncome) * 100).toInt()
            return CoachInsight(
                title = "Spending Exceeds Income",
                description = "Expenses surpass your recorded income by $overspendPct% (deficit of ${formatCurrency(deficit)}).",
                severity = InsightSeverity.WARNING,
                actionSuggestion = "Review non-essential purchases to restore positive cash flow"
            )
        }

        // Rule 2: Zero Income with Expenses
        if (totalIncome <= 0 && totalExpense > 0) {
            return CoachInsight(
                title = "Outflows Active",
                description = "You've recorded ${formatCurrency(totalExpense)} in expenses with no income yet logged.",
                severity = InsightSeverity.INFO,
                actionSuggestion = "Remember to record your paychecks or income sources"
            )
        }

        // Rule 3: Category Dominance Warning (> 40% of total expenses in one category when multiple expense categories exist)
        if (totalExpense > 0 && expenseByCategory.size > 1) {
            val highestCategoryEntry = expenseByCategory.maxByOrNull { it.value }
            if (highestCategoryEntry != null) {
                val categoryExpense = highestCategoryEntry.value
                val categoryShare = (categoryExpense / totalExpense)
                if (categoryShare >= 0.40 && totalExpense >= 100.0) {
                    val category = categories.find { it.id == highestCategoryEntry.key }
                    val catName = category?.name ?: "a single category"
                    val catEmoji = category?.emoji?.let { "$it " } ?: ""
                    val pct = (categoryShare * 100).toInt()
                    return CoachInsight(
                        title = "High Spending Concentration",
                        description = "$catEmoji$catName accounts for $pct% of your total spending (${formatCurrency(categoryExpense)}).",
                        severity = InsightSeverity.WARNING,
                        actionSuggestion = "Consider setting a budget cap for $catName"
                    )
                }
            }
        }

        // Rule 4: Super Saver Celebration (Saving rate >= 50%)
        val savings = totalIncome - totalExpense
        val savingsRate = if (totalIncome > 0) savings / totalIncome else 0.0
        val savingsRatePct = (savingsRate * 100).toInt()

        if (savingsRate >= 0.50) {
            return CoachInsight(
                title = "Exceptional Saving Rate!",
                description = "You are saving $savingsRatePct% of your income (${formatCurrency(savings)} saved). Fantastic discipline!",
                severity = InsightSeverity.CELEBRATION,
                actionSuggestion = "Keep it up! Your future self will thank you"
            )
        }

        // Rule 5: Healthy Saving (20% to 50%)
        if (savingsRate >= 0.20) {
            return CoachInsight(
                title = "Healthy Cash Flow",
                description = "You're retaining $savingsRatePct% of your earnings. You are comfortably within recommended saving guidelines.",
                severity = InsightSeverity.CELEBRATION,
                actionSuggestion = "Consider funneling savings into an emergency fund or investments"
            )
        }

        // Rule 6: Tight Margin Progress (0% to 20%)
        return CoachInsight(
            title = "Steady Progress",
            description = "You're keeping your head above water with a $savingsRatePct% saving cushion (${formatCurrency(savings)} saved).",
            severity = InsightSeverity.INFO,
            actionSuggestion = "Look for 1-2 small recurring costs you can trim"
        )
    }

    private fun formatCurrency(amount: Double): String = CurrencyFormatter.format(amount)
}
