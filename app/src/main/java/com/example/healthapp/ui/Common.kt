package com.example.healthapp.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import java.time.LocalDate

/** 前日・翌日へ移動できる日付の行。 */
@Composable
fun DateRow(date: LocalDate, onChange: (LocalDate) -> Unit) {
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
