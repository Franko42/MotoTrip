package com.mototrip.domain.calculations

import com.mototrip.core.location.LocationFilter
import com.mototrip.data.local.entity.TripEventType
import com.mototrip.domain.model.CalculationResult
import com.mototrip.domain.model.Expense
import com.mototrip.domain.model.FuelEntry
import com.mototrip.domain.model.GpsSpeedStatistics
import com.mototrip.domain.model.SpeedSample
import com.mototrip.domain.model.Stop
import com.mototrip.domain.model.TrackPoint
import com.mototrip.domain.model.TripEconomicReport
import com.mototrip.domain.model.TripEvent
import com.mototrip.domain.model.TripTimeStatistics
import java.util.Locale

data class MoneyTotal(
    val currencyCode: String,
    val fuelAmountMinorUnits: Long,
    val otherExpensesMinorUnits: Long,
    val totalAmountMinorUnits: Long
)

object TripCalculations {

    private const val MIN_MOVING_SPEED_MPS = 0.8f // ~2.88 km/h, vehicle is moving
    private const val MAX_VALID_SPEED_MPS = 70.0f // ~252 km/h, reasonable upper limit for motorcycle
    private const val SIGNAL_GAP_THRESHOLD_MILLIS = 90_000L // 90 seconds without GPS implies tracking gap

    /**
     * Group expenses and fuel entries by currency code.
     * Prevents adding together amounts with different currencies.
     */
    fun calculateExpensesByCurrency(
        fuelEntries: List<FuelEntry>,
        expenses: List<Expense>
    ): List<MoneyTotal> {
        val currencies = (fuelEntries.map { it.currencyCode } + expenses.map { it.currencyCode }).toSet()

        return currencies.map { currency ->
            val fuelTotal = fuelEntries
                .filter { it.currencyCode == currency }
                .sumOf { it.amountMinorUnits }

            val otherTotal = expenses
                .filter { it.currencyCode == currency }
                .sumOf { it.amountMinorUnits }

            MoneyTotal(
                currencyCode = currency,
                fuelAmountMinorUnits = fuelTotal,
                otherExpensesMinorUnits = otherTotal,
                totalAmountMinorUnits = fuelTotal + otherTotal
            )
        }
    }

    /**
     * Calculates price per liter.
     * Returns null if liters is null, non-positive, or non-finite.
     */
    fun calculatePricePerLiter(amountMinorUnits: Long, liters: Double?): Double? {
        if (liters == null || liters <= 0.0 || liters.isNaN() || liters.isInfinite() || amountMinorUnits < 0) return null
        val price = (amountMinorUnits.toDouble() / 100.0) / liters
        return if (price.isFinite() && price >= 0.0) price else null
    }

    /**
     * Calculates cost per kilometer based on disbursed money.
     * Returns null if distance is 0 or less.
     */
    fun calculateCostPerKm(totalAmountMinorUnits: Long, distanceMeters: Long): Double? {
        if (distanceMeters <= 0 || totalAmountMinorUnits < 0) return null
        val distanceKm = distanceMeters.toDouble() / 1000.0
        val amount = totalAmountMinorUnits.toDouble() / 100.0
        val cost = amount / distanceKm
        return if (cost.isFinite() && cost >= 0.0) cost else null
    }

