package com.mototrip.ui.history

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.mototrip.MotoTripApp
import com.mototrip.core.backup.BackupManager
import com.mototrip.core.backup.BackupPayload
import com.mototrip.core.backup.BackupValidationResult
import com.mototrip.domain.model.Trip
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

private val DateFormatter: DateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(
    viewModel: HistoryViewModel,
    onNavigateBack: () -> Unit,
    onTripSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val trips by viewModel.trips.collectAsState()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    var showMenu by remember { mutableStateOf(false) }
    var pendingRestorePayload by remember { mutableStateOf<BackupPayload?>(null) }
    var restoreErrorMessage by remember { mutableStateOf<String?>(null) }

    // SAF Create Document for JSON export
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val app = context.applicationContext as MotoTripApp
                    val snapshot = BackupManager.createDatabaseSnapshot(app.appContainer.database)
                    val json = BackupManager.serializeBackup(snapshot)

                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            stream.write(json.toByteArray(Charsets.UTF_8))
                        }
                    }
                    snackbarHostState.showSnackbar("Copia de seguridad exportada con éxito")
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar("Error al exportar copia: ${e.message}")
                }
            }
        }
    }

    // SAF Open Document for JSON import
    val importJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch {
                try {
                    val jsonString = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            stream.bufferedReader(Charsets.UTF_8).readText()
                        }
                    }

                    if (jsonString.isNullOrBlank()) {
                        restoreErrorMessage = "El archivo seleccionado está vacío."
                        return@launch
                    }

                    when (val result = BackupManager.parseAndValidate(jsonString)) {
                        is BackupValidationResult.Valid -> {
                            pendingRestorePayload = result.payload
                        }
                        is BackupValidationResult.IncompatibleVersion -> {
                            restoreErrorMessage = "Versión incompatible de copia de seguridad (Versión encontrada: ${result.foundVersion}, esperada: ${result.expectedVersion})."
                        }
                        is BackupValidationResult.Corrupted -> {
                            restoreErrorMessage = "El archivo de respaldo está dañado o tiene un formato inválido: ${result.reason}"
                        }
                    }
                } catch (e: Exception) {
                    restoreErrorMessage = "Error al leer archivo: ${e.message}"
                }
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Historial de viajes") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opciones de respaldo"
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Exportar copia de seguridad (JSON)") },
                            onClick = {
                                showMenu = false
                                val filename = "mototrip_backup_${System.currentTimeMillis()}.json"
                                exportJsonLauncher.launch(filename)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Restaurar copia de seguridad (JSON)") },
                            onClick = {
                                showMenu = false
                                importJsonLauncher.launch(arrayOf("application/json", "text/*"))
                            }
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
        if (trips.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(32.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "No hay viajes registrados aún.",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = "Cuando inicies y guardes tus viajes en moto aparecerán aquí.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(trips, key = { it.id }) { trip ->
                    TripHistoryItem(trip = trip, onClick = { onTripSelected(trip.id) })
                }
            }
        }
    }

    // Confirmation dialog before restoring backup
    pendingRestorePayload?.let { payload ->
        AlertDialog(
            onDismissRequest = { pendingRestorePayload = null },
            title = { Text("Confirmar restauración de copia") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Se validó el archivo de respaldo correctamente.")
                    Text("• Viajes a restaurar: ${payload.trips.size}")
                    Text("• Paradas: ${payload.stops.size}")
                    Text("• Cargas de combustible: ${payload.fuelEntries.size}")
                    Text("• Otros gastos: ${payload.expenses.size}")
                    Text("• Puntos de recorrido: ${payload.trackPoints.size}")
                    Spacer(modifier = Modifier.padding(4.dp))
                    Text(
                        text = "ADVERTENCIA: Esta acción reemplazará los datos actuales para garantizar la consistencia de las relaciones.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    onClick = {
                        val toRestore = payload
                        pendingRestorePayload = null
                        coroutineScope.launch {
                            val app = context.applicationContext as MotoTripApp
                            val result = BackupManager.restoreDatabase(app.appContainer.database, toRestore, clearExisting = true)
                            if (result.isSuccess) {
                                snackbarHostState.showSnackbar("Copia de seguridad restaurada exitosamente")
                            } else {
                                snackbarHostState.showSnackbar("Error al restaurar: ${result.exceptionOrNull()?.message}")
                            }
                        }
                    }
                ) {
                    Text("Reemplazar y restaurar")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingRestorePayload = null }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Error dialog for corrupt or incompatible backup
    restoreErrorMessage?.let { message ->
        AlertDialog(
            onDismissRequest = { restoreErrorMessage = null },
            title = { Text("Copia de seguridad rechazada") },
            text = { Text(message) },
            confirmButton = {
                Button(onClick = { restoreErrorMessage = null }) {
                    Text("Entendido")
                }
            }
        )
    }
}

@Composable
fun TripHistoryItem(
    trip: Trip,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = trip.name,
                    style = MaterialTheme.typography.titleMedium
                )
                SuggestionChip(
                    onClick = {},
                    label = { Text(trip.status.name) }
                )
            }

            val dateStr = Instant.ofEpochMilli(trip.startedAt)
                .atZone(ZoneId.systemDefault())
                .format(DateFormatter)

            Text(
                text = "Fecha: $dateStr",
                style = MaterialTheme.typography.bodySmall
            )

            if (!trip.originName.isNullOrBlank() || !trip.destinationName.isNullOrBlank()) {
                Text(
                    text = "${trip.originName ?: "?"} ➔ ${trip.destinationName ?: "?"}",
                    style = MaterialTheme.typography.bodyMedium
                )
            }

            val distanceKm = trip.distanceMeters / 1000.0
            Text(
                text = "Distancia: $distanceKm km",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}
