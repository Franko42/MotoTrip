# Modelo de datos

Las entidades definitivas deben implementarse como entidades Room con UUID string como clave primaria. Usar índices por claves foráneas y por fecha cuando corresponda.

## TripEntity
- `id: String` — UUID.
- `name: String`.
- `originName: String?`.
- `destinationName: String?`.
- `startedAt: Long`.
- `endedAt: Long?`.
- `status: TripStatus` — ACTIVE, PAUSED, COMPLETED.
- `distanceMeters: Long` — valor derivado y recalculable.
- `notes: String?`.
- `createdAt: Long`.
- `updatedAt: Long`.

## TrackPointEntity
- `id: String`.
- `tripId: String`.
- `timestamp: Long`.
- `latitude: Double`.
- `longitude: Double`.
- `accuracyMeters: Float?`.
- `speedMps: Float?`.
- `altitudeMeters: Double?`.
- Índice `(tripId, timestamp)`.
- Clave foránea a TripEntity.

## StopEntity
- `id: String`.
- `tripId: String`.
- `type: StopType` — FUEL, REST, FOOD, LODGING, REPAIR, TOURISM, OTHER.
- `arrivedAt: Long`.
- `departedAt: Long?`.
- `latitude: Double?`.
- `longitude: Double?`.
- `name: String?`.
- `notes: String?`.
- Clave foránea a TripEntity.

## FuelEntryEntity
- `id: String`.
- `tripId: String`.
- `stopId: String?`.
- `occurredAt: Long`.
- `amountMinorUnits: Long`.
- `currencyCode: String`, inicialmente ARS.
- `liters: Double?`.
- `odometerKm: Double?`.
- `stationName: String?`.
- `isFullTank: Boolean`.
- Claves foráneas a TripEntity y, si no es null, StopEntity.
- Validar importes no negativos y litros positivos cuando estén informados.

## ExpenseEntity
- `id: String`.
- `tripId: String`.
- `stopId: String?`.
- `occurredAt: Long`.
- `amountMinorUnits: Long`.
- `currencyCode: String`.
- `category: String` o enum controlado.
- `description: String?`.
- Claves foráneas a TripEntity y StopEntity.

## TripEventEntity
- `id: String`.
- `tripId: String`.
- `timestamp: Long`.
- `eventType: String` o enum: STARTED, PAUSED, RESUMED, COMPLETED, TRACKING_INTERRUPTED, TRACKING_RECOVERED.
- `latitude: Double?`.
- `longitude: Double?`.
- `details: String?`.

## Ajustes
Usar DataStore para preferencias sencillas (unidades, intervalo GPS, fecha del último backup). No crear una tabla de settings salvo que aparezca una necesidad real.

## Reglas
- Guardar timestamps como epoch UTC en milisegundos.
- Mostrar tiempos según zona horaria local.
- No usar Float/Double para importes monetarios.
- Mantener los puntos GPS originales separados de ediciones manuales.
- Las relaciones deben ser consistentes y las operaciones multi-entidad transaccionales.
- No usar destructive migration en producción.
- El estado activo se recupera de la base, no solo de memoria.
- Evaluar si `distanceMeters` se necesita persistir o si puede calcularse/caché; si se persiste, documentar su estrategia de recalculado.
