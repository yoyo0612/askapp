package com.example.healthapp.ui

import android.app.Application
import android.database.sqlite.SQLiteException
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.HealthApp
import com.example.healthapp.data.Food
import com.example.healthapp.data.FoodRepository
import com.example.healthapp.data.OpenFoodFactsClient
import com.example.healthapp.nutrition.Nutrients
import java.io.IOException
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
    val barcode: String? = null,
)

data class FoodUiState(
    val foods: List<Food> = emptyList(),
    val query: String = "",
    val barcodeInput: String = "",
    val loading: Boolean = false,
    val form: FoodForm? = null,
    val message: String? = null,
)

class FoodViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = FoodRepository((application as HealthApp).database)
    private val client = OpenFoodFactsClient()

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

    fun setBarcodeInput(text: String) {
        _state.update { it.copy(barcodeInput = text) }
    }

    fun showMessage(text: String) {
        _state.update { it.copy(message = text) }
    }

    /**
     * バーコードから食品を探す。
     * 1. 端末に登録済みならその食品を開く
     * 2. なければ Open Food Facts で商品情報を取得し、確認用のフォームに入れる
     * 3. 見つからなければ、バーコード入りの空のフォームを開く
     */
    fun lookupBarcode(raw: String) {
        val code = raw.trim()
        if (!BARCODE_PATTERN.matches(code)) {
            _state.update { it.copy(message = "バーコードは8〜14桁の数字で入力してください") }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(loading = true, message = null) }
            try {
                val registered = repository.findByBarcode(code)
                if (registered != null) {
                    _state.update {
                        it.copy(loading = false, form = registered.toForm(), message = "登録済みの食品です")
                    }
                    return@launch
                }

                val product = client.fetch(code)
                val form = if (product == null) {
                    FoodForm(barcode = code)
                } else {
                    FoodForm(
                        name = uniqueName(product.name, code),
                        kcal = product.per100g.kcal.plainText(),
                        protein = product.per100g.proteinG.plainText(),
                        fat = product.per100g.fatG.plainText(),
                        carbs = product.per100g.carbsG.plainText(),
                        fiber = product.per100g.fiberG.plainText(),
                        sodium = product.per100g.sodiumMg.plainText(),
                        barcode = code,
                    )
                }
                val message = when {
                    product == null -> "商品が見つかりませんでした。手入力で追加できます"
                    !product.complete -> "商品情報を取得しました。一部の栄養素は未登録のため0になっています。確認してください"
                    else -> "商品情報を取得しました。内容を確認して保存してください"
                }
                _state.update { it.copy(loading = false, form = form, message = message) }
            } catch (e: IOException) {
                _state.update { it.copy(loading = false, message = "通信に失敗しました。電波状況を確認してください") }
            }
        }
    }

    fun openNew() {
        _state.update { it.copy(form = FoodForm(), message = null) }
    }

    fun openEdit(food: Food) {
        _state.update { it.copy(form = food.toForm(), message = null) }
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
            barcode = form.barcode?.takeIf { it.isNotBlank() },
        )
        viewModelScope.launch {
            try {
                if (form.id == null) repository.add(food) else repository.update(food)
                _state.update { it.copy(form = null, message = "保存しました") }
                reload()
            } catch (e: SQLiteException) {
                _state.update {
                    it.copy(message = "保存できませんでした（同じ名前・同じバーコードの食品があるかもしれません）")
                }
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

    /** 同じ名前の食品が既にあれば、バーコードを付けて区別する。 */
    private suspend fun uniqueName(name: String, barcode: String): String {
        val exists = repository.list().any { it.name == name }
        return if (exists) "$name（$barcode）" else name
    }

    private fun Food.toForm(): FoodForm = FoodForm(
        id = id,
        name = name,
        kcal = per100g.kcal.plainText(),
        protein = per100g.proteinG.plainText(),
        fat = per100g.fatG.plainText(),
        carbs = per100g.carbsG.plainText(),
        fiber = per100g.fiberG.plainText(),
        sodium = per100g.sodiumMg.plainText(),
        barcode = barcode,
    )

    private companion object {
        val BARCODE_PATTERN = Regex("\\d{8,14}")
    }
}
