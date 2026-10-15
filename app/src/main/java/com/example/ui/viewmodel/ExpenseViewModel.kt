package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ExpenseEntity
import com.example.data.model.BackupData
import com.example.data.preferences.UserPreferences
import com.example.data.preferences.UserPreferencesRepository
import com.example.data.repository.ExpenseRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class ExpenseViewModel(
    private val repository: ExpenseRepository,
    private val preferencesRepository: UserPreferencesRepository
) : ViewModel() {

    val categories: StateFlow<List<CategoryEntity>> = repository.allCategories
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allExpenses: StateFlow<List<ExpenseEntity>> = repository.allExpenses
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val userPreferences: StateFlow<UserPreferences> = preferencesRepository.userPreferencesFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), UserPreferences())

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    // Filtered Expenses according to selected category filter and search query
    val filteredExpenses: StateFlow<List<ExpenseEntity>> = combine(
        allExpenses,
        categories,
        userPreferences,
        searchQuery
    ) { expensesList, categoriesList, prefs, query ->
        var list = expensesList

        // Category filter
        val selectedCatId = prefs.selectedCategoryFilter
        if (selectedCatId != -1L) {
            list = list.filter { it.categoryId == selectedCatId }
        }

        // Search query
        if (query.trim().isNotEmpty()) {
            val q = query.trim().lowercase()
            val categoryNameMap = categoriesList.associate { it.id to it.name.lowercase() }
            list = list.filter { exp ->
                exp.description.lowercase().contains(q) ||
                        (categoryNameMap[exp.categoryId]?.contains(q) == true)
            }
        }

        list
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setSelectedCategoryFilter(categoryId: Long) {
        viewModelScope.launch {
            preferencesRepository.updateSelectedCategoryFilter(categoryId)
        }
    }

    fun addCategory(name: String, budget: Double, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.insertCategory(name, budget)
                .onSuccess {
                    _snackbarMessage.value = "Category created successfully"
                    onSuccess()
                }
                .onFailure { error ->
                    _snackbarMessage.value = error.message ?: "An error occurred"
                }
        }
    }

    fun editCategory(category: CategoryEntity, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateCategory(category)
                .onSuccess {
                    _snackbarMessage.value = "Category updated successfully"
                    onSuccess()
                }
                .onFailure { error ->
                    _snackbarMessage.value = error.message ?: "An error occurred"
                }
        }
    }

    fun deleteCategory(category: CategoryEntity) {
        viewModelScope.launch {
            repository.deleteCategory(category)
            _snackbarMessage.value = "Category and all its expenses deleted"
        }
    }

    fun addExpense(
        description: String,
        amount: Double,
        date: Long,
        categoryId: Long,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            repository.insertExpense(description, amount, date, categoryId)
                .onSuccess {
                    _snackbarMessage.value = "Expense added successfully"
                    onSuccess()
                }
                .onFailure { error ->
                    _snackbarMessage.value = error.message ?: "An error occurred"
                }
        }
    }

    fun editExpense(expense: ExpenseEntity, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            repository.updateExpense(expense)
                .onSuccess {
                    _snackbarMessage.value = "Expense updated successfully"
                    onSuccess()
                }
                .onFailure { error ->
                    _snackbarMessage.value = error.message ?: "An error occurred"
                }
        }
    }

    fun moveExpenseCategory(expenseId: Long, newCategoryId: Long) {
        viewModelScope.launch {
            repository.moveExpenseCategory(expenseId, newCategoryId)
                .onSuccess {
                    _snackbarMessage.value = "Category changed successfully"
                }
                .onFailure { error ->
                    _snackbarMessage.value = error.message ?: "An error occurred"
                }
        }
    }

    fun moveExpenseUp(expense: ExpenseEntity, currentList: List<ExpenseEntity>) {
        val index = currentList.indexOfFirst { it.id == expense.id }
        if (index > 0) {
            val prevExpense = currentList[index - 1]
            val higherDate = maxOf(expense.date, prevExpense.date) + 1000L
            val lowerDate = minOf(expense.date, prevExpense.date) - 1000L
            viewModelScope.launch {
                repository.updateExpense(expense.copy(date = higherDate))
                repository.updateExpense(prevExpense.copy(date = lowerDate))
            }
        }
    }

    fun moveExpenseDown(expense: ExpenseEntity, currentList: List<ExpenseEntity>) {
        val index = currentList.indexOfFirst { it.id == expense.id }
        if (index >= 0 && index < currentList.size - 1) {
            val nextExpense = currentList[index + 1]
            val higherDate = maxOf(expense.date, nextExpense.date) + 1000L
            val lowerDate = minOf(expense.date, nextExpense.date) - 1000L
            viewModelScope.launch {
                repository.updateExpense(nextExpense.copy(date = higherDate))
                repository.updateExpense(expense.copy(date = lowerDate))
            }
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            repository.deleteExpense(expense)
            _snackbarMessage.value = "Expense record deleted"
        }
    }

    fun updateDarkMode(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateDarkMode(enabled)
        }
    }

    fun updateCurrencySymbol(symbol: String) {
        viewModelScope.launch {
            preferencesRepository.updateCurrencySymbol(symbol)
        }
    }

    fun updateDailyReminder(enabled: Boolean) {
        viewModelScope.launch {
            preferencesRepository.updateDailyReminder(enabled)
            if (enabled) {
                _snackbarMessage.value = "Daily reminder enabled"
            } else {
                _snackbarMessage.value = "Daily reminder disabled"
            }
        }
    }

    fun resetAllData() {
        viewModelScope.launch {
            repository.resetAllData()
            preferencesRepository.clearAllPreferences()
            _snackbarMessage.value = "All data reset successfully"
        }
    }

    fun createBackupJson(): String {
        val cats = categories.value
        val exps = allExpenses.value
        val prefs = userPreferences.value
        val backup = BackupData(
            categories = cats,
            expenses = exps,
            selectedCategoryFilter = prefs.selectedCategoryFilter,
            currencySymbol = prefs.currencySymbol,
            isDarkMode = prefs.isDarkMode
        )
        return backup.toJson()
    }

    fun restoreFromJson(jsonStr: String, onSuccess: () -> Unit = {}, onError: (String) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val backup = BackupData.parseJson(jsonStr)
                if (backup.categories.isEmpty() && backup.expenses.isEmpty()) {
                    onError("No valid data found in backup file")
                    return@launch
                }
                repository.restoreDatabase(backup.categories, backup.expenses)
                preferencesRepository.updateCurrencySymbol(backup.currencySymbol)
                preferencesRepository.updateDarkMode(backup.isDarkMode)
                preferencesRepository.updateSelectedCategoryFilter(backup.selectedCategoryFilter)

                _snackbarMessage.value = "Data restored successfully"
                onSuccess()
            } catch (e: Exception) {
                val msg = "Restore failed: ${e.localizedMessage ?: "Invalid JSON file"}"
                _snackbarMessage.value = msg
                onError(msg)
            }
        }
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showSnackbar(message: String) {
        _snackbarMessage.value = message
    }
}
