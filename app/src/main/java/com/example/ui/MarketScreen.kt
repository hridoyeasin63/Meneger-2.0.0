package com.example.ui

import android.os.Bundle
import android.widget.Toast
import kotlinx.coroutines.launch
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.TextRange
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import java.util.Locale
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.MarketItem
import com.example.ui.MarketViewModel

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarketApp(
    modifier: Modifier = Modifier,
    viewModel: MarketViewModel = viewModel(factory = MarketViewModel.Factory),
    financeViewModel: com.example.FinanceViewModel? = null
) {
    val items by viewModel.items.collectAsState()
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    // Multi-selection states for pushing to Tracker
    var isSelectionMode by remember(viewModel) {
        object : androidx.compose.runtime.MutableState<Boolean> {
            override var value: Boolean
                get() = viewModel.isSelectionMode
                set(value) { viewModel.isSelectionMode = value }
            override fun component1(): Boolean = value
            override fun component2(): (Boolean) -> Unit = { value = it }
        }
    }
    var selectedItemIds by remember(viewModel) {
        object : androidx.compose.runtime.MutableState<Set<Int>> {
            override var value: Set<Int>
                get() = viewModel.selectedItemIds
                set(value) { viewModel.selectedItemIds = value }
            override fun component1(): Set<Int> = value
            override fun component2(): (Set<Int>) -> Unit = { value = it }
        }
    }

    // Reorder mode state
    var isReorderMode by remember { mutableStateOf(false) }

    // Dialog State for custom edit modal
    var editingItem by remember { mutableStateOf<MarketItem?>(null) }

    // Undo States
    var recentlyDeletedItem by remember { mutableStateOf<MarketItem?>(null) }
    var showUndoBanner by remember { mutableStateOf(false) }

    // Delete confirmation state
    var showDeleteConfirmation by remember { mutableStateOf<MarketItem?>(null) }

    // Reset price confirmation dialog states
    var showResetAllConfirmDialog by remember { mutableStateOf(false) }
    var showResetSelectedConfirmDialog by remember { mutableStateOf(false) }

    // Timer to automatically clear/hide the undo banner after 5 seconds
    LaunchedEffect(showUndoBanner) {
        if (showUndoBanner) {
            kotlinx.coroutines.delay(5000)
            showUndoBanner = false
            recentlyDeletedItem = null
        }
    }

    // Sorting: 4 layout categories
    // 1. On and uncost list
    // 2. OFF and uncost list
    // 3. OFF and cost list
    // 4. On and cost list
    // Within each category, sorted by timestamp descending
    val sortedItems = remember(items) {
        items.sortedWith(
            compareBy<MarketItem> { item ->
                val hasCost = item.actualPrice > 0.0
                when {
                    item.isActive && !hasCost -> 0  // 1. On and uncost list
                    !item.isActive && !hasCost -> 1 // 2. OFF and uncost list
                    !item.isActive && hasCost -> 2  // 3. OFF and cost list
                    item.isActive && hasCost -> 3   // 4. On and cost list
                    else -> 4
                }
            }.thenByDescending { it.timestamp }
        )
    }

    // Totals calculations
    val totalTarget = items.filter { it.isActive }.sumOf { it.targetPrice }
    val totalActual = items.filter { it.isActive }.sumOf { it.actualPrice }
    val difference = totalTarget - totalActual

    var showMenu by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(Unit) {
                detectTapGestures {
                    focusManager.clearFocus()
                }
            }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(Unit) {
                    detectTapGestures {
                        focusManager.clearFocus()
                    }
                },
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            // Summary Card - Styled like Budget's summary card
            item(key = "bazar_summary_card") {
                val differenceAbs = if (difference >= 0) difference else -difference
                val displayDiffSymbol = if (difference >= 0) "+" else "-"
                val diffColor = if (difference >= 0) Color(0xFF86EFAC) else Color(0xFFFCA5A5)
                val budgetProgress = if (totalTarget > 0) (totalActual / totalTarget).toFloat().coerceIn(0f, 1f) else 0f

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .testTag("bazar_summary_card"),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFF0060A8)
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Top section
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Total Market Spend (Actual)",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = "৳" + convertToBengaliNumber(String.format(Locale.US, "%,.0f", totalActual)),
                                    style = MaterialTheme.typography.titleLarge.copy(
                                        fontSize = 26.sp,
                                        fontWeight = FontWeight.Bold
                                    ),
                                    color = Color.White
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.2f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("🛒", fontSize = 18.sp)
                            }
                        }

                        // Middle Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Target Budget",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "৳" + convertToBengaliNumber(String.format(Locale.US, "%,.0f", totalTarget)),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }

                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "Difference",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = Color.White.copy(alpha = 0.8f)
                                )
                                Text(
                                    text = "$displayDiffSymbol৳" + convertToBengaliNumber(String.format(Locale.US, "%,.0f", differenceAbs)),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = diffColor
                                )
                            }
                        }

                        // Bottom Progress Bar
                        Column {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Budget Usage",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White.copy(alpha = 0.85f)
                                )
                                Text(
                                    text = "${(budgetProgress * 100).toInt()}%",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { budgetProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFF86EFAC),
                                trackColor = Color.White.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }

            if (sortedItems.isNotEmpty()) {
                item(key = "bazar_header_bar") {
                    val areAllSelected = sortedItems.isNotEmpty() && selectedItemIds.size == sortedItems.size && sortedItems.all { it.id in selectedItemIds }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 0.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isSelectionMode) {
                                "Selected: ${convertToBengaliNumber(selectedItemIds.size.toString())}/${convertToBengaliNumber(sortedItems.size.toString())}"
                            } else if (isReorderMode) {
                                "Reorder Mode"
                            } else {
                                "Market List"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelectionMode) MaterialTheme.colorScheme.primary else if (isReorderMode) Color(0xFFD97706) else MaterialTheme.colorScheme.outlineVariant,
                            fontSize = 11.sp,
                            modifier = Modifier.weight(1f)
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            if (!isSelectionMode) {
                                // Sajano (Reorder) button next to Select button (Hidden when selection mode is active)
                                TextButton(
                                    onClick = {
                                        isReorderMode = !isReorderMode
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("bazar_reorder_button")
                                ) {
                                    Icon(
                                        imageVector = if (isReorderMode) Icons.Default.Close else Icons.Default.SwapVert,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = if (isReorderMode) Color(0xFFD97706) else MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (isReorderMode) "Cancel" else "Reorder",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isReorderMode) Color(0xFFD97706) else MaterialTheme.colorScheme.primary
                                    )
                                }

                                // Reset Price (Clean) button beside Reorder & Select
                                TextButton(
                                    onClick = {
                                        showResetAllConfirmDialog = true
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("reset_all_price_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reset Price",
                                        modifier = Modifier.size(15.dp),
                                        tint = Color(0xFF0284C7)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Reset Price",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0284C7)
                                    )
                                }

                                // Select button
                                TextButton(
                                    onClick = {
                                        isSelectionMode = true
                                        isReorderMode = false
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("bazar_select_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.List,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Select",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            } else {
                                // Selection Mode: Reorder button is HIDDEN.
                                // Select All / Deselect All toggle button
                                TextButton(
                                    onClick = {
                                        if (areAllSelected) {
                                            selectedItemIds = emptySet()
                                        } else {
                                            selectedItemIds = sortedItems.map { it.id }.toSet()
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("select_all_toggle_button")
                                ) {
                                    Icon(
                                        imageVector = if (areAllSelected) Icons.Default.Deselect else Icons.Default.SelectAll,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = Color(0xFF059669)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = if (areAllSelected) "Deselect All" else "Select All",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF059669)
                                    )
                                }

                                // Reset Price (Clean) button for selected items
                                TextButton(
                                    onClick = {
                                        if (selectedItemIds.isEmpty()) {
                                            Toast.makeText(context, "Please select items first!", Toast.LENGTH_SHORT).show()
                                        } else {
                                            showResetSelectedConfirmDialog = true
                                        }
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("reset_selected_price_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Reset Price",
                                        modifier = Modifier.size(15.dp),
                                        tint = Color(0xFF0284C7)
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Reset Price",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0284C7)
                                    )
                                }

                                // Cancel button to exit selection mode
                                TextButton(
                                    onClick = {
                                        isSelectionMode = false
                                        selectedItemIds = emptySet()
                                    },
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 4.dp),
                                    modifier = Modifier
                                        .height(32.dp)
                                        .testTag("bazar_cancel_selection_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = null,
                                        modifier = Modifier.size(15.dp),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                    Spacer(modifier = Modifier.width(3.dp))
                                    Text(
                                        text = "Cancel",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Main List using LazyColumn items
            if (sortedItems.isNotEmpty()) {
                items(
                    items = sortedItems,
                    key = { it.id }
                ) { item ->
                    val index = sortedItems.indexOfFirst { it.id == item.id }
                    MarketItemRow(
                        item = item,
                        onActualChange = { newPrice ->
                            viewModel.updateActualPrice(item, newPrice)
                        },
                        onDoubleClick = {
                            editingItem = item
                        },
                        onLongClick = {
                            showDeleteConfirmation = item
                        },
                        onToggleActive = {
                            viewModel.toggleItemActive(item)
                        },
                        isSelectionMode = isSelectionMode,
                        isSelected = item.id in selectedItemIds,
                        onSelectedChange = { selected ->
                            selectedItemIds = if (selected) {
                                selectedItemIds + item.id
                            } else {
                                selectedItemIds - item.id
                            }
                        },
                        isReorderMode = isReorderMode,
                        canMoveUp = index > 0,
                        canMoveDown = index < sortedItems.size - 1,
                        onMoveUp = {
                            viewModel.moveItemUp(item, sortedItems)
                        },
                        onMoveDown = {
                            viewModel.moveItemDown(item, sortedItems)
                        },
                        modifier = Modifier.animateItemPlacement(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        )
                    )
                }
            } else {
                item(key = "bazar_empty_state") {
                    Spacer(modifier = Modifier.height(36.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = "Empty Basket",
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                            modifier = Modifier.size(72.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "Your list is empty!",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Tap the plus (+) button below to add new items.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            item(key = "bazar_bottom_spacer") {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }

        // Custom Floating Undo Banner
        if (showUndoBanner && recentlyDeletedItem != null) {
            Card(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.inverseSurface
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 0.dp, vertical = 6.dp)
                    .testTag("undo_banner"),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ShoppingCart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.inverseOnSurface,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "\"${recentlyDeletedItem?.description}\" deleted",
                            color = MaterialTheme.colorScheme.inverseOnSurface,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    androidx.compose.material3.TextButton(
                        onClick = {
                            recentlyDeletedItem?.let { itemToRestore ->
                                viewModel.insertItem(itemToRestore)
                                Toast.makeText(context, "Item restored!", Toast.LENGTH_SHORT).show()
                            }
                            showUndoBanner = false
                            recentlyDeletedItem = null
                        },
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "Undo",
                            color = MaterialTheme.colorScheme.inversePrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // Floating buttons have been relocated to the floatingActionButton slot in MainActivity next to the plus button.
    }

    // Custom dialog edit modal for double-tap edit
    editingItem?.let { item ->
        val (initialNum, initialUnit) = remember(item.id) {
            val parts = item.quantity.split(" ")
            if (parts.size >= 2 && parts.last() in listOf("kg", "g", "pcs", "কেজি", "গ্রাম", "টি")) {
                val normUnit = when(parts.last()) {
                    "কেজি" -> "kg"
                    "গ্রাম" -> "g"
                    "টি" -> "pcs"
                    else -> parts.last()
                }
                Pair(parts.dropLast(1).joinToString(" "), normUnit)
            } else {
                Pair(item.quantity, "kg")
            }
        }
        var editDesc by remember(item.id) { mutableStateOf(item.description) }
        var editQtyNum by remember(item.id) { mutableStateOf(initialNum) }
        var editQtyUnit by remember(item.id) { mutableStateOf(initialUnit) }
        var editTarget by remember(item.id) { mutableStateOf(if (item.targetPrice == 0.0) "" else item.targetPrice.toString()) }
        var editActual by remember(item.id) { mutableStateOf(if (item.actualPrice == 0.0) "" else item.actualPrice.toString()) }
        var editIsActive by remember(item.id) { mutableStateOf(item.isActive) }

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { editingItem = null },
            title = {
                Text(
                    text = "Edit Item",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = editDesc,
                        onValueChange = { editDesc = it },
                        label = { Text("Item Description") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = editQtyNum,
                            onValueChange = { editQtyNum = it },
                            label = { Text("Quantity") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1.5f)
                        )

                        var editDropdownExpanded by remember { mutableStateOf(false) }
                        val editUnits = listOf("kg", "g", "pcs")

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .padding(top = 8.dp) // align with OutlinedTextField's top label baseline
                        ) {
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.8f)
                                ),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { editDropdownExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = editQtyUnit,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Unit",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = editDropdownExpanded,
                                onDismissRequest = { editDropdownExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                editUnits.forEach { unit ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = unit,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        onClick = {
                                            editQtyUnit = unit
                                            editDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = editTarget,
                            onValueChange = { editTarget = it },
                            label = { Text("Target ৳") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = editActual,
                            onValueChange = { editActual = it },
                            label = { Text("Actual ৳") },
                            placeholder = { Text("Actual") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Item Status:",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        FilterChip(
                            selected = editIsActive,
                            onClick = { editIsActive = !editIsActive },
                            label = {
                                Text(
                                    text = if (editIsActive) "Active (ON)" else "Inactive (OFF)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = Color(0xFF0060A8),
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetVal = editTarget.toDoubleOrNull()
                        val actualVal = editActual.toDoubleOrNull() ?: 0.0
                        val finalQty = if (editQtyNum.trim().isNotEmpty()) "${editQtyNum.trim()} $editQtyUnit" else ""
                        if (editDesc.trim().isNotEmpty() && finalQty.isNotEmpty() && targetVal != null) {
                            val updated = item.copy(
                                description = editDesc.trim(),
                                quantity = finalQty,
                                targetPrice = targetVal,
                                actualPrice = actualVal,
                                isActive = editIsActive
                            )
                            viewModel.updateItem(updated)
                            editingItem = null
                            Toast.makeText(context, "Updated successfully!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { editingItem = null }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.outlineVariant)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Delete Confirmation Dialog with Undo trigger
    showDeleteConfirmation?.let { itemToDelete ->
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showDeleteConfirmation = null },
            title = {
                Text(
                    text = "Delete Item?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "\"${itemToDelete.description}\" will be removed. Are you sure?",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (financeViewModel != null) {
                            financeViewModel.deleteMarketItemToTrash(itemToDelete)
                        } else {
                            viewModel.deleteItem(itemToDelete)
                        }
                        recentlyDeletedItem = itemToDelete
                        showUndoBanner = true
                        showDeleteConfirmation = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showDeleteConfirmation = null }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.outlineVariant)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Reset All Actual Prices Confirmation Dialog
    if (showResetAllConfirmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showResetAllConfirmDialog = false },
            title = {
                Text(
                    text = "Reset All Prices?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to reset the actual price of all market items to ৳0? Target prices and active states will be kept.",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllActualPrices()
                        showResetAllConfirmDialog = false
                        Toast.makeText(context, "All actual prices reset to ৳0", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reset All", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showResetAllConfirmDialog = false }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.outlineVariant)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Reset Selected Actual Prices Confirmation Dialog
    if (showResetSelectedConfirmDialog) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { showResetSelectedConfirmDialog = false },
            title = {
                Text(
                    text = "Reset Selected Prices?",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to reset the actual price of the ${selectedItemIds.size} selected item(s) to ৳0?",
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearActualPricesForSelected(selectedItemIds)
                        showResetSelectedConfirmDialog = false
                        Toast.makeText(context, "Selected actual prices reset to ৳0", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Reset Selected", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { showResetSelectedConfirmDialog = false }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.outlineVariant)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }

    // Add Item Dialog
    if (viewModel.showAddItemDialog) {
        var addDesc by remember { mutableStateOf("") }
        var addQtyNum by remember { mutableStateOf("") }
        var addQtyUnit by remember { mutableStateOf("kg") }
        var addTarget by remember { mutableStateOf("") }
        var addDropdownExpanded by remember { mutableStateOf(false) }
        val addUnits = listOf("kg", "g", "pcs")

        androidx.compose.material3.AlertDialog(
            onDismissRequest = { viewModel.showAddItemDialog = false },
            title = {
                Text(
                    text = "Add New Item",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedTextField(
                        value = addDesc,
                        onValueChange = { addDesc = it },
                        label = { Text("Item Description (e.g. Rice, Oil)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = addQtyNum,
                            onValueChange = { addQtyNum = it },
                            label = { Text("Quantity") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(
                                keyboardType = KeyboardType.Number,
                                imeAction = ImeAction.Next
                            ),
                            modifier = Modifier.weight(1.1f)
                        )

                        // Dropdown for Unit
                        Box(
                            modifier = Modifier
                                .weight(0.9f)
                                .height(56.dp)
                                .padding(top = 8.dp)
                        ) {
                            Card(
                                shape = RoundedCornerShape(4.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color.White.copy(alpha = 0.8f)
                                ),
                                modifier = Modifier
                                    .fillMaxSize()
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                        shape = RoundedCornerShape(4.dp)
                                    )
                                    .clickable { addDropdownExpanded = true }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = addQtyUnit,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Icon(
                                        imageVector = Icons.Default.ArrowDropDown,
                                        contentDescription = "Select Unit",
                                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = addDropdownExpanded,
                                onDismissRequest = { addDropdownExpanded = false },
                                modifier = Modifier.background(Color.White)
                            ) {
                                addUnits.forEach { unit ->
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = unit,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Medium
                                            )
                                        },
                                        onClick = {
                                            addQtyUnit = unit
                                            addDropdownExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = addTarget,
                        onValueChange = { addTarget = it },
                        label = { Text("Target Price ৳") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val targetVal = addTarget.toDoubleOrNull()
                        val descTrim = addDesc.trim()
                        val qtyTrim = addQtyNum.trim()

                        if (descTrim.isNotEmpty() && qtyTrim.isNotEmpty() && targetVal != null) {
                            val combinedQty = "$qtyTrim $addQtyUnit"
                            viewModel.addItem(descTrim, combinedQty, targetVal)
                            viewModel.showAddItemDialog = false
                        } else {
                            Toast.makeText(context, "Please fill all fields correctly", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Save", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(
                    onClick = { viewModel.showAddItemDialog = false }
                ) {
                    Text("Cancel", color = MaterialTheme.colorScheme.outlineVariant)
                }
            },
            shape = RoundedCornerShape(20.dp),
            containerColor = MaterialTheme.colorScheme.surface
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MarketItemRow(
    item: MarketItem,
    onActualChange: (Double) -> Unit,
    onDoubleClick: () -> Unit,
    onLongClick: () -> Unit,
    onToggleActive: () -> Unit = {},
    isSelectionMode: Boolean = false,
    isSelected: Boolean = false,
    onSelectedChange: (Boolean) -> Unit = {},
    isReorderMode: Boolean = false,
    canMoveUp: Boolean = false,
    canMoveDown: Boolean = false,
    onMoveUp: () -> Unit = {},
    onMoveDown: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Key remember block on both item.id and item.actualPrice to stay in perfect sync with edits
    var actualInputState by remember(item.id, item.actualPrice) {
        val text = if (item.actualPrice == 0.0) "" else {
            if (item.actualPrice % 1.0 == 0.0) item.actualPrice.toInt().toString() else item.actualPrice.toString()
        }
        mutableStateOf(TextFieldValue(text = text, selection = TextRange(text.length)))
    }
    var isEditingActual by remember { mutableStateOf(false) }
    var hasBeenFocused by remember(isEditingActual) { mutableStateOf(false) }

    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(isEditingActual) {
        if (isEditingActual) {
            focusRequester.requestFocus()
        }
    }

    // Background color of the card: white for active, darker muted gray/slate for disabled items
    val isPaid = item.actualPrice > 0.0
    val isActiveItem = item.isActive
    val cardBgColor = if (isActiveItem) MaterialTheme.colorScheme.surface else Color(0xFFE2E8F0)

    val cardBorderColor = if (!isActiveItem) {
        Color(0xFF94A3B8) // Visible darker border for inactive/disabled items
    } else if (isPaid) {
        Color(0xFF10B981).copy(alpha = 0.5f) // Subtle green border for paid items
    } else {
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f)
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(
            containerColor = cardBgColor
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp), // Elevated box shadow for beautiful depth
        modifier = modifier
            .fillMaxWidth()
            .border(
                width = 1.dp,
                color = cardBorderColor,
                shape = RoundedCornerShape(10.dp)
            )
            .combinedClickable(
                onDoubleClick = {
                    if (!isSelectionMode) {
                        onDoubleClick()
                    }
                },
                onLongClick = {
                    if (!isSelectionMode) {
                        onLongClick()
                    }
                },
                onClick = {
                    if (isSelectionMode) {
                        onSelectedChange(!isSelected)
                    } else {
                        focusManager.clearFocus()
                    }
                }
            )
            .testTag("market_item_row_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Custom circle checkbox for multi-select mode
            if (isSelectionMode) {
                IconButton(
                    onClick = {
                        onSelectedChange(!isSelected)
                    },
                    modifier = Modifier
                        .padding(end = 6.dp)
                        .size(28.dp)
                        .testTag("checkbox_${item.id}")
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .border(
                                width = 1.5.dp,
                                color = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isSelected) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }
            }

            // Left side column containing description and target below
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(end = 6.dp)
            ) {
                Text(
                    text = item.description,
                    style = MaterialTheme.typography.bodyMedium.copy(fontSize = 15.sp),
                    fontWeight = FontWeight.Bold,
                    color = if (isActiveItem) MaterialTheme.colorScheme.onSurface else Color(0xFF94A3B8),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("item_description_${item.id}")
                )

                Spacer(modifier = Modifier.height(2.dp))

                // Target price under description
                Text(
                    text = "Target: ৳${if (item.targetPrice % 1.0 == 0.0) item.targetPrice.toInt() else item.targetPrice}",
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                    fontWeight = FontWeight.Medium,
                    color = if (isActiveItem) MaterialTheme.colorScheme.outlineVariant else Color(0xFF94A3B8),
                    modifier = Modifier.testTag("item_target_${item.id}")
                )
            }

            // Right side inputs and controls: scaled font styling
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                // Distinct background badge/chip for quantity (পরিমাণ)
                Box(
                    modifier = Modifier
                        .background(
                            color = if (isActiveItem) MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f) else Color(0xFFCBD5E1),
                            shape = RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = item.quantity,
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 12.sp),
                        fontWeight = FontWeight.Bold,
                        color = if (isActiveItem) MaterialTheme.colorScheme.onSecondaryContainer else Color(0xFF475569)
                    )
                }

                if (isEditingActual && isActiveItem) {
                    // Actual price numeric input field
                    Box(
                        modifier = Modifier
                            .width(72.dp)
                            .height(36.dp)
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.background)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.primary,
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.End
                        ) {
                            BasicTextField(
                                value = actualInputState,
                                onValueChange = { input ->
                                    val txt = input.text
                                    if (txt.isEmpty() || txt.toDoubleOrNull() != null || txt == ".") {
                                        actualInputState = input.copy(
                                            selection = TextRange(txt.length)
                                        )
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        isEditingActual = false
                                        val value = actualInputState.text.toDoubleOrNull() ?: 0.0
                                        onActualChange(value)
                                        focusManager.clearFocus()
                                    }
                                ),
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.End,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 14.sp
                                ),
                                singleLine = true,
                                modifier = Modifier
                                    .weight(1f)
                                    .focusRequester(focusRequester)
                                    .testTag("item_actual_input_${item.id}")
                                    .onFocusChanged { focusState ->
                                        if (focusState.isFocused) {
                                            hasBeenFocused = true
                                            val currentText = actualInputState.text
                                            actualInputState = TextFieldValue(
                                                text = currentText,
                                                selection = TextRange(currentText.length)
                                            )
                                        } else if (hasBeenFocused) {
                                            isEditingActual = false
                                            val value = actualInputState.text.toDoubleOrNull() ?: 0.0
                                            onActualChange(value)
                                        }
                                    },
                                decorationBox = { innerTextField ->
                                    Box(contentAlignment = Alignment.CenterEnd) {
                                        if (actualInputState.text.isEmpty()) {
                                            Text(
                                                text = "COST",
                                                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f),
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                        innerTextField()
                                    }
                                }
                            )
                            Spacer(modifier = Modifier.width(2.dp))
                            Text(
                                text = "৳",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }
                    }
                } else {
                    // Plain clickable Text representing actual price, turns to input on click
                    val displayActualText = if (item.actualPrice == 0.0) "COST" else {
                        "৳${if (item.actualPrice % 1.0 == 0.0) item.actualPrice.toInt().toString() else item.actualPrice.toString()}"
                    }
                    val displayColor = if (!isActiveItem) {
                        Color(0xFF94A3B8)
                    } else if (item.actualPrice == 0.0) {
                        MaterialTheme.colorScheme.outlineVariant
                    } else {
                        MaterialTheme.colorScheme.primary
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isActiveItem) MaterialTheme.colorScheme.background else Color(0xFFE2E8F0))
                            .border(
                                width = 1.dp,
                                color = if (isActiveItem) MaterialTheme.colorScheme.outline.copy(alpha = 0.25f) else Color(0xFFCBD5E1),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .clickable {
                                if (isActiveItem) {
                                    val text = if (item.actualPrice == 0.0) "" else {
                                        if (item.actualPrice % 1.0 == 0.0) item.actualPrice.toInt().toString() else item.actualPrice.toString()
                                    }
                                    actualInputState = TextFieldValue(
                                        text = text,
                                        selection = TextRange(text.length)
                                    )
                                    isEditingActual = true
                                }
                            }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = displayActualText,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            color = displayColor,
                            textAlign = TextAlign.Center,
                            fontSize = 15.sp
                        )
                    }
                }

                // Up / Down reorder controls (only shown when isReorderMode is toggled on)
                if (isReorderMode && !isSelectionMode) {
                    Column(
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (canMoveUp) Color(0xFF0060A8) else Color(0xFFE2E8F0))
                                .clickable(enabled = canMoveUp) { onMoveUp() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowUp,
                                contentDescription = "Move Up",
                                tint = if (canMoveUp) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (canMoveDown) Color(0xFF0060A8) else Color(0xFFE2E8F0))
                                .clickable(enabled = canMoveDown) { onMoveDown() },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Move Down",
                                tint = if (canMoveDown) Color.White else Color(0xFF94A3B8),
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun PremiumInputField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    singleLine: Boolean = true,
    leadingIcon: (@Composable () -> Unit)? = null
) {
    var isFocused by remember { mutableStateOf(false) }
    val borderColor = if (isFocused) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f) // Distinct clear border
    val borderWidth = if (isFocused) 1.5.dp else 1.dp
    val containerColor = if (isFocused) Color.White.copy(alpha = 0.95f) else Color.White.copy(alpha = 0.8f)

    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        keyboardOptions = keyboardOptions,
        keyboardActions = keyboardActions,
        singleLine = singleLine,
        textStyle = TextStyle(
            fontSize = 13.sp,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Medium
        ),
        modifier = modifier.onFocusChanged { isFocused = it.isFocused },
        decorationBox = { innerTextField ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(42.dp) // Fixed height to prevent shrinking/resizing on text entry
                    .background(containerColor, RoundedCornerShape(10.dp))
                    .border(
                        width = borderWidth,
                        color = borderColor,
                        shape = RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 12.dp), // Comfortable horizontal padding
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leadingIcon != null) {
                    leadingIcon()
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart // Ensures text is perfectly vertically centered
                ) {
                    if (value.isEmpty()) {
                        Text(
                            text = placeholder,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                        )
                    }
                    innerTextField()
                }
            }
        }
    )
}

fun convertToBengaliNumber(input: String): String {
    return input
}
