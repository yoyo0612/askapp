package com.example.healthapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.HealthApp
import com.example.healthapp.data.BodyRepository
import com.example.healthapp.data.WeightEntry
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class WeightUiState(
    val date: LocalDate = LocalDate.now(),
    val kgText: String = "",
    /** 日付の昇順。 */
    val entries: List<WeightEntry> = emptyList(),
    val message: String? = null,
)

class WeightViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = BodyRepository((application as HealthApp).database)

    private val _state = MutableStateFlow(WeightUiState())
    val state: StateFlow<WeightUiState> = _state.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            val entries = repository.listWeights()
            _state.update { it.copy(entries = entries) }
        }
    }

    /** 日付を変えたら、その日の記録があれば入力欄に読み込む。 */
    fun setDate(date: LocalDate) {
        _state.update { current ->
            current.copy(
                date = date,
                kgText = current.entries.firstOrNull { it.date == date }?.kg?.toString().orEmpty(),
                message = null,
            )
        }
    }

    fun setKgText(text: String) {
        _state.update { it.copy(kgText = text) }
    }

    fun save() {
        val current = _state.value
        val kg = current.kgText.toDoubleOrNull()
        if (kg == null || kg !in MIN_KG..MAX_KG) {
            _state.update { it.copy(message = "体重は${MIN_KG.toInt()}〜${MAX_KG.toInt()}kgの数字で入力してください") }
            return
        }
        viewModelScope.launch {
            repository.saveWeight(WeightEntry(current.date, kg))
            _state.update { it.copy(message = "保存しました") }
            reload()
        }
    }

    fun delete(date: LocalDate) {
        viewModelScope.launch {
            repository.deleteWeight(date)
            _state.update { it.copy(message = "削除しました") }
            reload()
        }
    }

    private companion object {
        const val MIN_KG = 20.0
        const val MAX_KG = 300.0
    }
}