    /**
     * Calculates active duration (excluding paused intervals) from startedAt to endedAt/now.
     */
    fun calculateActiveDurationMillis(
        startedAt: Long,
        endedAt: Long?,
        events: List<TripEvent>,
        currentTimestamp: Long = System.currentTimeMillis()
    ): Long {
        val endTime = endedAt ?: currentTimestamp
        if (endTime <= startedAt) return 0L

        var totalPausedMillis = 0L
        var pauseStart: Long? = null

        val sortedEvents = events.sortedBy { it.timestamp }

        for (event in sortedEvents) {
            when (event.eventType) {
                TripEventType.PAUSED -> {
                    if (pauseStart == null && event.timestamp in startedAt..endTime) {
                        pauseStart = event.timestamp
                    }
                }
                TripEventType.RESUMED, TripEventType.COMPLETED -> {
                    if (pauseStart != null) {
                        val pauseEnd = minOf(event.timestamp, endTime)
                        if (pauseEnd >= pauseStart) {
                            totalPausedMillis += (pauseEnd - pauseStart)
                        }
                        pauseStart = null
                    }
                }
                else -> {}
            }
        }

        if (pauseStart != null && pauseStart <= endTime) {
            totalPausedMillis += (endTime - pauseStart)
        }

        val totalGross = endTime - startedAt
        return maxOf(0L, totalGross - totalPausedMillis)
    }

    /**
     * Calculates duration of a stop.
     * Returns null if departure was not registered or is prior to arrival.
     */
    fun calculateStopDurationMillis(arrivedAt: Long, departedAt: Long?): Long? {
        if (departedAt == null || departedAt < arrivedAt) return null
        return departedAt - arrivedAt
    }

    /**
     * Calculates fuel consumption in L/100km between two full-tank fuel entries with odometers.
     * Returns null if condition is not met (insufficient full tanks or missing odometers).
     */
    fun calculateFuelConsumptionBetweenFullTanks(
        earlierEntry: FuelEntry,
        laterEntry: FuelEntry
    ): Double? {
        if (!earlierEntry.isFullTank || !laterEntry.isFullTank) return null
        val odoStart = earlierEntry.odometerKm ?: return null
        val odoEnd = laterEntry.odometerKm ?: return null
        val liters = laterEntry.liters ?: return null

        val deltaDistance = odoEnd - odoStart
        if (deltaDistance <= 0.0 || liters <= 0.0 || deltaDistance.isNaN() || liters.isNaN()) return null

        val result = (liters / deltaDistance) * 100.0
        return if (result.isFinite() && result > 0.0) result else null
    }

    /**
     * Calculates trip fuel consumption in L/100km.
     * Prioritizes full-tank measurements. If only partial fills are available, returns an Estimated result.
     * If data is insufficient, returns Unknown with explanation.
     */
    fun calculateTripFuelConsumption(
        distanceMeters: Long,
        fuelEntries: List<FuelEntry>
    ): CalculationResult<Double> {
        if (distanceMeters <= 0) {
            return CalculationResult.Unknown("Distancia insuficiente o no registrada para calcular el consumo.")
        }
        val distanceKm = distanceMeters.toDouble() / 1000.0

        // 1. Check for consecutive full-tank entries with odometer readings
        val fullTanks = fuelEntries
            .filter { it.isFullTank && it.odometerKm != null && (it.liters ?: 0.0) > 0.0 }
            .sortedBy { it.odometerKm }

        if (fullTanks.size >= 2) {
            val first = fullTanks.first()
            val last = fullTanks.last()
            val deltaOdo = (last.odometerKm ?: 0.0) - (first.odometerKm ?: 0.0)
            val intermediateLiters = fullTanks.drop(1).mapNotNull { it.liters }.sum()

            if (deltaOdo > 0.0 && intermediateLiters > 0.0) {
                val consumptionL100 = (intermediateLiters / deltaOdo) * 100.0
                if (consumptionL100.isFinite() && consumptionL100 > 0.0) {
                    return CalculationResult.Confirmed(
                        value = consumptionL100,
                        details = String.format(
                            Locale.US,
                            "Medición confirmada de tanque lleno a tanque lleno (%.1f km, %.2f L)",
                            deltaOdo,
                            intermediateLiters
                        )
                    )
                }
            }
        }

        // 2. Fallback to total liters loaded over total distance, labeled as Estimated
        val totalLiters = fuelEntries.mapNotNull { it.liters }.filter { it > 0.0 && it.isFinite() }.sum()
        if (totalLiters > 0.0) {
            val estimatedL100 = (totalLiters / distanceKm) * 100.0
            if (estimatedL100.isFinite() && estimatedL100 > 0.0) {
                return CalculationResult.Estimated(
                    value = estimatedL100,
                    limitationReason = String.format(
                        Locale.US,
                        "Estimación basada en %.2f L cargados en %.1f km; no garantiza tanque lleno idéntico al inicio y fin.",
                        totalLiters,
                        distanceKm
                    )
                )
            }
        }

        return CalculationResult.Unknown("No se informaron litros cargados para calcular el consumo de combustible.")
    }

