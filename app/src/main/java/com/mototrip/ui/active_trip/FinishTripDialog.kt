package com.mototrip.ui.active_trip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun FinishTripDialog(
    onDismiss: () -> Unit,
    onConfirm: (distanceKm: Double) -> Unit
) {
    var distanceText by remember { mutableStateOf("") }
    val isValid = (distanceText.toDoubleOrNull() ?: -1.0) >= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Finalizar viaje") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "¿Deseas dar por terminado este viaje? Podrás registrar los kilómetros recorridos según el odómetro de tu moto.",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = distanceText,
                    onValueChange = { distanceText = it },
                    label = { Text("Distancia total en km") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                enabled = isValid,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                onClick = {
                    val distance = distanceText.toDoubleOrNull() ?: 0.0
                    onConfirm(distance)
                }
            ) {
                Text("Finalizar viaje")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
