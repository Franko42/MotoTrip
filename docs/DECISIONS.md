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

## Decisiones del parche de integridad y reportes (v1.1)
- **Fechas y trazabilidad**: `startedAt` representa la fecha real del viaje y se utiliza para ordenar y presentar en UI; `createdAt` se preserva intacto para auditoría y trazabilidad. Se permite la edición manual de fechas y metadatos sin sobreescribir `createdAt`.
- **Paradas manuales**: Salida explícita con duración calculada solo cuando existen llegada y salida válidas. Se mantiene nullable la hora de salida para evitar imputar horarios ficticios.
- **Separación económica (Requisito prioritario)**: El dinero desembolsado en cargas de combustible y otros gastos se calcula y reporta de forma independiente al costo del combustible efectivamente consumido. Las cargas de final de viaje no distorsionan el costo real del trayecto.
- **Resultados con incertidumbre explícita**: Se introduce `CalculationResult<T>` (`Confirmed`, `Estimated`, `Unknown`) para consumo, autonomía y velocidades, evitando mostrar ceros ficticios o estimaciones sin justificación.
- **Configuración de motocicleta**: Valores iniciales para Bajaj Boxer BM150 (capacidad provisional de 11L, reserva de 2.5L y consumo de referencia de 1.90 L/100 km) editables y marcados como provisionales hasta confirmación del usuario.
- **Persistencia y Room**: Migración `MIGRATION_1_2` que añade la columna `notes` a la tabla `fuel_entries` preservando la integridad de datos. El formato de backup JSON se actualiza a `schemaVersion = 2` con soporte total de importación para `schemaVersion = 1`.

## Actualizaciones de versiones desde la app (GitHub Releases)
- **Canal de distribución**: GitHub Releases público del repositorio (`Franko42/MotoTrip`). Sin backend intermediario ni servicios de telemetría/análisis.
- **Firma determinista de paquetes**: Se incluye `app/debug.keystore` configurado en `signingConfigs.debug` para garantizar que los binarios generados en GitHub Actions (CI) y los entornos de desarrollo local compartan idéntica firma, previniendo errores `INSTALL_FAILED_UPDATE_INCOMPATIBLE` en el sistema Android.
- **Instalación y almacenamiento seguro**: El APK descargado se aloja en `context.getExternalFilesDir(null)/updates` con permisos explícitos `setReadable(true, false)`. El traspaso al instalador de paquetes de Android (`PackageInstaller`) se realiza mediante `FileProvider` con autoridad `${applicationId}.fileprovider` y concesión explícita de `FLAG_GRANT_READ_URI_PERMISSION`.
- **Manejo de permisos en tiempo de ejecución**: En Android 8.0+ (API 26+), se consulta `canRequestPackageInstalls()`. Si no está concedido, se deriva limpiamente al usuario a `Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES`.
- **Redireccionamientos HTTP**: Las descargas siguen manualmente hasta 8 redireccionamientos (HTTP 302/307/308) correspondientes a los enlaces de almacenamiento AWS S3 de GitHub Releases.
- **Compatibilidad offline-first**: La búsqueda automática en inicio no bloquea ni muestra mensajes de error intrusivos si no hay conexión a internet; la búsqueda manual desde el menú notifica el estado detallado.

## Entorno de compilación y CI/CD (GitHub Actions)
- **Compilación y generación de APKs en GitHub Actions**: Toda compilación, validación de tests unitarios y generación de binarios APK (`app-debug.apk`) se delega exclusivamente a GitHub Actions (`.github/workflows/android.yml`).
- **Entorno local ligero**: El entorno de trabajo local no debe descargar ni almacenar JDKs, SDKs pesados de Android, Gradle o imágenes de contenedor para compilar localmente.
- **Trazabilidad de builds**: Las fallas de compilación o de pruebas se inspeccionan a través del historial de GitHub Actions o la rama `ci-log` (`ci-failure-log.txt`), y los éxitos en `ci-status` (`build-status.txt`).

