package com.example.ui

import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.example.data.AppDatabase
import com.example.data.MarketItem
import com.example.data.MarketItemRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class MarketViewModel(private val repository: MarketItemRepository) : ViewModel() {

    var showAddItemDialog by mutableStateOf(false)
    var isSelectionMode by mutableStateOf(false)
    var selectedItemIds by mutableStateOf(setOf<Int>())

    val items: StateFlow<List<MarketItem>> = repository.allItems
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun addItem(description: String, quantity: String, targetPrice: Double) {
        viewModelScope.launch {
            val item = MarketItem(
                description = description,
                quantity = quantity,
                targetPrice = targetPrice,
                actualPrice = 0.0
            )
            repository.insert(item)
        }
    }

    fun updateActualPrice(item: MarketItem, newActualPrice: Double) {
        viewModelScope.launch {
            val updated = item.copy(actualPrice = newActualPrice)
            repository.update(updated)
        }
    }

    fun updateItemActiveStatus(item: MarketItem, isActive: Boolean) {
        viewModelScope.launch {
            val updated = item.copy(isActive = isActive)
            repository.update(updated)
        }
    }

    fun toggleItemActive(item: MarketItem) {
        viewModelScope.launch {
            val updated = item.copy(isActive = !item.isActive, timestamp = System.currentTimeMillis())
            repository.update(updated)
        }
    }

    fun updateItem(item: MarketItem) {
        viewModelScope.launch {
            repository.update(item)
        }
    }

    fun insertItem(item: MarketItem) {
        viewModelScope.launch {
            repository.insert(item)
        }
    }

    fun deleteItem(item: MarketItem) {
        viewModelScope.launch {
            repository.delete(item)
        }
    }

    fun clearAllActualPrices() {
        viewModelScope.launch {
            repository.clearAllActualPrices()
        }
    }

    fun clearActualPricesForSelected(ids: Set<Int>) {
        if (ids.isNotEmpty()) {
            viewModelScope.launch {
                repository.clearActualPricesForIds(ids.toList())
            }
        }
    }

    fun moveItemUp(item: MarketItem, currentSortedList: List<MarketItem>) {
        val index = currentSortedList.indexOfFirst { it.id == item.id }
        if (index > 0) {
            val prevItem = currentSortedList[index - 1]
            val higherTimestamp = maxOf(item.timestamp, prevItem.timestamp) + 1000L
            val lowerTimestamp = minOf(item.timestamp, prevItem.timestamp) - 1000L
            viewModelScope.launch {
                repository.update(item.copy(timestamp = higherTimestamp))
                repository.update(prevItem.copy(timestamp = lowerTimestamp))
            }
        }
    }

    fun moveItemDown(item: MarketItem, currentSortedList: List<MarketItem>) {
        val index = currentSortedList.indexOfFirst { it.id == item.id }
        if (index >= 0 && index < currentSortedList.size - 1) {
            val nextItem = currentSortedList[index + 1]
            val higherTimestamp = maxOf(item.timestamp, nextItem.timestamp) + 1000L
            val lowerTimestamp = minOf(item.timestamp, nextItem.timestamp) - 1000L
            viewModelScope.launch {
                repository.update(nextItem.copy(timestamp = higherTimestamp))
                repository.update(item.copy(timestamp = lowerTimestamp))
            }
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(
                modelClass: Class<T>,
                extras: CreationExtras
            ): T {
                val application = checkNotNull(extras[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY])
                val database = AppDatabase.getDatabase(application)
                val repository = MarketItemRepository(database.marketItemDao())
                return MarketViewModel(repository) as T
            }
        }
    }
}
