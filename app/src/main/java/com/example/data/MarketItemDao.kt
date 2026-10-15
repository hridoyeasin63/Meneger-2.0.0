package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface MarketItemDao {
    @Query("SELECT * FROM market_items ORDER BY timestamp ASC")
    fun getAllItems(): Flow<List<MarketItem>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItem(item: MarketItem): Long

    @Update
    suspend fun updateItem(item: MarketItem)

    @Delete
    suspend fun deleteItem(item: MarketItem)

    @Query("DELETE FROM market_items")
    suspend fun clearAllItems()

    @Query("DELETE FROM market_items WHERE id = :id")
    suspend fun deleteItemById(id: Int)

    @Query("UPDATE market_items SET actualPrice = 0.0")
    suspend fun clearAllActualPrices()

    @Query("UPDATE market_items SET actualPrice = 0.0 WHERE id IN (:ids)")
    suspend fun clearActualPricesForIds(ids: List<Int>)
}
