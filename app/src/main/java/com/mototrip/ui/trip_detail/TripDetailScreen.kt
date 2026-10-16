package com.mototrip.ui.trip_detail

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.mototrip.domain.calculations.TripCalculations
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    viewModel: TripDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.tripDeletedEvent.collect {
            onNavigateBack()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val detail = uiState.detail

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.trip?.name ?: "Detalle del viaje") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    if (detail != null) {
                        IconButton(onClick = { showDeleteConfirmDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Eliminar viaje",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        modifier = modifier.fillMaxSize()
    ) { innerPadding ->
        if (detail == null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                CircularProgressIndicator()
            }
        } else {
            val trip = detail.trip
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Trip Header Card
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = trip.name,
                                style = MaterialTheme.typography.headlineSmall
                            )
                            SuggestionChip(
                                onClick = {},
                                label = { Text(trip.status.name) }
                            )
                        }

                        if (!trip.originName.isNullOrBlank() || !trip.destinationName.isNullOrBlank()) {
                            Text(
                                text = "Recorrido: ${trip.originName ?: "?"}  ➔  ${trip.destinationName ?: "?"}",
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }

                        val startStr = Instant.ofEpochMilli(trip.startedAt)
                            .atZone(ZoneId.systemDefault())
                            .format(DateTimeFormat)

                        Text(
                            text = "Inicio: $startStr",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        trip.endedAt?.let { endMs ->
                            val endStr = Instant.ofEpochMilli(endMs)
                                .atZone(ZoneId.systemDefault())
                                .format(DateTimeFormat)
                            Text(
                                text = "Fin: $endStr",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        // Active duration
                        val totalMinutes = uiState.activeDurationMillis / (1000 * 60)
                        val hours = totalMinutes / 60
                        val mins = totalMinutes % 60
                        Text(
                            text = "Tiempo en marcha (sin pausas): ${hours}h ${mins}m",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        // Distance
                        val distKm = trip.distanceMeters / 1000.0
                        Text(
                            text = "Distancia: $distKm km",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        // Cost per km
                        uiState.costPerKm?.let { cost ->
                            val primaryCurrency = uiState.moneyTotals.firstOrNull()?.currencyCode ?: ""
                            Text(
                                text = String.format(Locale.getDefault(), "Costo estimado: %.2f %s / km", cost, primaryCurrency),
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Map visualization Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    com.mototrip.ui.maps.TripMapView(
                        trackPoints = uiState.trackPoints,
                        stops = detail.stops,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Financial Totals Card
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Resumen de gastos por moneda",
                            style = MaterialTheme.typography.titleMedium
                        )

                        if (uiState.moneyTotals.isEmpty()) {
                            Text("No se registraron gastos en este viaje.", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            uiState.moneyTotals.forEach { total ->
                                val fuel = total.fuelAmountMinorUnits / 100.0
                                val other = total.otherExpensesMinorUnits / 100.0
                                val grandTotal = total.totalAmountMinorUnits / 100.0

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = "Moneda: ${total.currencyCode}",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text(
                                        text = "• Combustible: ${total.currencyCode} $fuel",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "• Otros gastos: ${total.currencyCode} $other",
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                    Text(
                                        text = "• Total: ${total.currencyCode} $grandTotal",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }
                }

                // Stops Section
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Paradas manuales (${detail.stops.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (detail.stops.isEmpty()) {
                            Text("No se registraron paradas.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            detail.stops.forEach { stop ->
                                val time = Instant.ofEpochMilli(stop.arrivedAt)
                                    .atZone(ZoneId.systemDefault())
                                    .format(DateTimeFormat)
                                Text("• [$time] ${stop.type.name}: ${stop.name ?: "Sin nombre"}")
                                if (!stop.notes.isNullOrBlank()) {
                                    Text("   Notas: ${stop.notes}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // Fuel Section
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Cargas de combustible (${detail.fuelEntries.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (detail.fuelEntries.isEmpty()) {
                            Text("No hay cargas de combustible.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            detail.fuelEntries.forEach { f ->
                                val amount = f.amountMinorUnits / 100.0
                                val pricePerLiter = TripCalculations.calculatePricePerLiter(f.amountMinorUnits, f.liters)
                                val priceStr = pricePerLiter?.let { String.format(Locale.getDefault(), " (%.2f %s/L)", it, f.currencyCode) } ?: ""
                                val odoStr = f.odometerKm?.let { " - Odómetro: $it km" } ?: ""
                                val fullTankStr = if (f.isFullTank) " [Tanque lleno]" else ""

                                Text("• ${f.currencyCode} $amount - ${f.liters ?: "?"} L$priceStr$odoStr$fullTankStr")
                                if (!f.stationName.isNullOrBlank()) {
                                    Text("   Estación: ${f.stationName}", style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }

                // Expenses Section
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Otros gastos (${detail.expenses.size})",
                            style = MaterialTheme.typography.titleMedium
                        )
                        if (detail.expenses.isEmpty()) {
                            Text("No hay otros gastos.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            detail.expenses.forEach { exp ->
                                val amount = exp.amountMinorUnits / 100.0
                                Text("• [${exp.category}] ${exp.currencyCode} $amount: ${exp.description ?: ""}")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showDeleteConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("¿Eliminar viaje?") },
            text = {
                Text("Esta acción eliminará el viaje y todas sus paradas, cargas de combustible y gastos asociados. Esta acción no se puede deshacer.")
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        showDeleteConfirmDialog = false
                        viewModel.deleteTrip()
                    }
                ) {
                    Text("Eliminar definitivamente")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
