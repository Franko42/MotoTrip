# Casos de uso y reglas de negocio

## Viajes
- `StartTrip`: impedir un segundo viaje activo; crear viaje y evento STARTED en una transacción lógica.
- `PauseTrip`: solo para viaje ACTIVE; guardar evento PAUSED.
- `ResumeTrip`: solo para viaje PAUSED; guardar evento RESUMED.
- `FinishTrip`: completar viaje, cerrar estado y consolidar métricas.
- `RecoverActiveTrip`: recuperar estado persistido y detectar huecos desde el último punto fiable.

## GPS
- `RecordTrackPoint`: validar coordenadas, timestamp, precisión y velocidad antes de persistir.
- `RecalculateDistance`: filtrar puntos inválidos y recalcular la distancia.
- No interpolar intervalos largos como si fueran datos medidos.
- Mantener el dato GPS original; las correcciones manuales no lo sobrescriben silenciosamente.

## Paradas
- `AddStop`: crear parada por acción explícita del usuario.
- `EditStop`: permitir corrección de hora, tipo, nombre, ubicación y notas.
- La ubicación es opcional cuando el GPS no está disponible.
- En v1 no se generan paradas automáticamente.

## Gastos
- `AddFuelEntry`: validar importe, litros, moneda y datos opcionales.
- `AddExpense`: validar importe, categoría y fecha.
- `CalculateTripSummary`: sumar combustible y otros gastos por moneda; no sumar monedas diferentes sin una conversión explícita.
- `CalculateFuelConsumption`: producir estimación solo si los datos son suficientes, preferiblemente entre cargas completas.

## Respaldo
- `ExportTrip`: exportar viaje/historial en un formato versionado.
- `CreateBackup`: crear snapshot consistente y registrar fecha/resultado.
- `RestoreBackup`: validar antes de modificar la base; mostrar conflictos y pedir confirmación.
- Nunca importar silenciosamente duplicados o reemplazar datos existentes.

## Cálculos
- Distancia: sumar distancias entre puntos consecutivos válidos, con filtros contra ruido y saltos imposibles.
- Duración total: fin menos inicio, descontando pausas explícitas.
- Tiempo en movimiento: estimación a partir de intervalos fiables.
- Tiempo detenido: solo computarlo donde la información sea suficiente; distinguirlo de pausas explícitas.
- Costo por km: gasto total dividido por distancia válida mayor que cero.
- Precio por litro: importe dividido por litros positivos.
