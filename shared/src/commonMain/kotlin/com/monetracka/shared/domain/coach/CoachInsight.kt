package com.monetracka.shared.domain.coach

/**
 * Priority and visual tone of the coach insight.
 */
enum class InsightSeverity {
    WARNING,
    CELEBRATION,
    INFO
}

/**
 * Represents a dynamic financial health insight generated from user transactions.
 */
data class CoachInsight(
    val title: String,
    val description: String,
    val severity: InsightSeverity,
    val actionSuggestion: String? = null
)
