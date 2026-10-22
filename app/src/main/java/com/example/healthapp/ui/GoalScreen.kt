package com.example.healthapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthapp.nutrition.Nutrients
import com.example.healthapp.nutrition.achievementPercent
import com.example.healthapp.nutrition.total

@Composable
fun GoalScreen(viewModel: GoalViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("目標・集計", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("1日あたりの目標値", style = MaterialTheme.typography.titleMedium)
                GOAL_LABELS.forEachIndexed { index, label ->
                    OutlinedTextField(
                        value = state.fields[index],
                        onValueChange = { viewModel.setField(index, it) },
                        label = { Text(label) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Button(onClick = viewModel::save, modifier = Modifier.fillMaxWidth()) {
                    Text("目標を保存")
                }
                state.message?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        state.today?.let { summary ->
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        "今日の達成状況（${summary.date.toDisplay()}）",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    val goal = state.goal
                    val actual = summary.total
                    ProgressRow("エネルギー", actual.kcal, goal.kcal, "kcal")
                    ProgressRow("たんぱく質", actual.proteinG, goal.proteinG, "g")
                    ProgressRow("脂質", actual.fatG, goal.fatG, "g")
                    ProgressRow("炭水化物", actual.carbsG, goal.carbsG, "g")
                    ProgressRow("食物繊維", actual.fiberG, goal.fiberG, "g")
                    ProgressRow("ナトリウム", actual.sodiumMg, goal.sodiumMg, "mg")
                    Text(
                        "運動による消費: ${state.burnedKcal.oneDecimal()} kcal",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("直近7日間（記録のある日）", style = MaterialTheme.typography.titleMedium)
                if (state.recent.isEmpty()) {
                    Text("記録がありません", color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    val average = state.recent.map { it.total }.total()
                    val count = state.recent.size
                    Text(
                        "記録日の平均: ${(average.kcal / count).oneDecimal()} kcal",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    state.recent.forEach { day ->
                        Column {
                            Text(day.date.toDisplay(), style = MaterialTheme.typography.titleSmall)
                            Text(
                                day.total.summaryLine(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressRow(label: String, actual: Double, goal: Double, unit: String) {
    val percent = achievementPercent(actual, goal)
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(label, modifier = Modifier.weight(1f))
            Text("${actual.oneDecimal()} / ${goal.plainText()} $unit（${percent}%）")
        }
        LinearProgressIndicator(
            progress = { (percent / 100f).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
