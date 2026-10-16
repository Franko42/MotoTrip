package com.mototrip.ui.active_trip

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

@Composable
fun AddFuelEntryDialog(
    onDismiss: () -> Unit,
    onConfirm: (
        amount: Double,
        currencyCode: String,
        liters: Double?,
        odometerKm: Double?,
        stationName: String?,
        isFullTank: Boolean
    ) -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var currencyCode by remember { mutableStateOf("ARS") }
    var litersText by remember { mutableStateOf("") }
    var odometerText by remember { mutableStateOf("") }
    var stationName by remember { mutableStateOf("") }
    var isFullTank by remember { mutableStateOf(false) }

    val isValid = (amountText.toDoubleOrNull() ?: -1.0) >= 0.0

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(text = "Carga de combustible") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = amountText,
                    onValueChange = { amountText = it },
                    label = { Text("Importe total (*)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = litersText,
                    onValueChange = { litersText = it },
                    label = { Text("Litros cargados (opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = odometerText,
                    onValueChange = { odometerText = it },
                    label = { Text("Odómetro actual en km (opcional)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = stationName,
                    onValueChange = { stationName = it },
                    label = { Text("Estación / Bandera (opcional)") },
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Checkbox(
                        checked = isFullTank,
                        onCheckedChange = { isFullTank = it }
                    )
                    Text("¿Fue tanque lleno?")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = isValid,
                onClick = {
                    val amount = amountText.toDoubleOrNull() ?: 0.0
                    val liters = litersText.toDoubleOrNull()?.takeIf { it > 0 }
                    val odo = odometerText.toDoubleOrNull()?.takeIf { it >= 0 }
                    onConfirm(amount, currencyCode, liters, odo, stationName, isFullTank)
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}
