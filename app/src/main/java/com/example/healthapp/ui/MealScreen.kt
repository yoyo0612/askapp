package com.example.healthapp.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.MenuAnchorType
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthapp.data.DaySummary
import com.example.healthapp.data.MealDetail
import com.example.healthapp.data.MealType

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MealScreen(viewModel: MealViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("食事記録・栄養計算", style = MaterialTheme.typography.headlineSmall)

            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text("食事を追加", style = MaterialTheme.typography.titleMedium)

                    DateRow(
                        date = state.date,
                        onChange = viewModel::setDate,
                    )

                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        MealType.entries.forEach { type ->
                            FilterChip(
                                selected = state.mealType == type,
                                onClick = { viewModel.setMealType(type) },
                                label = { Text(type.label) },
                            )
                        }
                    }

                    FoodDropdown(
                        foods = state.foods,
                        selectedFoodId = state.selectedFoodId,
                        onSelect = viewModel::selectFood,
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = state.gramsText,
                            onValueChange = viewModel::setGrams,
                            label = { Text("量 (g)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.width(120.dp),
                        )
                        Button(onClick = viewModel::addDraftItem) {
                            Text("リストに追加")
                        }
                    }

                    DraftList(
                        draft = state.draft,
                        onRemove = viewModel::removeDraftItem,
                    )

                    Button(
                        onClick = viewModel::saveMeal,
                        enabled = state.draft.isNotEmpty(),
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("この食事を記録する")
                    }

                    state.message?.let {
                        Text(it, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            state.summary?.let { SummarySection(it) }
        }
    }
}

@Composable
private fun DateRow(date: java.time.LocalDate, onChange: (java.time.LocalDate) -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedButton(onClick = { onChange(date.minusDays(1)) }) { Text("◀ 前日") }
        Text(
            date.toDisplay(),
            modifier = Modifier.weight(1f),
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.Bold,
        )
        OutlinedButton(onClick = { onChange(date.plusDays(1)) }) { Text("翌日 ▶") }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FoodDropdown(
    foods: List<com.example.healthapp.data.Food>,
    selectedFoodId: Long?,
    onSelect: (Long) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedName = foods.firstOrNull { it.id == selectedFoodId }?.name.orEmpty()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
    ) {
        OutlinedTextField(
            value = selectedName,
            onValueChange = {},
            readOnly = true,
            label = { Text("食品") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier = Modifier
                .menuAnchor(MenuAnchorType.PrimaryNotEditable)
                .fillMaxWidth(),
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            foods.forEach { food ->
                DropdownMenuItem(
                    text = { Text(food.name) },
                    onClick = {
                        onSelect(food.id)
                        expanded = false
                    },
                )
            }
        }
    }
}

@Composable
private fun DraftList(draft: List<DraftItem>, onRemove: (Int) -> Unit) {
    if (draft.isEmpty()) {
        Text("まだ食品が追加されていません", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    Column {
        draft.forEachIndexed { index, item ->
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    "${item.name}  ${item.grams.gramsText()}g",
                    modifier = Modifier.weight(1f),
                )
                TextButton(onClick = { onRemove(index) }) { Text("削除") }
            }
        }
    }
}

@Composable
private fun SummarySection(summary: DaySummary) {
    Text(
        "${summary.date.toDisplay()} の記録",
        style = MaterialTheme.typography.titleMedium,
    )
    if (summary.meals.isEmpty()) {
        Text("この日の記録はまだありません", color = MaterialTheme.colorScheme.onSurfaceVariant)
        return
    }
    summary.meals.forEach { MealCard(it) }
    TotalCard(summary)
}

@Composable
private fun MealCard(meal: MealDetail) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(meal.mealType.label, style = MaterialTheme.typography.titleSmall)
            meal.items.forEach { item ->
                Column {
                    Text("${item.name}  ${item.grams.gramsText()}g")
                    Text(
                        item.nutrients.summaryLine(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            HorizontalDivider()
            Text("小計: ${meal.total.summaryLine()}", style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
private fun TotalCard(summary: DaySummary) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text("1日の合計", style = MaterialTheme.typography.titleSmall)
            Text(summary.total.summaryLine(), style = MaterialTheme.typography.bodyMedium)
        }
    }
}
