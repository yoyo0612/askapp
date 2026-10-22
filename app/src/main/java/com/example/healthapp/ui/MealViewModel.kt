package com.example.healthapp.ui

import android.app.Application
import android.database.sqlite.SQLiteException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.HealthApp
import com.example.healthapp.data.DaySummary
import com.example.healthapp.data.FoodRepository
import com.example.healthapp.data.MealDetail
import com.example.healthapp.data.MealItemInput
import com.example.healthapp.data.MealRepository
import com.example.healthapp.data.MealType
import com.example.healthapp.data.Food
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
    /** 編集中の食事の id。null のときは新規記録。 */
    val editingMealId: Long? = null,
    val summary: DaySummary? = null,
    val message: String? = null,
)

class MealViewModel(application: Application) : AndroidViewModel(application) {

    private val database = (application as HealthApp).database
    private val foodRepository = FoodRepository(database)
    private val mealRepository = MealRepository(database)

    private val _state = MutableStateFlow(MealUiState())
    val state: StateFlow<MealUiState> = _state.asStateFlow()

    init {
        reload()
    }

    /** 食品一覧と表示中の日の集計を読み直す。食品タブで変更があった後にも呼ぶ。 */
    fun reload() {
        viewModelScope.launch {
            val foods = foodRepository.list()
            _state.update { current ->
                val stillExists = foods.any { it.id == current.selectedFoodId }
                current.copy(
                    foods = foods,
                    selectedFoodId = if (stillExists) current.selectedFoodId else foods.firstOrNull()?.id,
                )
            }
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

    /** 記録済みの食事を編集用に読み込む。 */
    fun startEdit(meal: MealDetail) {
        _state.update {
            it.copy(
                editingMealId = meal.id,
                mealType = meal.mealType,
                draft = meal.items.map { item -> DraftItem(item.foodId, item.name, item.grams) },
                message = null,
            )
        }
    }

    fun cancelEdit() {
        _state.update { it.copy(editingMealId = null, draft = emptyList(), message = null) }
    }

    fun saveMeal() {
        val current = _state.value
        if (current.draft.isEmpty()) {
            _state.update { it.copy(message = "食品を1つ以上追加してください") }
            return
        }
        viewModelScope.launch {
            val items = current.draft.map { MealItemInput(it.foodId, it.grams) }
            val editingId = current.editingMealId
            try {
                if (editingId == null) {
                    mealRepository.addMeal(current.date, current.mealType, items)
                } else {
                    mealRepository.updateMeal(editingId, current.date, current.mealType, items)
                }
                _state.update {
                    it.copy(
                        draft = emptyList(),
                        editingMealId = null,
                        message = if (editingId == null) "記録しました" else "変更を保存しました",
                    )
                }
                refreshSummary()
            } catch (e: SQLiteException) {
                _state.update { it.copy(message = "保存に失敗しました") }
            }
        }
    }

    fun deleteMeal(mealId: Long) {
        viewModelScope.launch {
            mealRepository.deleteMeal(mealId)
            _state.update {
                it.copy(
                    editingMealId = if (it.editingMealId == mealId) null else it.editingMealId,
                    draft = if (it.editingMealId == mealId) emptyList() else it.draft,
                    message = "削除しました",
                )
            }
            refreshSummary()
        }
    }

    private suspend fun refreshSummary() {
        val summary = mealRepository.daySummary(_state.value.date)
        _state.update { it.copy(summary = summary) }
    }
}
