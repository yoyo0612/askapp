package com.example.healthapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.HealthApp
import com.example.healthapp.data.BodyRepository
import com.example.healthapp.data.DailyTotal
import com.example.healthapp.data.DaySummary
import com.example.healthapp.data.ExerciseRepository
import com.example.healthapp.data.MealRepository
import com.example.healthapp.nutrition.Nutrients
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 目標値の入力欄の並び。Nutrients の項目と同じ順番。 */
val GOAL_LABELS = listOf(
    "エネルギー(kcal)",
    "たんぱく質(g)",
    "脂質(g)",
    "炭水化物(g)",
    "食物繊維(g)",
    "ナトリウム(mg)",
)

data class GoalUiState(
    val fields: List<String> = List(GOAL_LABELS.size) { "" },
    val goal: Nutrients = Nutrients.ZERO,
    val today: DaySummary? = null,
    /** 今日の運動による消費カロリー合計。 */
    val burnedKcal: Double = 0.0,
    /** 直近7日のうち記録のある日（新しい順）。 */
    val recent: List<DailyTotal> = emptyList(),
    val message: String? = null,
)

class GoalViewModel(application: Application) : AndroidViewModel(application) {

    private val database = (application as HealthApp).database
    private val bodyRepository = BodyRepository(database)
    private val mealRepository = MealRepository(database)
    private val exerciseRepository = ExerciseRepository(database)

    private val _state = MutableStateFlow(GoalUiState())
    val state: StateFlow<GoalUiState> = _state.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            val goal = bodyRepository.getGoal()
            val today = LocalDate.now()
            val summary = mealRepository.daySummary(today)
            val burned = exerciseRepository.listFor(today).sumOf { it.kcal }
            val recent = mealRepository.dailyTotals(today.minusDays(6), today)
            _state.update {
                it.copy(
                    fields = goal.toList().map { value -> value.plainText() },
                    goal = goal,
                    today = summary,
                    burnedKcal = burned,
                    recent = recent.asReversed(),
                )
            }
        }
    }

    fun setField(index: Int, text: String) {
        _state.update { current ->
            current.copy(fields = current.fields.mapIndexed { i, old -> if (i == index) text else old })
        }
    }

    fun save() {
        val values = _state.value.fields.map { text -> text.toDoubleOrNull()?.takeIf { it >= 0 } }
        if (values.any { it == null }) {
            _state.update { it.copy(message = "すべての項目に0以上の数字を入力してください") }
            return
        }
        viewModelScope.launch {
            bodyRepository.saveGoal(values.map { it!! }.toNutrients())
            _state.update { it.copy(message = "目標を保存しました") }
            reload()
        }
    }
}

private fun Nutrients.toList(): List<Double> = listOf(kcal, proteinG, fatG, carbsG, fiberG, sodiumMg)

private fun List<Double>.toNutrients(): Nutrients =
    Nutrients(this[0], this[1], this[2], this[3], this[4], this[5])
