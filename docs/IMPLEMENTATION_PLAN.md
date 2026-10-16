# Plan de implementación

## Fase 0 — Bootstrap
- Crear proyecto Android nativo con Kotlin y Compose.
- Elegir `applicationId`, `minSdk`, `targetSdk` y versiones compatibles.
- Configurar análisis estático y pruebas.
- Crear navegación y una pantalla placeholder.
- Confirmar que compila en entorno limpio.

**Salida:** proyecto mínimo compilable, sin GPS todavía.

## Fase 1 — Persistencia y viajes manuales
- Añadir Room y migración inicial.
- Implementar TripEntity, StopEntity, FuelEntryEntity, ExpenseEntity y TripEventEntity.
- Crear repositorios y casos de uso.
- Crear pantallas para iniciar/finalizar viaje, paradas, gastos e historial.
- Añadir pruebas unitarias y de Room.

**Salida:** registro manual completo sin seguimiento GPS.

## Fase 2 — Seguimiento GPS
- Implementar permisos y servicio en primer plano.
- Persistir puntos y eventos de interrupción.
- Implementar filtros y cálculo de distancia.
- Recuperar estado después de interrupciones.
- Probar en dispositivo físico con pantalla apagada.

**Salida:** recorrido registrado y recuperable.

## Fase 3 — Mapas y resumen
- Integrar MapLibre y una fuente cartográfica con uso offline permitido.
- Mostrar recorrido guardado.
- Calcular y presentar tiempos y gastos.
- Documentar limitaciones y datos faltantes.

**Salida:** detalle visual del viaje.

## Fase 4 — Exportación y respaldo
- Exportar CSV y JSON.
- Crear backup local periódico con WorkManager.
- Añadir selector de destino para exportación manual.
- Validar e importar copias de seguridad.
- Probar archivos corruptos, incompatibles y duplicados.

**Salida:** datos portables y restaurables.

## Definition of Done
- Requisitos de la fase implementados.
- Build y pruebas ejecutados.
- Sin destructive migrations ni secretos.
- Documentación actualizada.
- Diff revisado y resumen de verificaciones informado.
