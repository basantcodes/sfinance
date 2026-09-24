package com.example.ui.viewmodel

import android.content.Context
import com.example.R
import com.example.data.local.entities.Budget
import com.example.data.local.entities.Category
import com.example.data.local.entities.CategoryType
import com.example.data.repository.FinanceRepository

class BudgetCategoryFeatureViewModel(
    private val context: Context,
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
        emitEvent(context.getString(R.string.category_created, name))
    }

    suspend fun saveBudget(
        userId: String,
        categoryId: String,
        monthlyLimit: Double,
        rollover: Boolean
    ) {
        repository.setBudget(userId, categoryId, monthlyLimit, rollover)
        emitEvent(context.getString(R.string.budget_updated))
    }

    suspend fun deleteBudget(budget: Budget) {
        repository.deleteBudget(budget)
        emitEvent(context.getString(R.string.budget_removed))
    }

    suspend fun getCategories(userId: String): List<Category> = repository.getCategories(userId)
}
