package com.example.data.repository

import com.example.data.local.dao.CategoryDao
import com.example.data.local.dao.ExpenseDao
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.ExpenseEntity
import kotlinx.coroutines.flow.Flow

class ExpenseRepository(
    private val categoryDao: CategoryDao,
    private val expenseDao: ExpenseDao
) {
    val allCategories: Flow<List<CategoryEntity>> = categoryDao.getAllCategories()
    val allExpenses: Flow<List<ExpenseEntity>> = expenseDao.getAllExpenses()

    fun getExpensesByCategoryId(categoryId: Long): Flow<List<ExpenseEntity>> {
        return expenseDao.getExpensesByCategoryId(categoryId)
    }

    fun searchExpenses(query: String): Flow<List<ExpenseEntity>> {
        return expenseDao.searchExpensesByDescription(query)
    }

    suspend fun getCategoryByName(name: String): CategoryEntity? {
        return categoryDao.getCategoryByName(name.trim())
    }

    suspend fun insertCategory(name: String, budget: Double): Result<Long> {
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Category name cannot be empty"))
        }
        val existing = categoryDao.getCategoryByName(trimmedName)
        if (existing != null) {
            return Result.failure(IllegalArgumentException("Category with this name already exists"))
        }
        val category = CategoryEntity(name = trimmedName, budget = budget)
        val id = categoryDao.insertCategory(category)
        return Result.success(id)
    }

    suspend fun updateCategory(category: CategoryEntity): Result<Unit> {
        val trimmedName = category.name.trim()
        if (trimmedName.isEmpty()) {
            return Result.failure(IllegalArgumentException("Category name cannot be empty"))
        }
        val existing = categoryDao.getCategoryByName(trimmedName)
        if (existing != null && existing.id != category.id) {
            return Result.failure(IllegalArgumentException("Another category already uses this name"))
        }
        categoryDao.updateCategory(category.copy(name = trimmedName))
        return Result.success(Unit)
    }

    suspend fun deleteCategory(category: CategoryEntity) {
        categoryDao.deleteCategory(category)
    }

    suspend fun insertExpense(description: String, amount: Double, date: Long, categoryId: Long): Result<Long> {
        val trimmedDesc = description.trim()
        if (trimmedDesc.isEmpty()) {
            return Result.failure(IllegalArgumentException("Description cannot be empty"))
        }
        if (amount <= 0.0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than 0"))
        }
        val expense = ExpenseEntity(
            description = trimmedDesc,
            amount = amount,
            date = date,
            categoryId = categoryId
        )
        val id = expenseDao.insertExpense(expense)
        return Result.success(id)
    }

    suspend fun updateExpense(expense: ExpenseEntity): Result<Unit> {
        if (expense.description.trim().isEmpty()) {
            return Result.failure(IllegalArgumentException("Description cannot be empty"))
        }
        if (expense.amount <= 0.0) {
            return Result.failure(IllegalArgumentException("Amount must be greater than 0"))
        }
        expenseDao.updateExpense(expense.copy(description = expense.description.trim()))
        return Result.success(Unit)
    }

    suspend fun moveExpenseCategory(expenseId: Long, newCategoryId: Long): Result<Unit> {
        val expense = expenseDao.getExpenseById(expenseId)
            ?: return Result.failure(IllegalArgumentException("Expense not found"))
        expenseDao.updateExpense(expense.copy(categoryId = newCategoryId))
        return Result.success(Unit)
    }

    suspend fun deleteExpense(expense: ExpenseEntity) {
        expenseDao.deleteExpense(expense)
    }

    suspend fun resetAllData() {
        expenseDao.deleteAllExpenses()
        categoryDao.deleteAllCategories()
    }

    suspend fun restoreDatabase(categories: List<CategoryEntity>, expenses: List<ExpenseEntity>) {
        expenseDao.deleteAllExpenses()
        categoryDao.deleteAllCategories()
        
        categoryDao.insertCategories(categories)
        expenseDao.insertExpenses(expenses)
    }
}
