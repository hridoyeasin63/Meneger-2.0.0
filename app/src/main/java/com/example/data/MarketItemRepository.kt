package com.example.data

import kotlinx.coroutines.flow.Flow

class MarketItemRepository(private val marketItemDao: MarketItemDao) {
    val allItems: Flow<List<MarketItem>> = marketItemDao.getAllItems()

    suspend fun insert(item: MarketItem): Long {
        return marketItemDao.insertItem(item)
    }

    suspend fun update(item: MarketItem) {
        marketItemDao.updateItem(item)
    }

    suspend fun delete(item: MarketItem) {
        marketItemDao.deleteItem(item)
    }

    suspend fun deleteById(id: Int) {
        marketItemDao.deleteItemById(id)
    }

    suspend fun clearAllActualPrices() {
        marketItemDao.clearAllActualPrices()
    }

    suspend fun clearActualPricesForIds(ids: List<Int>) {
        if (ids.isNotEmpty()) {
            marketItemDao.clearActualPricesForIds(ids)
        }
    }
}