    /**
     * Calculates trip fuel efficiency in km/L based on consumption calculation.
     */
    fun calculateTripEfficiencyKmL(
        distanceMeters: Long,
        fuelEntries: List<FuelEntry>
    ): CalculationResult<Double> {
        return when (val consumptionResult = calculateTripFuelConsumption(distanceMeters, fuelEntries)) {
            is CalculationResult.Confirmed -> {
                val kmL = if (consumptionResult.value > 0.0) 100.0 / consumptionResult.value else 0.0
                CalculationResult.Confirmed(kmL, consumptionResult.details)
            }
            is CalculationResult.Estimated -> {
                val kmL = if (consumptionResult.value > 0.0) 100.0 / consumptionResult.value else 0.0
                CalculationResult.Estimated(kmL, consumptionResult.limitationReason)
            }
            is CalculationResult.Unknown -> CalculationResult.Unknown(consumptionResult.reason)
        }
    }

    /**
     * Theoretical range with full tank.
     * Formula: tankCapacityLiters * efficiencyKmL
     */
    fun calculateTheoreticalRangeKm(
        tankCapacityLiters: Double,
        efficiencyKmL: Double
    ): CalculationResult<Double> {
        if (tankCapacityLiters <= 0.0 || efficiencyKmL <= 0.0 || !tankCapacityLiters.isFinite() || !efficiencyKmL.isFinite()) {
            return CalculationResult.Unknown("Capacidad del tanque o rendimiento no disponibles.")
        }
        val range = tankCapacityLiters * efficiencyKmL
        return CalculationResult.Confirmed(range, "Autonomía teórica con tanque lleno (capacidad nominal)")
    }

    /**
     * Estimated range from full tank until hitting reserve.
     * Formula: (tankCapacityLiters - reserveLiters) * efficiencyKmL
     */
    fun calculateRangeToReserveKm(
        tankCapacityLiters: Double,
        reserveLiters: Double,
        efficiencyKmL: Double
    ): CalculationResult<Double> {
        val usableLiters = tankCapacityLiters - reserveLiters
        if (usableLiters <= 0.0 || efficiencyKmL <= 0.0 || !usableLiters.isFinite() || !efficiencyKmL.isFinite()) {
            return CalculationResult.Unknown("Volumen utilizable previo a reserva no válido o rendimiento desconocido.")
        }
        val range = usableLiters * efficiencyKmL
        return CalculationResult.Confirmed(range, "Autonomía estimada hasta entrar en reserva")
    }

    /**
     * Current estimated range based on known remaining fuel.
     * If remaining fuel is unknown, returns Unknown without guessing.
     */
    fun calculateCurrentEstimatedRangeKm(
        remainingFuelLiters: Double?,
        efficiencyKmL: Double
    ): CalculationResult<Double> {
        if (remainingFuelLiters == null || remainingFuelLiters <= 0.0 || !remainingFuelLiters.isFinite()) {
            return CalculationResult.Unknown("Combustible restante en el tanque desconocido; no se puede calcular autonomía actual restante.")
        }
        if (efficiencyKmL <= 0.0 || !efficiencyKmL.isFinite()) {
            return CalculationResult.Unknown("Rendimiento de combustible no disponible para estimar autonomía restante.")
        }
        val range = remainingFuelLiters * efficiencyKmL
        return CalculationResult.Estimated(range, "Basado en volumen de combustible restante informado o estimado")
    }

