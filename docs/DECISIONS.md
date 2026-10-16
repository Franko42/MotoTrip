# Registro de decisiones

## Confirmadas
- Plataforma: Android nativo.
- Lenguaje: Kotlin.
- Interfaz: Jetpack Compose.
- Persistencia: Room/SQLite.
- Arquitectura: capas UI, dominio y datos; MVVM.
- Uso personal, sin backend ni cuenta.
- Offline-first.
- Seguimiento GPS automático durante viajes iniciados explícitamente.
- Paradas manuales en v1.
- Mapas offline prioritarios.
- Exportación manual y respaldo periódico local.
- No incluir navegación giro a giro en v1.

## Deben verificarse antes de implementarse
- `minSdk` fijado en 26, `targetSdk` preparado para Android 15/16 (API 35/36).
- Compatibilidad con Android 16: cumplimiento estricto de `FOREGROUND_SERVICE_LOCATION`, verificación previa de permisos en tiempo de ejecución antes de `startForeground()` y restricción de inicio exclusivo con Activity visible en primer plano.
- Proveedor y licencia de datos cartográficos offline.
- Estrategia concreta de descarga, almacenamiento y atribución de mapas.
- Uso de Fused Location Provider frente a una alternativa menos dependiente de Google Play services.
- Frecuencia GPS y criterios de filtrado, a validar en pruebas reales.
- Política de retención y tamaño máximo de puntos por viaje.
- Si `distanceMeters` se persiste o se deriva bajo demanda.

## No cambiar sin justificar y documentar
- Stack principal.
- Ausencia de backend.
- Privacidad local-first.
- Registro manual de paradas.
- Reglas de integridad y backup.
