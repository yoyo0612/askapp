package com.example.healthapp.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.healthapp.data.Food

@Composable
fun FoodScreen(viewModel: FoodViewModel = viewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = viewModel::openNew) {
                Icon(Icons.Filled.Add, contentDescription = "食品を追加")
            }
        },
    ) { innerPadding ->
        val visible = state.foods.filter { it.name.contains(state.query.trim(), ignoreCase = true) }

        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("食品一覧（100gあたり）", style = MaterialTheme.typography.headlineSmall)

            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::setQuery,
                label = { Text("検索") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )

            state.message?.let {
                Text(it, color = MaterialTheme.colorScheme.primary)
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (visible.isEmpty()) {
                    Text("該当する食品がありません", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                visible.forEach { food ->
                    FoodRow(food = food, onClick = { viewModel.openEdit(food) })
                }
            }
        }
    }

    state.form?.let { form ->
        FoodFormDialog(
            form = form,
            onChange = viewModel::updateForm,
            onSave = viewModel::saveForm,
            onDelete = viewModel::deleteForm,
            onDismiss = viewModel::closeForm,
        )
    }
}

@Composable
private fun FoodRow(food: Food, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(food.name, style = MaterialTheme.typography.titleSmall)
            Text(
                food.per100g.summaryLine(),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun FoodFormDialog(
    form: FoodForm,
    onChange: (FoodForm) -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (form.id == null) "食品を追加" else "食品を編集") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("すべて100gあたりの値", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(
                    value = form.name,
                    onValueChange = { onChange(form.copy(name = it)) },
                    label = { Text("食品名") },
                    singleLine = true,
                )
                NumberField("エネルギー(kcal)", form.kcal) { onChange(form.copy(kcal = it)) }
                NumberField("たんぱく質(g)", form.protein) { onChange(form.copy(protein = it)) }
                NumberField("脂質(g)", form.fat) { onChange(form.copy(fat = it)) }
                NumberField("炭水化物(g)", form.carbs) { onChange(form.copy(carbs = it)) }
                NumberField("食物繊維(g)", form.fiber) { onChange(form.copy(fiber = it)) }
                NumberField("ナトリウム(mg)", form.sodium) { onChange(form.copy(sodium = it)) }
            }
        },
        confirmButton = {
            TextButton(onClick = onSave) { Text("保存") }
        },
        dismissButton = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (form.id != null) {
                    TextButton(onClick = onDelete) {
                        Text("削除", color = MaterialTheme.colorScheme.error)
                    }
                }
                TextButton(onClick = onDismiss) { Text("キャンセル") }
            }
        },
    )
}

@Composable
private fun NumberField(label: String, value: String, onValueChange: (String) -> Unit) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
    )
}