    /**
     * Produces economic reports per currency, strictly distinguishing between
     * money disbursed (cash outlay) and estimated economic cost of fuel consumed.
     */
    fun calculateEconomicReports(
        fuelEntries: List<FuelEntry>,
        expenses: List<Expense>,
        distanceMeters: Long,
        referenceConsumptionL100: Double? = null
    ): List<TripEconomicReport> {
        val currencies = (fuelEntries.map { it.currencyCode } + expenses.map { it.currencyCode }).toSet()
        val distanceKm = if (distanceMeters > 0) distanceMeters.toDouble() / 1000.0 else 0.0

        return currencies.map { currency ->
            val currencyFuel = fuelEntries.filter { it.currencyCode == currency }
            val currencyExpenses = expenses.filter { it.currencyCode == currency }

            val totalPaidFuel = currencyFuel.sumOf { it.amountMinorUnits }
            val totalOtherExpenses = currencyExpenses.sumOf { it.amountMinorUnits }
            val totalDisbursed = totalPaidFuel + totalOtherExpenses

            // Representative price per liter for this currency
            val entriesWithLitersAndAmount = currencyFuel.filter {
                it.amountMinorUnits > 0 && (it.liters ?: 0.0) > 0.0
            }
            val totalMinorUnitsWithLiters = entriesWithLitersAndAmount.sumOf { it.amountMinorUnits }
            val totalLitersForPrice = entriesWithLitersAndAmount.mapNotNull { it.liters }.sum()

            val representativePricePerLiter = if (totalLitersForPrice > 0.0) {
                (totalMinorUnitsWithLiters.toDouble() / 100.0) / totalLitersForPrice
            } else null

            // Determine consumption L/100km to use:
            // Prefer confirmed trip consumption (two full tanks with odometers).
            // If not confirmed, prefer motorcycle reference consumption if available to prevent end-of-trip fuel fills from distorting trip costs.
            // Fallback to estimated trip consumption from total liters loaded, if any.
            val tripConsumptionResult = calculateTripFuelConsumption(distanceMeters, currencyFuel)
            val effectiveL100 = if (tripConsumptionResult is CalculationResult.Confirmed) {
                tripConsumptionResult.value
            } else {
                referenceConsumptionL100?.takeIf { it > 0.0 } ?: tripConsumptionResult.valueOrNull
            }

            val estimatedFuelCostResult: CalculationResult<Long> = when {
                distanceKm <= 0.0 -> {
                    CalculationResult.Unknown("Distancia del viaje insuficiente para estimar consumo real de combustible.")
                }
                representativePricePerLiter == null -> {
                    CalculationResult.Unknown("Sin cargas con litros e importe en $currency para derivar el precio por litro representativo.")
                }
                effectiveL100 == null -> {
                    CalculationResult.Unknown("Sin datos de consumo (ni medición del viaje ni consumo de referencia) para estimar litros consumidos.")
                }
                else -> {
                    val estimatedLitersConsumed = (distanceKm * effectiveL100) / 100.0
                    val estimatedCostMinor = (estimatedLitersConsumed * representativePricePerLiter * 100.0).toLong()
                    val details = String.format(
                        Locale.US,
                        "Estimado: %.2f L consumidos (a %.2f L/100km) × %.2f %s/L",
                        estimatedLitersConsumed,
                        effectiveL100,
                        representativePricePerLiter,
                        currency
                    )
                    CalculationResult.Estimated(estimatedCostMinor, details)
                }
            }

            val estimatedTotalTripCostResult: CalculationResult<Long> = when (estimatedFuelCostResult) {
                is CalculationResult.Confirmed -> {
                    CalculationResult.Confirmed(
                        estimatedFuelCostResult.value + totalOtherExpenses,
                        "Costo de combustible consumido + otros gastos desembolsados"
                    )
                }
                is CalculationResult.Estimated -> {
                    CalculationResult.Estimated(
                        estimatedFuelCostResult.value + totalOtherExpenses,
                        "Costo estimado de combustible consumido + otros gastos desembolsados"
                    )
                }
                is CalculationResult.Unknown -> {
                    CalculationResult.Unknown("Costo total no determinable: ${estimatedFuelCostResult.reason}")
                }
            }

            TripEconomicReport(
                currencyCode = currency,
                totalPaidFuelMinorUnits = totalPaidFuel,
                totalOtherExpensesMinorUnits = totalOtherExpenses,
                totalDisbursedMinorUnits = totalDisbursed,
                estimatedFuelConsumedCostMinorUnits = estimatedFuelCostResult,
                estimatedTotalTripCostMinorUnits = estimatedTotalTripCostResult
            )
        }
    }

