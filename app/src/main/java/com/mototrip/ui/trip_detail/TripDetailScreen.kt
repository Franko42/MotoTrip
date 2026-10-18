package com.mototrip.ui.trip_detail

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.mototrip.MotoTripApp
import com.mototrip.core.backup.BackupManager
import com.mototrip.data.local.entity.StopType
import com.mototrip.domain.calculations.TripCalculations
import com.mototrip.domain.model.CalculationResult
import com.mototrip.domain.model.FuelEntry
import com.mototrip.domain.model.MotorcycleConfig
import com.mototrip.domain.model.Stop
import com.mototrip.ui.maps.TripMapView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DateTimeFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

private fun formatEpochMillis(millis: Long): String {
    return Instant.ofEpochMilli(millis)
        .atZone(ZoneId.systemDefault())
        .format(DateTimeFormat)
}

private fun parseDateTimeStringToMillis(str: String): Long? {
    return runCatching {
        val ldt = LocalDateTime.parse(str.trim(), DateTimeFormat)
        ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }.getOrNull()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TripDetailScreen(
    viewModel: TripDetailViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val detail = uiState.detail
    val snackbarHostState = remember { SnackbarHostState() }

    var showDeleteConfirmDialog by remember { mutableStateOf(false) }
    var showEditTripDialog by remember { mutableStateOf(false) }
    var showEditMotoDialog by remember { mutableStateOf(false) }
    var stopToEdit by remember { mutableStateOf<Stop?>(null) }
    var fuelToEdit by remember { mutableStateOf<FuelEntry?>(null) }

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

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null && detail != null) {
            coroutineScope.launch {
                try {
                    val app = context.applicationContext as MotoTripApp
                    val db = app.appContainer.database
                    val tripEntity = db.tripDao().getTripById(detail.trip.id)
                    if (tripEntity != null) {
                        val stops = db.stopDao().getStopsByTrip(tripEntity.id)
                        val fuel = db.fuelEntryDao().getFuelEntriesByTrip(tripEntity.id)
                        val exp = db.expenseDao().getExpensesByTrip(tripEntity.id)
                        val csv = BackupManager.exportTripToCsv(tripEntity, stops, fuel, exp)

                        withContext(Dispatchers.IO) {
                            context.contentResolver.openOutputStream(uri)?.use { stream ->
                                stream.write(csv.toByteArray(Charsets.UTF_8))
                            }
                        }
                        snackbarHostState.showSnackbar("Viaje exportado a CSV con éxito")
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Error al exportar CSV: ${e.message}")
                }
            }
        }
    }

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
                        IconButton(onClick = { showEditTripDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Editar viaje",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        IconButton(onClick = { viewModel.recalculateDistance() }) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Recalcular distancia GPS",
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        TextButton(
                            onClick = {
                                val filename = "viaje_${detail.trip.name.replace(" ", "_")}_${detail.trip.startedAt}.csv"
                                exportCsvLauncher.launch(filename)
                            }
                        ) {
                            Text("CSV", color = MaterialTheme.colorScheme.onPrimaryContainer)
                        }

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

                        Text(
                            text = "Fecha de inicio del viaje: ${formatEpochMillis(trip.startedAt)}",
                            style = MaterialTheme.typography.bodyMedium
                        )

                        if (trip.endedAt != null) {
                            Text(
                                text = "Fecha de finalización: ${formatEpochMillis(trip.endedAt)}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }

                        Text(
                            text = "Fecha de registro creado: ${formatEpochMillis(trip.createdAt)}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )

                        // Distance
                        val distKm = trip.distanceMeters / 1000.0
                        Text(
                            text = "Distancia: $distKm km",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )

                        if (!trip.notes.isNullOrBlank()) {
                            Text(
                                text = "Notas: ${trip.notes}",
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }
                }

                // Economic Report Card (Priority Section 5 - Separación estricta de desembolsos y costos consumidos)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Reporte Económico del Viaje",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Text(
                            text = "Distingue desembolsos en efectivo del costo estimado de recursos consumidos.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (uiState.economicReports.isEmpty()) {
                            Text("No se registraron gastos ni cargas en este viaje.", style = MaterialTheme.typography.bodyMedium)
                        } else {
                            for (report in uiState.economicReports) {
                                val c = report.currencyCode
                                val paidFuel = report.totalPaidFuelMinorUnits / 100.0
                                val otherExp = report.totalOtherExpensesMinorUnits / 100.0
                                val totalDisbursed = report.totalDisbursedMinorUnits / 100.0

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = "Moneda: $c",
                                        style = MaterialTheme.typography.titleSmall
                                    )
                                    Text("• Dinero pagado en cargas de combustible: $c $paidFuel")
                                    Text("• Total otros gastos (peajes, comidas, etc.): $c $otherExp")
                                    Text(
                                        text = "• Total dinero desembolsado: $c $totalDisbursed",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.primary
                                    )

                                    // Fuel Consumed Cost
                                    when (val fuelCost = report.estimatedFuelConsumedCostMinorUnits) {
                                        is CalculationResult.Confirmed -> {
                                            Text("• Costo de combustible consumido: $c ${fuelCost.value / 100.0} (Confirmado)")
                                        }
                                        is CalculationResult.Estimated -> {
                                            Text("• Costo estimado del combustible consumido: $c ${fuelCost.value / 100.0} (Estimado)")
                                            Text("   ↳ ${fuelCost.limitationReason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                        is CalculationResult.Unknown -> {
                                            Text("• Costo de combustible consumido: Desconocido (${fuelCost.reason})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                                        }
                                    }

                                    // Estimated Total Trip Cost
                                    when (val totalCost = report.estimatedTotalTripCostMinorUnits) {
                                        is CalculationResult.Confirmed -> {
                                            Text("• Costo económico total del viaje: $c ${totalCost.value / 100.0}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.tertiary)
                                        }
                                        is CalculationResult.Estimated -> {
                                            Text("• Costo económico total estimado: $c ${totalCost.value / 100.0}", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.tertiary)
                                        }
                                        is CalculationResult.Unknown -> {
                                            Text("• Costo económico total: No determinable (${totalCost.reason})", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Fuel Consumption and Autonomy Card (Section 4 & 10)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Consumo y Autonomía",
                                style = MaterialTheme.typography.titleMedium
                            )
                            TextButton(onClick = { showEditMotoDialog = true }) {
                                Text("Ajustar moto")
                            }
                        }

                        val moto = uiState.motorcycleConfig
                        Text("Motocicleta: ${moto.modelName} (Tanque: ${moto.tankCapacityLiters}L provisional, Reserva: ${moto.reserveLiters}L provisional)", style = MaterialTheme.typography.bodySmall)

                        // Trip Fuel Consumption
                        when (val cons = uiState.tripFuelConsumption) {
                            is CalculationResult.Confirmed -> {
                                val eff = uiState.tripEfficiencyKmL?.valueOrNull ?: 0.0
                                Text(
                                    text = String.format(Locale.getDefault(), "• Consumo medido: %.2f L/100 km (%.2f km/L) [Confirmado]", cons.value, eff),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                cons.details?.let { Text("   ↳ $it", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline) }
                            }
                            is CalculationResult.Estimated -> {
                                val eff = uiState.tripEfficiencyKmL?.valueOrNull ?: 0.0
                                Text(
                                    text = String.format(Locale.getDefault(), "• Consumo medido: %.2f L/100 km (%.2f km/L) [Estimado]", cons.value, eff),
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Text("   ↳ ${cons.limitationReason}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.outline)
                            }
                            is CalculationResult.Unknown, null -> {
                                Text(
                                    text = "• Consumo del viaje: Desconocido (${cons?.reason ?: "Faltan datos"})",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                                Text(
                                    text = String.format(Locale.getDefault(), "   (Referencia configurada: %.2f L/100 km / %.2f km/L)", moto.referenceConsumptionL100, moto.referenceEfficiencyKmL),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }

                        // Theoretical Range
                        when (val range = uiState.theoreticalRangeKm) {
                            is CalculationResult.Confirmed -> {
                                Text(String.format(Locale.getDefault(), "• Autonomía teórica tanque lleno: %.1f km", range.value))
                            }
                            is CalculationResult.Estimated -> {
                                Text(String.format(Locale.getDefault(), "• Autonomía teórica estimada tanque lleno: %.1f km", range.value))
                            }
                            is CalculationResult.Unknown, null -> {}
                        }

                        // Range to Reserve
                        when (val rangeRes = uiState.rangeToReserveKm) {
                            is CalculationResult.Confirmed -> {
                                Text(String.format(Locale.getDefault(), "• Autonomía estimada hasta la reserva: %.1f km", rangeRes.value))
                            }
                            is CalculationResult.Estimated -> {
                                Text(String.format(Locale.getDefault(), "• Autonomía estimada hasta la reserva: %.1f km", rangeRes.value))
                            }
                            is CalculationResult.Unknown, null -> {}
                        }

                        Text(
                            text = "Aviso: Se desconoce el combustible exacto remanente en el tanque. Las autonomías expuestas son teóricas según capacidad nominal.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                // GPS & Speed Statistics Card (Section 6)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Estadísticas GPS y Velocidad",
                            style = MaterialTheme.typography.titleMedium
                        )

                        val speedStats = uiState.speedStats
                        if (speedStats == null) {
                            Text("Sin datos GPS calculados.", style = MaterialTheme.typography.bodySmall)
                        } else {
                            // Moving speed
                            when (val mov = speedStats.averageMovingSpeedKmh) {
                                is CalculationResult.Confirmed -> {
                                    Text(String.format(Locale.getDefault(), "• Velocidad media en movimiento: %.1f km/h", mov.value))
                                }
                                is CalculationResult.Estimated -> {
                                    Text(String.format(Locale.getDefault(), "• Velocidad media en movimiento: %.1f km/h (Estimada)", mov.value))
                                }
                                is CalculationResult.Unknown -> {
                                    Text("• Velocidad media en movimiento: Desconocida (${mov.reason})", style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            // Total speed
                            when (val tot = speedStats.averageTotalSpeedKmh) {
                                is CalculationResult.Confirmed -> {
                                    Text(String.format(Locale.getDefault(), "• Velocidad media total (con paradas): %.1f km/h", tot.value))
                                }
                                is CalculationResult.Estimated -> {
                                    Text(String.format(Locale.getDefault(), "• Velocidad media total (con paradas): %.1f km/h (Estimada)", tot.value))
                                }
                                is CalculationResult.Unknown -> {
                                    Text("• Velocidad media total: Desconocida (${tot.reason})", style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            // Max speed
                            when (val maxS = speedStats.maxValidSpeedKmh) {
                                is CalculationResult.Confirmed -> {
                                    Text(String.format(Locale.getDefault(), "• Velocidad máxima GPS válida: %.1f km/h", maxS.value))
                                }
                                is CalculationResult.Estimated -> {
                                    Text(String.format(Locale.getDefault(), "• Velocidad máxima GPS: %.1f km/h (Parcial: %s)", maxS.value, maxS.limitationReason))
                                }
                                is CalculationResult.Unknown -> {
                                    Text("• Velocidad máxima GPS: Desconocida (${maxS.reason})", style = MaterialTheme.typography.bodySmall)
                                }
                            }

                            Text(
                                text = "Muestras GPS válidas: ${speedStats.validPointsCount} | Descartadas por ruido/error: ${speedStats.discardedPointsCount}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )

                            if (!speedStats.coverageNotes.isNullOrBlank()) {
                                Text(
                                    text = "Cobertura: ${speedStats.coverageNotes}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                        }
                    }
                }

                // Time Breakdown Card (Section 7)
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Tiempos del Viaje",
                            style = MaterialTheme.typography.titleMedium
                        )

                        val timeStats = uiState.timeStats
                        if (timeStats != null) {
                            val totMin = timeStats.totalDurationMillis / 60000
                            val movMin = timeStats.movingDurationMillis / 60000
                            val stopMin = timeStats.confirmedStoppedDurationMillis / 60000
                            val manualStopMin = timeStats.manualStopsDurationMillis / 60000
                            val untrackedMin = timeStats.untrackedOrInterruptedDurationMillis / 60000

                            Text("• Duración total real: ${totMin / 60}h ${totMin % 60}m")
                            Text("• Tiempo en movimiento confirmado: ${movMin / 60}h ${movMin % 60}m")
                            Text("• Tiempo detenido confirmado (GPS): ${stopMin / 60}h ${stopMin % 60}m")
                            Text("• Tiempo en paradas manuales completadas: ${manualStopMin / 60}h ${manualStopMin % 60}m")
                            if (untrackedMin > 0) {
                                Text("• Tiempo con seguimiento pausado o huecos de señal: ${untrackedMin / 60}h ${untrackedMin % 60}m", color = MaterialTheme.colorScheme.outline)
                            }
                        } else {
                            val totalMinutes = uiState.activeDurationMillis / (1000 * 60)
                            Text("• Tiempo en marcha (sin pausas): ${totalMinutes / 60}h ${totalMinutes % 60}m")
                        }
                    }
                }

                // Map visualization Card
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(260.dp)
                ) {
                    TripMapView(
                        trackPoints = uiState.trackPoints,
                        stops = detail.stops,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Stops Section (Section 3)
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
                            for (stop in detail.stops) {
                                val arrTime = formatEpochMillis(stop.arrivedAt)
                                val depTime = stop.departedAt?.let { formatEpochMillis(it) } ?: "Sin salida registrada"
                                val durationMillis = TripCalculations.calculateStopDurationMillis(stop.arrivedAt, stop.departedAt)
                                val durStr = if (durationMillis != null) "${durationMillis / 60000} min" else "Duración desconocida"

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("• [${stop.type.name}] ${stop.name ?: "Sin nombre"}")
                                        Text("   Llegada: $arrTime · Salida: $depTime", style = MaterialTheme.typography.bodySmall)
                                        Text("   Duración: $durStr${if (!stop.notes.isNullOrBlank()) " · Notas: ${stop.notes}" else ""}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    }
                                    IconButton(onClick = { stopToEdit = stop }) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar parada")
                                    }
                                }
                            }
                        }
                    }
                }

                // Fuel Section (Section 4)
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
                            for (f in detail.fuelEntries) {
                                val amount = f.amountMinorUnits / 100.0
                                val pricePerLiter = TripCalculations.calculatePricePerLiter(f.amountMinorUnits, f.liters)
                                val priceStr = pricePerLiter?.let { String.format(Locale.getDefault(), " (%.2f %s/L)", it, f.currencyCode) } ?: ""
                                val odoStr = f.odometerKm?.let { " - Odómetro: $it km" } ?: ""
                                val fullTankStr = if (f.isFullTank) " [Tanque lleno]" else ""
                                val notesStr = if (!f.notes.isNullOrBlank()) " · ${f.notes}" else ""

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("• ${f.currencyCode} $amount - ${f.liters ?: "Litros no informados"}${if (f.liters != null) " L" else ""}$priceStr$odoStr$fullTankStr")
                                        Text("   Fecha: ${formatEpochMillis(f.occurredAt)}${if (!f.stationName.isNullOrBlank()) " · Estación: ${f.stationName}" else ""}$notesStr", style = MaterialTheme.typography.bodySmall)
                                    }
                                    IconButton(onClick = { fuelToEdit = f }) {
                                        Icon(imageVector = Icons.Default.Edit, contentDescription = "Editar carga")
                                    }
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
                            for (exp in detail.expenses) {
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

    if (showEditTripDialog && detail != null) {
        var name by remember { mutableStateOf(detail.trip.name) }
        var origin by remember { mutableStateOf(detail.trip.originName.orEmpty()) }
        var dest by remember { mutableStateOf(detail.trip.destinationName.orEmpty()) }
        var notes by remember { mutableStateOf(detail.trip.notes.orEmpty()) }
        var startStr by remember { mutableStateOf(formatEpochMillis(detail.trip.startedAt)) }
        var endStr by remember { mutableStateOf(detail.trip.endedAt?.let { formatEpochMillis(it) }.orEmpty()) }

        AlertDialog(
            onDismissRequest = { showEditTripDialog = false },
            title = { Text("Editar datos y fechas del viaje") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre del viaje") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = origin,
                        onValueChange = { origin = it },
                        label = { Text("Origen") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = dest,
                        onValueChange = { dest = it },
                        label = { Text("Destino") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = startStr,
                        onValueChange = { startStr = it },
                        label = { Text("Fecha de inicio (dd/MM/yyyy HH:mm)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = endStr,
                        onValueChange = { endStr = it },
                        label = { Text("Fecha de fin (opcional, dd/MM/yyyy HH:mm)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedStart = parseDateTimeStringToMillis(startStr) ?: detail.trip.startedAt
                        val parsedEnd = if (endStr.isNotBlank()) parseDateTimeStringToMillis(endStr) else null
                        viewModel.updateTripMetadata(name, origin, dest, notes)
                        viewModel.updateTripDates(parsedStart, parsedEnd)
                        showEditTripDialog = false
                    }
                ) {
                    Text("Guardar cambios")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditTripDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    if (showEditMotoDialog) {
        val currentMoto = uiState.motorcycleConfig
        var model by remember { mutableStateOf(currentMoto.modelName) }
        var capacityText by remember { mutableStateOf(currentMoto.tankCapacityLiters.toString()) }
        var reserveText by remember { mutableStateOf(currentMoto.reserveLiters.toString()) }
        var refConsText by remember { mutableStateOf(currentMoto.referenceConsumptionL100.toString()) }
        var capConfirmed by remember { mutableStateOf(currentMoto.isCapacityConfirmed) }
        var resConfirmed by remember { mutableStateOf(currentMoto.isReserveConfirmed) }

        AlertDialog(
            onDismissRequest = { showEditMotoDialog = false },
            title = { Text("Configuración de motocicleta") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = model,
                        onValueChange = { model = it },
                        label = { Text("Modelo de moto") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = capacityText,
                        onValueChange = { capacityText = it },
                        label = { Text("Capacidad del tanque (L)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = reserveText,
                        onValueChange = { reserveText = it },
                        label = { Text("Reserva de combustible (L)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = refConsText,
                        onValueChange = { refConsText = it },
                        label = { Text("Consumo de referencia (L/100 km)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = capConfirmed, onCheckedChange = { capConfirmed = it })
                        Text("Capacidad verificada por usuario")
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = resConfirmed, onCheckedChange = { resConfirmed = it })
                        Text("Reserva verificada por usuario")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val cap = capacityText.toDoubleOrNull() ?: currentMoto.tankCapacityLiters
                        val res = reserveText.toDoubleOrNull() ?: currentMoto.reserveLiters
                        val refCons = refConsText.toDoubleOrNull() ?: currentMoto.referenceConsumptionL100
                        viewModel.updateMotorcycleConfig(
                            currentMoto.copy(
                                modelName = model,
                                tankCapacityLiters = cap,
                                reserveLiters = res,
                                referenceConsumptionL100 = refCons,
                                isCapacityConfirmed = capConfirmed,
                                isReserveConfirmed = resConfirmed
                            )
                        )
                        showEditMotoDialog = false
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditMotoDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    stopToEdit?.let { stop ->
        var selectedType by remember { mutableStateOf(stop.type) }
        var expandedType by remember { mutableStateOf(false) }
        var name by remember { mutableStateOf(stop.name.orEmpty()) }
        var notes by remember { mutableStateOf(stop.notes.orEmpty()) }
        var arrStr by remember { mutableStateOf(formatEpochMillis(stop.arrivedAt)) }
        var depStr by remember { mutableStateOf(stop.departedAt?.let { formatEpochMillis(it) }.orEmpty()) }

        AlertDialog(
            onDismissRequest = { stopToEdit = null },
            title = { Text("Editar parada manual") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ExposedDropdownMenuBox(
                        expanded = expandedType,
                        onExpandedChange = { expandedType = !expandedType }
                    ) {
                        OutlinedTextField(
                            value = selectedType.name,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Categoría de parada") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedType) },
                            modifier = Modifier.menuAnchor().fillMaxWidth()
                        )
                        ExposedDropdownMenu(
                            expanded = expandedType,
                            onDismissRequest = { expandedType = false }
                        ) {
                            StopType.entries.forEach { type ->
                                DropdownMenuItem(
                                    text = { Text(type.name) },
                                    onClick = {
                                        selectedType = type
                                        expandedType = false
                                    }
                                )
                            }
                        }
                    }

                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Nombre / Lugar") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = arrStr,
                        onValueChange = { arrStr = it },
                        label = { Text("Hora de llegada (dd/MM/yyyy HH:mm)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = depStr,
                        onValueChange = { depStr = it },
                        label = { Text("Hora de salida (opcional)") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val parsedArr = parseDateTimeStringToMillis(arrStr) ?: stop.arrivedAt
                        val parsedDep = if (depStr.isNotBlank()) parseDateTimeStringToMillis(depStr) else null
                        viewModel.updateStop(
                            stop.copy(
                                type = selectedType,
                                name = name.takeIf { it.isNotBlank() },
                                notes = notes.takeIf { it.isNotBlank() },
                                arrivedAt = parsedArr,
                                departedAt = parsedDep
                            )
                        )
                        stopToEdit = null
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { stopToEdit = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    fuelToEdit?.let { fuel ->
        var amountText by remember { mutableStateOf((fuel.amountMinorUnits / 100.0).toString()) }
        var litersText by remember { mutableStateOf(fuel.liters?.toString().orEmpty()) }
        var odoText by remember { mutableStateOf(fuel.odometerKm?.toString().orEmpty()) }
        var station by remember { mutableStateOf(fuel.stationName.orEmpty()) }
        var isFull by remember { mutableStateOf(fuel.isFullTank) }
        var notes by remember { mutableStateOf(fuel.notes.orEmpty()) }

        AlertDialog(
            onDismissRequest = { fuelToEdit = null },
            title = { Text("Editar carga de combustible") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = amountText,
                        onValueChange = { amountText = it },
                        label = { Text("Importe total pagado") },
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
                        value = odoText,
                        onValueChange = { odoText = it },
                        label = { Text("Odómetro en km (opcional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = station,
                        onValueChange = { station = it },
                        label = { Text("Estación / Bandera") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Notas") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(checked = isFull, onCheckedChange = { isFull = it })
                        Text("¿Fue tanque lleno?")
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull() ?: (fuel.amountMinorUnits / 100.0)
                        val liters = litersText.toDoubleOrNull()?.takeIf { it > 0 }
                        val odo = odoText.toDoubleOrNull()?.takeIf { it >= 0 }
                        viewModel.updateFuelEntry(
                            fuel.copy(
                                amountMinorUnits = (amount * 100).toLong(),
                                liters = liters,
                                odometerKm = odo,
                                stationName = station.takeIf { it.isNotBlank() },
                                isFullTank = isFull,
                                notes = notes.takeIf { it.isNotBlank() }
                            )
                        )
                        fuelToEdit = null
                    }
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { fuelToEdit = null }) {
                    Text("Cancelar")
                }
            }
        )
    }
}
