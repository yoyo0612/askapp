package com.example.healthapp.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.healthapp.HealthApp
import com.example.healthapp.data.BodyRepository
import com.example.healthapp.data.EXERCISE_TYPES
import com.example.healthapp.data.ExerciseEntry
import com.example.healthapp.data.ExerciseRepository
import com.example.healthapp.data.ExerciseType
import com.example.healthapp.nutrition.DEFAULT_WEIGHT_KG
import com.example.healthapp.nutrition.exerciseKcal
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ExerciseUiState(
    val date: LocalDate = LocalDate.now(),
    val selectedName: String = EXERCISE_TYPES.first().name,
    val minutesText: String = "30",
    val entries: List<ExerciseEntry> = emptyList(),
    /** その日に使う体重（直前の記録）。記録がなければ null で、既定値を使う。 */
    val weightKg: Double? = null,
    val message: String? = null,
) {
    val selectedType: ExerciseType
        get() = EXERCISE_TYPES.first { it.name == selectedName }

    /** 記録する前に表示する目安の消費カロリー。 */
    val previewKcal: Double?
        get() {
            val minutes = minutesText.toDoubleOrNull()?.takeIf { it > 0 } ?: return null
            return exerciseKcal(selectedType.met, weightKg ?: DEFAULT_WEIGHT_KG, minutes)
        }

    val totalKcal: Double
        get() = entries.sumOf { it.kcal }
}

class ExerciseViewModel(application: Application) : AndroidViewModel(application) {

    private val database = (application as HealthApp).database
    private val exerciseRepository = ExerciseRepository(database)
    private val bodyRepository = BodyRepository(database)

    private val _state = MutableStateFlow(ExerciseUiState())
    val state: StateFlow<ExerciseUiState> = _state.asStateFlow()

    init {
        reload()
    }

    fun reload() {
        viewModelScope.launch {
            val date = _state.value.date
            val entries = exerciseRepository.listFor(date)
            val weight = bodyRepository.weightOn(date)
            _state.update { it.copy(entries = entries, weightKg = weight) }
        }
    }

    fun setDate(date: LocalDate) {
        _state.update { it.copy(date = date, message = null) }
        reload()
    }

    fun selectType(name: String) {
        _state.update { it.copy(selectedName = name) }
    }

    fun setMinutes(text: String) {
        _state.update { it.copy(minutesText = text) }
    }

    fun add() {
        val current = _state.value
        val minutes = current.minutesText.toDoubleOrNull()
        if (minutes == null || minutes <= 0) {
            _state.update { it.copy(message = "時間は1分以上の数字で入力してください") }
            return
        }
        val type = current.selectedType
        val kcal = exerciseKcal(type.met, current.weightKg ?: DEFAULT_WEIGHT_KG, minutes)
        viewModelScope.launch {
            exerciseRepository.add(
                ExerciseEntry(id = 0, date = current.date, name = type.name, minutes = minutes, kcal = kcal),
            )
            _state.update { it.copy(message = "記録しました") }
            reload()
        }
    }

    fun delete(id: Long) {
        viewModelScope.launch {
            exerciseRepository.delete(id)
            _state.update { it.copy(message = "削除しました") }
            reload()
        }
    }
}