    /**
     * Calculates GPS and Speed Statistics from recorded track points.
     * Evaluates moving speed, total speed, valid max speed, speed evolution, and coverage.
     */
    fun calculateGpsSpeedStatistics(
        points: List<TrackPoint>,
        startedAt: Long,
        endedAt: Long?,
        currentTime: Long = System.currentTimeMillis()
    ): GpsSpeedStatistics {
        if (points.isEmpty()) {
            return GpsSpeedStatistics(
                averageMovingSpeedKmh = CalculationResult.Unknown("No se registraron muestras GPS para este viaje."),
                averageTotalSpeedKmh = CalculationResult.Unknown("No se registraron muestras GPS para este viaje."),
                maxValidSpeedKmh = CalculationResult.Unknown("Sin muestras GPS para evaluar velocidad máxima."),
                speedHistory = emptyList(),
                validPointsCount = 0,
                discardedPointsCount = 0,
                isCoverageSufficient = false,
                coverageNotes = "Sin puntos registrados"
            )
        }

        val sortedPoints = points.sortedBy { it.timestamp }
        var validPointsCount = 0
        var discardedPointsCount = 0

        var movingDistanceMeters = 0.0
        var movingTimeMillis = 0L

        var maxSpeedMps = 0.0f
        var hasValidSpeedSample = false

        val speedHistory = mutableListOf<SpeedSample>()
        var previousValidPoint: TrackPoint? = null
        var totalValidDistanceMeters = 0.0
        var hasSignalGaps = false

        for (pt in sortedPoints) {
            // Quality filters: valid coordinates and horizontal accuracy
            val isCoordsValid = pt.latitude in -90.0..90.0 && pt.longitude in -180.0..180.0
            val isAccuracyValid = (pt.accuracyMeters ?: Float.MAX_VALUE) in 0.0f..LocationFilter.MAX_ALLOWED_ACCURACY_METERS

            if (!isCoordsValid || !isAccuracyValid) {
                discardedPointsCount++
                continue
            }

            if (previousValidPoint != null) {
                val deltaMillis = pt.timestamp - previousValidPoint.timestamp
                if (deltaMillis <= 0) {
                    discardedPointsCount++
                    continue
                }

                if (deltaMillis > SIGNAL_GAP_THRESHOLD_MILLIS) {
                    hasSignalGaps = true
                }

                val dist = LocationFilter.calculateHaversineDistance(
                    previousValidPoint.latitude, previousValidPoint.longitude,
                    pt.latitude, pt.longitude
                )

                val derivedSpeedMps = dist / (deltaMillis / 1000.0)

                // Reject anomaly jumps (teleport spikes)
                if (derivedSpeedMps > MAX_VALID_SPEED_MPS) {
                    discardedPointsCount++
                    continue
                }

                totalValidDistanceMeters += dist

                // Check if vehicle was moving
                val speedMps = pt.speedMps
                val isMoving = if (speedMps != null && speedMps >= 0.0f) {
                    speedMps >= MIN_MOVING_SPEED_MPS
                } else {
                    derivedSpeedMps >= MIN_MOVING_SPEED_MPS
                }

                if (isMoving) {
                    movingDistanceMeters += dist
                    movingTimeMillis += deltaMillis
                }

                // Check speed sample
                val validSpeed = when {
                    speedMps != null && speedMps in 0.0f..MAX_VALID_SPEED_MPS -> speedMps
                    derivedSpeedMps in 0.0..MAX_VALID_SPEED_MPS.toDouble() -> derivedSpeedMps.toFloat()
                    else -> null
                }

                if (validSpeed != null) {
                    hasValidSpeedSample = true
                    if (validSpeed > maxSpeedMps) {
                        maxSpeedMps = validSpeed
                    }
                    speedHistory.add(SpeedSample(pt.timestamp, (validSpeed * 3.6).toDouble()))
                }
            } else {
                // First point
                val speedMps = pt.speedMps
                if (speedMps != null && speedMps in 0.0f..MAX_VALID_SPEED_MPS) {
                    hasValidSpeedSample = true
                    maxSpeedMps = maxOf(maxSpeedMps, speedMps)
                    speedHistory.add(SpeedSample(pt.timestamp, (speedMps * 3.6).toDouble()))
                }
            }

            validPointsCount++
            previousValidPoint = pt
        }

        // 1. Moving average speed
        val avgMovingSpeedResult: CalculationResult<Double> = if (movingTimeMillis > 5000L && movingDistanceMeters > 20.0) {
            val movingHours = movingTimeMillis / (1000.0 * 3600.0)
            val kmh = (movingDistanceMeters / 1000.0) / movingHours
            if (kmh.isFinite() && kmh >= 0.0) {
                CalculationResult.Confirmed(kmh, "Calculado a partir de muestras GPS en movimiento")
            } else {
                CalculationResult.Unknown("Datos insuficientes para media en movimiento.")
            }
        } else {
            CalculationResult.Unknown("Tiempo o desplazamiento en movimiento insuficiente.")
        }

        // 2. Total average speed
        val tripEndTime = endedAt ?: currentTime
        val grossDurationMillis = maxOf(0L, tripEndTime - startedAt)
        val avgTotalSpeedResult: CalculationResult<Double> = if (grossDurationMillis > 10_000L && totalValidDistanceMeters > 20.0) {
            val totalHours = grossDurationMillis / (1000.0 * 3600.0)
            val kmh = (totalValidDistanceMeters / 1000.0) / totalHours
            if (kmh.isFinite() && kmh >= 0.0) {
                CalculationResult.Confirmed(kmh, "Calculado entre inicio y fin del viaje, incluyendo detenciones")
            } else {
                CalculationResult.Unknown("Cálculo de velocidad media total no finito.")
            }
        } else {
            CalculationResult.Unknown("Duración de viaje insuficiente para velocidad media total.")
        }

        // 3. Max valid speed
        val maxSpeedKmh = (maxSpeedMps * 3.6).toDouble()
        val maxSpeedResult: CalculationResult<Double> = if (hasValidSpeedSample && maxSpeedKmh > 0.0) {
            if (hasSignalGaps) {
                CalculationResult.Estimated(
                    maxSpeedKmh,
                    "Máxima registrada en muestras disponibles; hubo pérdidas de señal donde pudo no registrarse."
                )
            } else {
                CalculationResult.Confirmed(maxSpeedKmh, "Velocidad máxima GPS verificada")
            }
        } else {
            CalculationResult.Unknown("No se capturaron muestras de velocidad válidas.")
        }

        val coverageNotes = when {
            hasSignalGaps -> "Se detectaron intervalos sin señal GPS superiores a 90 segundos; estadísticas parciales."
            discardedPointsCount > validPointsCount -> "Alto porcentaje de muestras descartadas por baja precisión."
            else -> null
        }

        return GpsSpeedStatistics(
            averageMovingSpeedKmh = avgMovingSpeedResult,
            averageTotalSpeedKmh = avgTotalSpeedResult,
            maxValidSpeedKmh = maxSpeedResult,
            speedHistory = speedHistory,
            validPointsCount = validPointsCount,
            discardedPointsCount = discardedPointsCount,
            isCoverageSufficient = !hasSignalGaps && validPointsCount >= 2,
            coverageNotes = coverageNotes
        )
    }

