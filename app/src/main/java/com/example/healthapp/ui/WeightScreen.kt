package com.example.healthapp.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthapp.data.WeightEntry
import com.example.healthapp.nutrition.weightChange

@Composable
fun WeightScreen(viewModel: WeightViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val change = weightChange(state.entries.map { it.kg })

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text("体重記録", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                DateRow(date = state.date, onChange = viewModel::setDate)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OutlinedTextField(
                        value = state.kgText,
                        onValueChange = viewModel::setKgText,
                        label = { Text("体重 (kg)") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.width(160.dp),
                    )
                    Button(onClick = viewModel::save) { Text("保存") }
                }

                state.message?.let {
                    Text(it, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("推移", style = MaterialTheme.typography.titleMedium)
                if (state.entries.size < 2) {
                    Text(
                        "2件以上記録するとグラフが表示されます",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    WeightChart(
                        entries = state.entries,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                    )
                }
                state.entries.lastOrNull()?.let { latest ->
                    Text("最新: ${latest.kg.oneDecimal()} kg")
                }
                change?.let {
                    val sign = if (it > 0) "+" else ""
                    Text("最初の記録からの増減: $sign${it.oneDecimal()} kg")
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("履歴", style = MaterialTheme.typography.titleMedium)
                if (state.entries.isEmpty()) {
                    Text("まだ記録がありません", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                state.entries.asReversed().forEach { entry ->
                    HistoryRow(entry = entry, onDelete = { viewModel.delete(entry.date) })
                }
            }
        }
    }
}

@Composable
private fun HistoryRow(entry: WeightEntry, onDelete: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "${entry.date.toDisplay()}  ${entry.kg.oneDecimal()} kg",
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onDelete) {
            Text("削除", color = MaterialTheme.colorScheme.error)
        }
    }
}

@Composable
private fun WeightChart(entries: List<WeightEntry>, modifier: Modifier = Modifier) {
    val lineColor = MaterialTheme.colorScheme.primary
    Canvas(modifier = modifier) {
        val min = entries.minOf { it.kg }
        val max = entries.maxOf { it.kg }
        // 全て同じ値のときも線が描けるように最低幅を持たせる
        val range = (max - min).coerceAtLeast(1.0)
        val stepX = size.width / (entries.size - 1)

        val path = Path()
        entries.forEachIndexed { index, entry ->
            val x = index * stepX
            val y = size.height - ((entry.kg - min) / range * size.height).toFloat()
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = lineColor, style = Stroke(width = 4.dp.toPx()))
    }
}
