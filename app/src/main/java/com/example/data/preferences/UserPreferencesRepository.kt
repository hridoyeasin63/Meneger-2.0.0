package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

data class UserPreferences(
    val isDarkMode: Boolean = false,
    val currencySymbol: String = "৳",
    val isDailyReminderEnabled: Boolean = false,
    val selectedCategoryFilter: Long = -1L // -1L means ALL
)

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val IS_DARK_MODE = booleanPreferencesKey("is_dark_mode")
        val CURRENCY_SYMBOL = stringPreferencesKey("currency_symbol")
        val DAILY_REMINDER_ENABLED = booleanPreferencesKey("daily_reminder_enabled")
        val SELECTED_CATEGORY_FILTER = longPreferencesKey("selected_category_filter")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { preferences ->
        UserPreferences(
            isDarkMode = preferences[PreferencesKeys.IS_DARK_MODE] ?: false,
            currencySymbol = preferences[PreferencesKeys.CURRENCY_SYMBOL] ?: "৳",
            isDailyReminderEnabled = preferences[PreferencesKeys.DAILY_REMINDER_ENABLED] ?: false,
            selectedCategoryFilter = preferences[PreferencesKeys.SELECTED_CATEGORY_FILTER] ?: -1L
        )
    }

    suspend fun updateDarkMode(isDarkMode: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.IS_DARK_MODE] = isDarkMode
        }
    }

    suspend fun updateCurrencySymbol(currencySymbol: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY_SYMBOL] = currencySymbol
        }
    }

    suspend fun updateDailyReminder(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.DAILY_REMINDER_ENABLED] = enabled
        }
    }

    suspend fun updateSelectedCategoryFilter(categoryId: Long) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.SELECTED_CATEGORY_FILTER] = categoryId
        }
    }

    suspend fun clearAllPreferences() {
        context.dataStore.edit { preferences ->
            preferences.clear()
        }
    }
}
