package com.example.data.model

import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ExpenseEntity
import org.json.JSONArray
import org.json.JSONObject

data class BackupData(
    val categories: List<CategoryEntity>,
    val expenses: List<ExpenseEntity>,
    val selectedCategoryFilter: Long = -1L,
    val currencySymbol: String = "৳",
    val isDarkMode: Boolean = false,
    val backupTimestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): String {
        val root = JSONObject()
        root.put("app", "AdvancedExpenseTracker")
        root.put("version", 1)
        root.put("timestamp", backupTimestamp)
        root.put("currencySymbol", currencySymbol)
        root.put("isDarkMode", isDarkMode)
        root.put("selectedCategoryFilter", selectedCategoryFilter)

        val catArray = JSONArray()
        categories.forEach { cat ->
            val obj = JSONObject()
            obj.put("id", cat.id)
            obj.put("name", cat.name)
            obj.put("budget", cat.budget)
            catArray.put(obj)
        }
        root.put("categories", catArray)

        val expArray = JSONArray()
        expenses.forEach { exp ->
            val obj = JSONObject()
            obj.put("id", exp.id)
            obj.put("description", exp.description)
            obj.put("amount", exp.amount)
            obj.put("date", exp.date)
            obj.put("categoryId", exp.categoryId)
            expArray.put(obj)
        }
        root.put("expenses", expArray)

        return root.toString(2)
    }

    companion object {
        fun parseJson(jsonStr: String): BackupData {
            val root = JSONObject(jsonStr)

            var currencySymbol = root.optString("currencySymbol", "৳")
            var isDarkMode = root.optBoolean("isDarkMode", false)
            var selectedCategoryFilter = root.optLong("selectedCategoryFilter", -1L)
            val timestamp = root.optLong("timestamp", System.currentTimeMillis())

            if (root.has("user_preferences")) {
                val prefObj = root.getJSONObject("user_preferences")
                currencySymbol = prefObj.optString("currencySymbol", currencySymbol)
                isDarkMode = prefObj.optBoolean("isDarkMode", isDarkMode)
                selectedCategoryFilter = prefObj.optLong("selectedCategoryFilter", selectedCategoryFilter)
            }

            val categories = mutableListOf<CategoryEntity>()
            if (root.has("categories")) {
                val catArray = root.getJSONArray("categories")
                for (i in 0 until catArray.length()) {
                    val obj = catArray.getJSONObject(i)
                    categories.add(
                        CategoryEntity(
                            id = obj.optLong("id", 0L),
                            name = obj.getString("name"),
                            budget = obj.optDouble("budget", 0.0)
                        )
                    )
                }
            }

            val expenses = mutableListOf<ExpenseEntity>()
            if (root.has("expenses")) {
                val expArray = root.getJSONArray("expenses")
                for (i in 0 until expArray.length()) {
                    val obj = expArray.getJSONObject(i)
                    expenses.add(
                        ExpenseEntity(
                            id = obj.optLong("id", 0L),
                            description = obj.getString("description"),
                            amount = obj.getDouble("amount"),
                            date = obj.getLong("date"),
                            categoryId = obj.getLong("categoryId")
                        )
                    )
                }
            }

            return BackupData(
                categories = categories,
                expenses = expenses,
                selectedCategoryFilter = selectedCategoryFilter,
                currencySymbol = currencySymbol,
                isDarkMode = isDarkMode,
                backupTimestamp = timestamp
            )
        }
    }
}
