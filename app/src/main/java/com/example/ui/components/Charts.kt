package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.ui.theme.CategoryColors

data class CategoryExpenseItem(
    val category: CategoryEntity,
    val spent: Double,
    val percentage: Float,
    val color: Color
)

@Composable
fun PieChartCard(
    categories: List<CategoryEntity>,
    expenses: List<ExpenseEntity>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val categorySpentMap = remember(categories, expenses) {
        val map = mutableMapOf<Long, Double>()
        expenses.forEach { exp ->
            map[exp.categoryId] = (map[exp.categoryId] ?: 0.0) + exp.amount
        }
        map
    }

    val totalSpent = remember(categorySpentMap) { categorySpentMap.values.sum() }

    val chartItems = remember(categories, categorySpentMap, totalSpent) {
        categories.mapIndexedNotNull { index, cat ->
            val spent = categorySpentMap[cat.id] ?: 0.0
            if (spent > 0) {
                val percentage = if (totalSpent > 0) ((spent / totalSpent) * 100).toFloat() else 0f
                CategoryExpenseItem(
                    category = cat,
                    spent = spent,
                    percentage = percentage,
                    color = CategoryColors[index % CategoryColors.size]
                )
            } else null
        }.sortedByDescending { it.spent }
    }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(chartItems) {
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 1000))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Category Expenses Pie Chart",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (chartItems.isEmpty()) {
                Box(
                    modifier = Modifier.height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Not enough data to display pie chart",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(200.dp)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        var startAngle = -90f
                        val strokeWidth = 36.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset((size.width - diameter) / 2, (size.height - diameter) / 2)

                        chartItems.forEach { item ->
                            val sweepAngle = (item.percentage / 100f) * 360f * animationProgress.value
                            drawArc(
                                color = item.color,
                                startAngle = startAngle,
                                sweepAngle = sweepAngle,
                                useCenter = false,
                                topLeft = topLeft,
                                size = Size(diameter, diameter),
                                style = Stroke(width = strokeWidth)
                            )
                            startAngle += sweepAngle
                        }
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Total Spent",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "$currencySymbol %.0f".format(totalSpent),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Legend List
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    chartItems.forEach { item ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(item.color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = item.category.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            Text(
                                text = "$currencySymbol %.2f (%.1f%%)".format(item.spent, item.percentage),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BarChartCard(
    categories: List<CategoryEntity>,
    expenses: List<ExpenseEntity>,
    currencySymbol: String,
    modifier: Modifier = Modifier
) {
    val categorySpentMap = remember(categories, expenses) {
        val map = mutableMapOf<Long, Double>()
        expenses.forEach { exp ->
            map[exp.categoryId] = (map[exp.categoryId] ?: 0.0) + exp.amount
        }
        map
    }

    val topCategories = remember(categories, categorySpentMap) {
        categories.mapIndexed { index, cat ->
            val spent = categorySpentMap[cat.id] ?: 0.0
            CategoryExpenseItem(
                category = cat,
                spent = spent,
                percentage = 0f,
                color = CategoryColors[index % CategoryColors.size]
            )
        }.filter { it.spent > 0 }.sortedByDescending { it.spent }.take(6)
    }

    val maxSpent = remember(topCategories) {
        (topCategories.maxOfOrNull { it.spent } ?: 1.0).coerceAtLeast(1.0)
    }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(topCategories) {
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 1000))
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Top Category Expenses Bar Chart",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(20.dp))

            if (topCategories.isEmpty()) {
                Box(
                    modifier = Modifier.height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No data available to display bar chart",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else {
                Canvas(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .padding(horizontal = 8.dp)
                ) {
                    val barWidth = (size.width / (topCategories.size * 2)).coerceAtMost(36.dp.toPx())
                    val spacing = (size.width - (barWidth * topCategories.size)) / (topCategories.size + 1)

                    topCategories.forEachIndexed { index, item ->
                        val barHeight = ((item.spent / maxSpent) * (size.height - 30.dp.toPx())).toFloat() * animationProgress.value
                        val x = spacing + index * (barWidth + spacing)
                        val y = size.height - barHeight

                        drawRoundRect(
                            color = item.color,
                            topLeft = Offset(x, y),
                            size = Size(barWidth, barHeight),
                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(12f, 12f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    topCategories.forEach { item ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.width(50.dp)
                        ) {
                            Text(
                                text = item.category.name,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "%.0f".format(item.spent),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