    /**
     * Calculates trip time breakdown distinguishing:
     * - Total duration
     * - Confirmed moving time
     * - Confirmed stopped time
     * - Manual stops duration
     * - Untracked/interrupted time
     */
    fun calculateTripTimeStatistics(
        startedAt: Long,
        endedAt: Long?,
        points: List<TrackPoint>,
        stops: List<Stop>,
        events: List<TripEvent>,
        currentTime: Long = System.currentTimeMillis()
    ): TripTimeStatistics {
        val endTime = endedAt ?: currentTime
        val totalDurationMillis = maxOf(0L, endTime - startedAt)

        // Manual stops duration
        val manualStopsDurationMillis = stops
            .mapNotNull { calculateStopDurationMillis(it.arrivedAt, it.departedAt) }
            .sum()

        // Track points analysis for moving vs confirmed stopped vs gaps
        var movingDurationMillis = 0L
        var confirmedStoppedDurationMillis = 0L
        var untrackedDurationMillis = 0L

        val sortedPoints = points.sortedBy { it.timestamp }
        var prevPoint: TrackPoint? = null

        for (pt in sortedPoints) {
            val isCoordsValid = pt.latitude in -90.0..90.0 && pt.longitude in -180.0..180.0
            val isAccuracyValid = (pt.accuracyMeters ?: Float.MAX_VALUE) in 0.0f..LocationFilter.MAX_ALLOWED_ACCURACY_METERS

            if (!isCoordsValid || !isAccuracyValid) continue

            if (prevPoint != null) {
                val delta = pt.timestamp - prevPoint.timestamp
                if (delta > 0) {
                    if (delta > SIGNAL_GAP_THRESHOLD_MILLIS) {
                        untrackedDurationMillis += delta
                    } else {
                        val dist = LocationFilter.calculateHaversineDistance(
                            prevPoint.latitude, prevPoint.longitude,
                            pt.latitude, pt.longitude
                        )
                        val speedMps = pt.speedMps
                        val isMoving = if (speedMps != null && speedMps >= 0.0f) {
                            speedMps >= MIN_MOVING_SPEED_MPS
                        } else {
                            (dist / (delta / 1000.0)) >= MIN_MOVING_SPEED_MPS
                        }

                        if (isMoving) {
                            movingDurationMillis += delta
                        } else {
                            confirmedStoppedDurationMillis += delta
                        }
                    }
                }
            }
            prevPoint = pt
        }

        // Untracked time from paused events if not already accounted
        var pausedEventsMillis = 0L
        var pauseStart: Long? = null
        for (ev in events.sortedBy { it.timestamp }) {
            when (ev.eventType) {
                TripEventType.PAUSED, TripEventType.TRACKING_INTERRUPTED -> {
                    if (pauseStart == null) pauseStart = ev.timestamp
                }
                TripEventType.RESUMED, TripEventType.TRACKING_RECOVERED, TripEventType.COMPLETED -> {
                    if (pauseStart != null) {
                        val duration = maxOf(0L, ev.timestamp - pauseStart)
                        pausedEventsMillis += duration
                        pauseStart = null
                    }
                }
                else -> {}
            }
        }
        if (pauseStart != null) {
            pausedEventsMillis += maxOf(0L, endTime - pauseStart)
        }

        val totalUntracked = maxOf(untrackedDurationMillis, pausedEventsMillis)

        return TripTimeStatistics(
            totalDurationMillis = totalDurationMillis,
            movingDurationMillis = movingDurationMillis,
            confirmedStoppedDurationMillis = confirmedStoppedDurationMillis,
            manualStopsDurationMillis = manualStopsDurationMillis,
            untrackedOrInterruptedDurationMillis = totalUntracked
        )
    }
}
