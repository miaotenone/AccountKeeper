package com.example.accountkeeper.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.accountkeeper.data.model.Budget
import com.example.accountkeeper.ui.theme.AppStrings
import java.util.Locale

@Composable
fun TransactionBudgetHint(
    strings: AppStrings,
    currency: String,
    monthlyBudget: Budget?,
    monthlySpent: Double,
    categoryBudget: Budget?,
    categorySpent: Double
) {
    val monthlyOver = monthlyBudget != null && monthlySpent > monthlyBudget.amount
    val categoryOver = categoryBudget != null && categorySpent > categoryBudget.amount
    Text(
        text = "${strings.budget}: ${monthlyBudget?.let { "$currency${String.format(Locale.US, "%.2f", it.amount)}" } ?: strings.none} / ${strings.amount} $currency${String.format(Locale.US, "%.2f", monthlySpent)}",
        color = if (monthlyOver) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
    )
    if (categoryBudget != null) {
        Text(
            text = "${strings.category}: $currency${String.format(Locale.US, "%.2f", categorySpent)} / $currency${String.format(Locale.US, "%.2f", categoryBudget.amount)}",
            color = if (categoryOver) MaterialTheme.colorScheme.error else Color.Unspecified
        )
    }
}
