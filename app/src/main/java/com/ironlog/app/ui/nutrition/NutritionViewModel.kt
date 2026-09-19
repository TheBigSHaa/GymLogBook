package com.ironlog.app.ui.nutrition

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ironlog.app.data.model.IngredientCheck
import com.ironlog.app.data.sync.SyncWorker
import dagger.hilt.android.qualifiers.ApplicationContext
import com.ironlog.app.data.model.MealCompletion
import com.ironlog.app.data.model.MealIngredient
import com.ironlog.app.data.model.MealOption
import com.ironlog.app.data.model.MealSlot
import com.ironlog.app.data.repository.NutritionRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.TemporalAdjusters
import javax.inject.Inject

@HiltViewModel
class NutritionViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repo: NutritionRepository,
) : ViewModel() {

    private val today: LocalDate = LocalDate.now()
    private val weekStart: LocalDate = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    private val weekEnd: LocalDate = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))

    private val _expandedSlotId = MutableStateFlow<Int?>(null)

    data class MealOptionWithState(
        val option: MealOption,
        val ingredients: List<MealIngredient>,
        val isDisabled: Boolean = false,
        val disabledReason: String? = null,
    )

    data class MealSlotWithOptions(
        val slot: MealSlot,
        val options: List<MealOptionWithState>,
    )

    data class NutritionUiState(
        val mealSlots: List<MealSlotWithOptions> = emptyList(),
        val expandedSlotId: Int? = null,
        val completions: Map<Int, MealCompletion> = emptyMap(),
        val ingredientChecks: Map<Int, Map<Int, Boolean>> = emptyMap(),
        val waterMl: Int = 0,
        val waterTargetMl: Int = 2500,
        val consumedProtein: Float = 0f,
        val consumedCarbs: Float = 0f,
        val consumedFat: Float = 0f,
        val targetProtein: Float = 175f,
        val targetCarbs: Float = 175f,
        val targetFat: Float = 60f,
        val weeklyEggsUsed: Int = 0,
        val weeklyEggBudget: Int = 4,
    )

    private data class StructureData(
        val slots: List<MealSlotWithOptions>,
        val ingredientMap: Map<Int, MealIngredient>,
    )

    private data class DynamicData(
        val completions: List<MealCompletion>,
        val todayChecks: List<IngredientCheck>,
        val weekChecks: List<IngredientCheck>,
        val waterMl: Int,
        val waterTargetMl: Int,
    )

    // Returns a map of optionId -> disabledReason for options that violate conflict rules.
    private fun computeDisabledOptions(
        structure: StructureData,
        todayChecks: List<IngredientCheck>,
    ): Map<Int, String> {
        val checkedIngredientIds = todayChecks.filter { it.checked }.map { it.ingredientId }.toSet()
        val allOptions = structure.slots.flatMap { it.options }

        // An option is "started" if the user has checked at least one of its ingredients today.
        fun isStarted(optionId: Int): Boolean =
            allOptions.firstOrNull { it.option.id == optionId }
                ?.ingredients?.any { it.id in checkedIngredientIds } == true

        val disabled = mutableMapOf<Int, String>()

        // Rule 1: Max 3 whole eggs/day — disable egg-containing options that haven't been started.
        val todayEggs = todayChecks.filter { it.checked }
            .sumOf { check -> structure.ingredientMap[check.ingredientId]?.wholeEggCount ?: 0 }
        if (todayEggs >= 3) {
            allOptions.forEach { opt ->
                val hasEggs = opt.ingredients.any {
                    (structure.ingredientMap[it.id]?.wholeEggCount ?: 0) > 0
                }
                if (hasEggs && !isStarted(opt.option.id)) {
                    disabled[opt.option.id] = "Max 3 eggs reached today"
                }
            }
        }

        // Rule 2: Red meat once/day (options 402, 603, 604).
        val redMeatIds = setOf(402, 603, 604)
        val startedRedMeat = redMeatIds.firstOrNull { isStarted(it) }
        if (startedRedMeat != null) {
            redMeatIds.filter { it != startedRedMeat }.forEach { optId ->
                disabled.putIfAbsent(optId, "Red meat already logged today")
            }
        }

        // Rule 3: Eggs (202) and Kebab (603) cannot be combined.
        if (isStarted(202) && !isStarted(603)) {
            disabled.putIfAbsent(603, "Can't combine eggs & kebab")
        }
        if (isStarted(603) && !isStarted(202)) {
            disabled.putIfAbsent(202, "Can't combine kebab & eggs")
        }

        return disabled
    }

    val uiState: StateFlow<NutritionUiState> = run {
        val structureFlow = combine(
            repo.getMealSlots(),
            repo.getAllOptions(),
            repo.getAllIngredients(),
        ) { slots, options, ingredients ->
            val optsBySlot = options.groupBy { it.mealSlotId }
            val ingsByOpt = ingredients.groupBy { it.mealOptionId }
            val slotList = slots.map { slot ->
                MealSlotWithOptions(
                    slot = slot,
                    options = (optsBySlot[slot.id] ?: emptyList()).map { opt ->
                        MealOptionWithState(opt, ingsByOpt[opt.id] ?: emptyList())
                    },
                )
            }
            StructureData(
                slots = slotList,
                ingredientMap = ingredients.associateBy { it.id },
            )
        }

        val dynamicFlow = combine(
            repo.getCompletionsForDate(today),
            repo.getAllChecksForDate(today),
            repo.getAllChecksForWeek(weekStart, weekEnd),
            repo.getDailyLog(today),
        ) { completions, todayChecks, weekChecks, log ->
            DynamicData(
                completions = completions,
                todayChecks = todayChecks,
                weekChecks = weekChecks,
                waterMl = log?.waterMl ?: 0,
                waterTargetMl = log?.waterTargetMl ?: 2500,
            )
        }

        combine(structureFlow, dynamicFlow, _expandedSlotId) { structure, dynamic, expandedId ->
            val completionsMap = dynamic.completions.associateBy { it.mealSlotId }

            var protein = 0f
            var carbs = 0f
            var fat = 0f
            dynamic.todayChecks.filter { it.checked }.forEach { check ->
                structure.ingredientMap[check.ingredientId]?.let { ing ->
                    protein += ing.proteinG
                    carbs += ing.carbsG
                    fat += ing.fatG
                }
            }

            val weeklyEggs = dynamic.weekChecks
                .filter { it.checked }
                .sumOf { check -> structure.ingredientMap[check.ingredientId]?.wholeEggCount ?: 0 }

            val checksMap = dynamic.todayChecks
                .groupBy { it.mealSlotId }
                .mapValues { (_, list) -> list.associate { it.ingredientId to it.checked } }

            val disabledOptions = computeDisabledOptions(structure, dynamic.todayChecks)

            val slotsWithConflicts = structure.slots.map { slotWithOpts ->
                slotWithOpts.copy(
                    options = slotWithOpts.options.map { optWithState ->
                        val reason = disabledOptions[optWithState.option.id]
                        optWithState.copy(
                            isDisabled = reason != null,
                            disabledReason = reason,
                        )
                    },
                )
            }

            NutritionUiState(
                mealSlots = slotsWithConflicts,
                expandedSlotId = expandedId,
                completions = completionsMap,
                ingredientChecks = checksMap,
                waterMl = dynamic.waterMl,
                waterTargetMl = dynamic.waterTargetMl,
                consumedProtein = protein,
                consumedCarbs = carbs,
                consumedFat = fat,
                weeklyEggsUsed = weeklyEggs,
            )
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), NutritionUiState())
    }

    fun expandSlot(slotId: Int) {
        _expandedSlotId.update { if (it == slotId) null else slotId }
    }

    fun selectOption(slotId: Int, optionId: Int) {
        viewModelScope.launch { repo.selectOption(today, slotId, optionId) }
    }

    fun toggleMealComplete(slotId: Int, optionId: Int) {
        viewModelScope.launch {
            repo.toggleMealCompletion(today, slotId, optionId)
            SyncWorker.syncNow(context)
        }
    }

    fun toggleIngredient(slotId: Int, ingredientId: Int) {
        viewModelScope.launch { repo.toggleIngredientCheck(today, slotId, ingredientId) }
    }

    fun addWater() {
        viewModelScope.launch { repo.addWater(today, 200) }
    }
}
