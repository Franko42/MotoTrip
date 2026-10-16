package com.mototrip.ui.active_trip

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
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mototrip.data.local.entity.TripStatus
import com.mototrip.service.TripTrackingService
import com.mototrip.ui.maps.TripMapView
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val TimeFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveTripScreen(
    viewModel: ActiveTripViewModel,
    onNavigateBack: () -> Unit,
    onTripFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val tripDetail by viewModel.tripDetail.collectAsState()
    val trackPoints by viewModel.trackPoints.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showAddStopDialog by remember { mutableStateOf(false) }
    var showAddFuelDialog by remember { mutableStateOf(false) }
    var showAddExpenseDialog by remember { mutableStateOf(false) }
    var showFinishDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.tripFinishedEvent.collect {
            onTripFinished()
        }
    }

    LaunchedEffect(Unit) {
        viewModel.userMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    val detail = tripDetail

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail?.trip?.name ?: "Viaje en curso") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
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
                // Cockpit Telemetry HUD
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (trip.status == TripStatus.ACTIVE) "● REGISTRANDO GPS" else "⏸ PAUSADO",
                                style = MaterialTheme.typography.titleMedium,
                                color = if (trip.status == TripStatus.ACTIVE)
                                    MaterialTheme.colorScheme.tertiary
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                            SuggestionChip(
                                onClick = {},
                                label = { Text("${trackPoints.size} pts") }
                            )
                        }

                        // Glanceable Distance
                        val distKm = trip.distanceMeters / 1000.0
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = String.format(Locale.getDefault(), "%.1f", distKm),
                                style = MaterialTheme.typography.displayMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "km recorridos",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(bottom = 6.dp)
                            )
                        }

                        val startTimeStr = Instant.ofEpochMilli(trip.startedAt)
                            .atZone(ZoneId.systemDefault())
                            .format(TimeFormatter)

                        Text(
                            text = "Inicio: $startTimeStr${if (!trip.originName.isNullOrBlank()) " · Desde ${trip.originName}" else ""}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                // Live Map Preview of GPS track
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(240.dp)
                ) {
                    TripMapView(
                        trackPoints = trackPoints,
                        stops = detail.stops,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Main trip controls (Large 56dp glove-friendly touch targets)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (trip.status == TripStatus.ACTIVE) {
                        FilledTonalButton(
                            onClick = {
                                viewModel.pauseTrip()
                                TripTrackingService.pauseTracking(context, trip.id)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Text("Pausar", style = MaterialTheme.typography.titleMedium)
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.resumeTrip()
                                TripTrackingService.resumeTracking(context, trip.id)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                        ) {
                            Text("Reanudar", style = MaterialTheme.typography.titleMedium)
                        }
                    }

                    Button(
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error
                        ),
                        onClick = { showFinishDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                    ) {
                        Text("Finalizar", style = MaterialTheme.typography.titleMedium)
                    }
                }

                Text(
                    text = "Acciones en ruta",
                    style = MaterialTheme.typography.titleMedium
                )

                // Quick Logging buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedButton(
                        onClick = { showAddStopDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("+ Parada")
                    }
                    OutlinedButton(
                        onClick = { showAddFuelDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("+ Nafta")
                    }
                    OutlinedButton(
                        onClick = { showAddExpenseDialog = true },
                        modifier = Modifier
                            .weight(1f)
                            .height(48.dp)
                    ) {
                        Text("+ Gasto")
                    }
                }

                // Summary of entries registered so far
                Text(
                    text = "Registros de este viaje",
                    style = MaterialTheme.typography.titleMedium
                )

                // Stops
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Paradas manuales (${detail.stops.size})",
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (detail.stops.isEmpty()) {
                            Text("No has registrado paradas todavía.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            for (stop in detail.stops) {
                                val time = Instant.ofEpochMilli(stop.arrivedAt)
                                    .atZone(ZoneId.systemDefault())
                                    .format(TimeFormatter)
                                Text("• [$time] ${stop.type.name} - ${stop.name ?: "Sin nombre"}")
                            }
                        }
                    }
                }

                // Fuel
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Combustible (${detail.fuelEntries.size})",
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (detail.fuelEntries.isEmpty()) {
                            Text("No hay cargas registradas.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            for (f in detail.fuelEntries) {
                                val amountFormatted = f.amountMinorUnits / 100.0
                                val litersStr = f.liters?.let { " ($it L)" } ?: ""
                                Text("• ${f.currencyCode} $amountFormatted$litersStr - ${f.stationName ?: "Estación"}")
                            }
                        }
                    }
                }

                // Other Expenses
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Otros gastos (${detail.expenses.size})",
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (detail.expenses.isEmpty()) {
                            Text("No hay gastos registrados.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            for (exp in detail.expenses) {
                                val amountFormatted = exp.amountMinorUnits / 100.0
                                Text("• [${exp.category}] ${exp.currencyCode} $amountFormatted ${exp.description ?: ""}")
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddStopDialog) {
        AddStopDialog(
            onDismiss = { showAddStopDialog = false },
            onConfirm = { type, name, notes ->
                showAddStopDialog = false
                viewModel.addStop(type, name, notes)
            }
        )
    }

    if (showAddFuelDialog) {
        AddFuelEntryDialog(
            onDismiss = { showAddFuelDialog = false },
            onConfirm = { amount, currency, liters, odo, station, isFull ->
                showAddFuelDialog = false
                viewModel.addFuelEntry(amount, currency, liters, odo, station, isFull, null)
            }
        )
    }

    if (showAddExpenseDialog) {
        AddExpenseDialog(
            onDismiss = { showAddExpenseDialog = false },
            onConfirm = { category, amount, currency, desc ->
                showAddExpenseDialog = false
                viewModel.addExpense(category, amount, currency, desc, null)
            }
        )
    }

    if (showFinishDialog) {
        FinishTripDialog(
            onDismiss = { showFinishDialog = false },
            onConfirm = { distanceKm ->
                showFinishDialog = false
                viewModel.finishTrip(distanceKm)
                TripTrackingService.stopTracking(context)
            }
        )
    }
}
