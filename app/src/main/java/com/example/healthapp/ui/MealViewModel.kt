package com.example.healthapp.ui

import android.app.Application
import android.database.sqlite.SQLiteException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.data.DaySummary
import com.example.healthapp.data.Food
import com.example.healthapp.data.HealthDatabase
import com.example.healthapp.data.MealItemInput
import com.example.healthapp.data.MealRepository
import com.example.healthapp.data.MealType
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DraftItem(val foodId: Long, val name: String, val grams: Double)

data class MealUiState(
    val date: LocalDate = LocalDate.now(),
    val mealType: MealType = MealType.BREAKFAST,
    val foods: List<Food> = emptyList(),
    val selectedFoodId: Long? = null,
    val gramsText: String = "100",
    val draft: List<DraftItem> = emptyList(),
    val summary: DaySummary? = null,
    val message: String? = null,
)

class MealViewModel(application: Application) : AndroidViewModel(application) {

    private val database = HealthDatabase(application)
    private val repository = MealRepository(database)

    private val _state = MutableStateFlow(MealUiState())
    val state: StateFlow<MealUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            val foods = repository.listFoods()
            _state.update { it.copy(foods = foods, selectedFoodId = foods.firstOrNull()?.id) }
            refreshSummary()
        }
    }

    fun setDate(date: LocalDate) {
        _state.update { it.copy(date = date, message = null) }
        viewModelScope.launch { refreshSummary() }
    }

    fun setMealType(mealType: MealType) {
        _state.update { it.copy(mealType = mealType) }
    }

    fun selectFood(foodId: Long) {
        _state.update { it.copy(selectedFoodId = foodId) }
    }

    fun setGrams(text: String) {
        _state.update { it.copy(gramsText = text) }
    }

    fun addDraftItem() {
        val current = _state.value
        val food = current.foods.firstOrNull { it.id == current.selectedFoodId } ?: return
        val grams = current.gramsText.toDoubleOrNull()
        if (grams == null || grams <= 0) {
            _state.update { it.copy(message = "量は1g以上の数字で入力してください") }
            return
        }
        _state.update {
            it.copy(
                draft = it.draft + DraftItem(food.id, food.name, grams),
                message = null,
            )
        }
    }

    fun removeDraftItem(index: Int) {
        _state.update { it.copy(draft = it.draft.filterIndexed { i, _ -> i != index }) }
    }

    fun saveMeal() {
        val current = _state.value
        if (current.draft.isEmpty()) {
            _state.update { it.copy(message = "食品を1つ以上追加してください") }
            return
        }
        viewModelScope.launch {
            try {
                repository.addMeal(
                    date = current.date,
                    mealType = current.mealType,
                    items = current.draft.map { MealItemInput(it.foodId, it.grams) },
                )
                _state.update { it.copy(draft = emptyList(), message = "記録しました") }
                refreshSummary()
            } catch (e: SQLiteException) {
                _state.update { it.copy(message = "保存に失敗しました") }
            }
        }
    }

    private suspend fun refreshSummary() {
        val date = _state.value.date
        val summary = repository.daySummary(date)
        _state.update { it.copy(summary = summary) }
    }

    override fun onCleared() {
        database.close()
        super.onCleared()
    }
}
