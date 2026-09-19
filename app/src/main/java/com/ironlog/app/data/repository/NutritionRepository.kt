package com.ironlog.app.data.repository

import androidx.room.withTransaction
import com.ironlog.app.data.db.AppDatabase
import com.ironlog.app.data.db.dao.DailyNutritionLogDao
import com.ironlog.app.data.db.dao.IngredientCheckDao
import com.ironlog.app.data.db.dao.MealCompletionDao
import com.ironlog.app.data.db.dao.MealIngredientDao
import com.ironlog.app.data.db.dao.MealOptionDao
import com.ironlog.app.data.db.dao.MealSlotDao
import com.ironlog.app.data.model.DailyNutritionLog
import com.ironlog.app.data.model.IngredientCheck
import com.ironlog.app.data.model.MealCompletion
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NutritionRepository @Inject constructor(
    private val db: AppDatabase,
    private val mealSlotDao: MealSlotDao,
    private val mealOptionDao: MealOptionDao,
    private val mealIngredientDao: MealIngredientDao,
    private val dailyNutritionLogDao: DailyNutritionLogDao,
    private val mealCompletionDao: MealCompletionDao,
    private val ingredientCheckDao: IngredientCheckDao,
) {
    // ── Static structure flows ─────────────────────────────────────────────────
    fun getMealSlots() = mealSlotDao.getAllOrdered()
    fun getAllOptions() = mealOptionDao.getAll()
    fun getAllIngredients() = mealIngredientDao.getAll()

    // ── Dynamic / per-date flows ───────────────────────────────────────────────
    fun getDailyLog(date: LocalDate) = dailyNutritionLogDao.getForDate(date)
    fun getCompletionsForDate(date: LocalDate) = mealCompletionDao.getCompletionsForDate(date)
    fun getAllChecksForDate(date: LocalDate) = ingredientCheckDao.getAllChecksForDate(date)
    fun getAllChecksForWeek(start: LocalDate, end: LocalDate) =
        ingredientCheckDao.getAllChecksForWeek(start, end)

    // ── Water ─────────────────────────────────────────────────────────────────
    suspend fun addWater(date: LocalDate, amount: Int) {
        db.withTransaction {
            dailyNutritionLogDao.insertIfNotExists(DailyNutritionLog(date = date))
            dailyNutritionLogDao.addWater(date, amount)
        }
    }

    // ── Option selection: records the selected option without clearing ingredient checks ─
    // Ingredient checks persist across option switches so users can combine items from
    // multiple options within the same meal slot.
    suspend fun selectOption(date: LocalDate, slotId: Int, optionId: Int) {
        db.withTransaction {
            val existing = mealCompletionDao.getCompletion(date, slotId)
            val completed = existing?.completed ?: false
            mealCompletionDao.upsert(MealCompletion(date, slotId, optionId, completed = completed))
        }
    }

    // ── Quick-complete: toggle ALL ingredients in the selected option ──────────
    suspend fun toggleMealCompletion(date: LocalDate, slotId: Int, optionId: Int) {
        db.withTransaction {
            val ingredients = mealIngredientDao.getIngredientsForOptionOnce(optionId)
            if (ingredients.isEmpty()) return@withTransaction

            val currentChecks = ingredientCheckDao.getChecksForMealOnDateOnce(date, slotId)
            val checkedIds = currentChecks.filter { it.checked }.map { it.ingredientId }.toSet()
            val allChecked = ingredients.all { it.id in checkedIds }

            // If all checked → uncheck all; otherwise → check all
            val newChecked = !allChecked
            val newChecks = ingredients.map { ingredient ->
                IngredientCheck(date, slotId, ingredient.id, checked = newChecked)
            }
            ingredientCheckDao.upsertAll(newChecks)

            // Keep MealCompletion in sync for option tracking
            mealCompletionDao.upsert(MealCompletion(date, slotId, optionId, completed = newChecked))
        }
    }

    // ── Per-ingredient toggle ─────────────────────────────────────────────────
    suspend fun toggleIngredientCheck(date: LocalDate, slotId: Int, ingredientId: Int) {
        db.withTransaction {
            val existing = ingredientCheckDao.getCheck(date, slotId, ingredientId)
            ingredientCheckDao.upsert(
                IngredientCheck(date, slotId, ingredientId, checked = !(existing?.checked ?: false)),
            )
        }
    }
}
