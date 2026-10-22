package com.example.healthapp.ui

import android.app.Application
import android.database.sqlite.SQLiteException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.HealthApp
import com.example.healthapp.data.Food
import com.example.healthapp.data.FoodRepository
import com.example.healthapp.nutrition.Nutrients
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** 編集ダイアログの入力内容。id が null のときは新規追加。 */
data class FoodForm(
    val id: Long? = null,
    val name: String = "",
    val kcal: String = "",
    val protein: String = "",
    val fat: String = "",
    val carbs: String = "",
    val fiber: String = "",
    val sodium: String = "",
)

data class FoodUiState(
    val foods: List<Food> = emptyList(),
    val query: String = "",
    val form: FoodForm? = null,
    val message: String? = null,
)

class FoodViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FoodRepository((application as HealthApp).database)

    private val _state = MutableStateFlow(FoodUiState())
    val state: StateFlow<FoodUiState> = _state.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            val foods = repository.list()
            _state.update { it.copy(foods = foods) }
        }
    }

    fun setQuery(query: String) {
        _state.update { it.copy(query = query) }
    }

    fun openNew() {
        _state.update { it.copy(form = FoodForm(), message = null) }
    }

    fun openEdit(food: Food) {
        val nutrients = food.per100g
        _state.update {
            it.copy(
                form = FoodForm(
                    id = food.id,
                    name = food.name,
                    kcal = nutrients.kcal.plainText(),
                    protein = nutrients.proteinG.plainText(),
                    fat = nutrients.fatG.plainText(),
                    carbs = nutrients.carbsG.plainText(),
                    fiber = nutrients.fiberG.plainText(),
                    sodium = nutrients.sodiumMg.plainText(),
                ),
                message = null,
            )
        }
    }

    fun updateForm(form: FoodForm) {
        _state.update { it.copy(form = form) }
    }

    fun closeForm() {
        _state.update { it.copy(form = null) }
    }

    fun saveForm() {
        val form = _state.value.form ?: return
        val name = form.name.trim()
        val numbers = listOf(form.kcal, form.protein, form.fat, form.carbs, form.fiber, form.sodium)
            .map { text -> text.toDoubleOrNull()?.takeIf { it >= 0 } }
        if (name.isEmpty() || numbers.any { it == null }) {
            _state.update { it.copy(message = "食品名と各栄養素（0以上の数字）を入力してください") }
            return
        }
        val n = numbers.map { it!! }
        val food = Food(
            id = form.id ?: 0,
            name = name,
            per100g = Nutrients(n[0], n[1], n[2], n[3], n[4], n[5]),
        )
        viewModelScope.launch {
            try {
                if (form.id == null) repository.add(food) else repository.update(food)
                _state.update { it.copy(form = null, message = "保存しました") }
                reload()
            } catch (e: SQLiteException) {
                _state.update { it.copy(message = "保存できませんでした（同じ名前の食品があるかもしれません）") }
            }
        }
    }

    fun deleteForm() {
        val id = _state.value.form?.id ?: return
        viewModelScope.launch {
            try {
                repository.delete(id)
                _state.update { it.copy(form = null, message = "削除しました") }
                reload()
            } catch (e: SQLiteException) {
                _state.update {
                    it.copy(message = "この食品は食事の記録で使われているため削除できません")
                }
            }
        }
    }
}
