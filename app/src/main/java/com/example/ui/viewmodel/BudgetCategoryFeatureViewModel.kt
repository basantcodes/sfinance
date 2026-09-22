package com.example.ui.viewmodel

import com.example.data.local.entities.Budget
import com.example.data.local.entities.Category
import com.example.data.local.entities.CategoryType
import com.example.data.repository.FinanceRepository

class BudgetCategoryFeatureViewModel(
    private val repository: FinanceRepository,
    private val emitEvent: (String) -> Unit = {}
) {
    suspend fun createCategory(
        userId: String,
        name: String,
        type: CategoryType,
        icon: String? = null
    ) {
        repository.createCategory(userId, name, type, icon)
        emitEvent("Category '$name' created")
    }

    suspend fun saveBudget(
        userId: String,
        categoryId: String,
        monthlyLimit: Double,
        rollover: Boolean
    ) {
        repository.setBudget(userId, categoryId, monthlyLimit, rollover)
        emitEvent("Budget updated")
    }

    suspend fun deleteBudget(budget: Budget) {
        repository.deleteBudget(budget)
        emitEvent("Budget removed")
    }

    suspend fun getCategories(userId: String): List<Category> = repository.getCategories(userId)
}
