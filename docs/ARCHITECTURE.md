# Arquitectura propuesta

## Enfoque

Aplicación Android nativa con un único módulo `app` inicialmente. Arquitectura por capas, MVVM en presentación y casos de uso en dominio.

## Stack candidato

- Kotlin
- Jetpack Compose
- ViewModel + StateFlow
- Room / SQLite
- Kotlin Coroutines y Flow
- Fused Location Provider, si se acepta su dependencia de Google Play services; evaluar alternativa si el requisito de independencia es prioritario.
- Foreground Service para seguimiento activo, según las reglas de la versión Android objetivo.
- MapLibre Native para visualización, con fuente cartográfica compatible con uso offline.
- WorkManager para respaldo periódico no exacto.
- Android Storage Access Framework para exportación manual.

El agente debe comprobar versiones compatibles y documentación oficial antes de crear el proyecto.

## Estructura sugerida

```text
app/src/main/java/com/mototrip/
  MotoTripApp.kt
  MainActivity.kt
  core/
    database/
    location/
    maps/
    backup/
    time/
  data/
    local/entity/
    local/dao/
    local/converter/
    repository/
  domain/
    model/
    repository/
    usecase/
    calculations/
  service/
    TripTrackingService.kt
  ui/
    home/
    active_trip/
    stops/
    expenses/
    trip_detail/
    history/
    settings/
  navigation/
```

## Dependencias entre capas

- UI → casos de uso y estado de presentación.
- Dominio → modelos, interfaces de repositorio y reglas puras.
- Data → implementaciones de repositorio, Room y mapeadores.
- Servicio de seguimiento → componente de ubicación y repositorios/casos de uso, sin depender de una pantalla.
- Backup → snapshot consistente de datos y formato de exportación versionado.

No permitir que composables consulten DAO directamente ni que la UI mantenga una segunda fuente de verdad del viaje activo.

## Servicio GPS

- Solo se inicia mediante una acción explícita del usuario.
- Mantiene una notificación persistente mientras realiza seguimiento.
- Guarda puntos en lotes razonables, con tolerancia a cierre inesperado.
- Persiste estado y eventos de inicio, pausa, reanudación, finalización e interrupción.
- No asume que Android permitirá ejecutar indefinidamente cualquier servicio.
- Detecta intervalos sin datos y los expone en el resumen.
- No iniciar automáticamente un viaje tras reiniciar el teléfono.

## Mapas

Distinguir:
1. Persistencia de coordenadas.
2. Visualización del mapa sin conexión.
3. Navegación offline.

Solo las dos primeras se consideran para v1. Verificar licencias, atribución, formato de paquetes y permiso de almacenamiento antes de seleccionar una fuente de mapas.

## Respaldo

- Exportación manual mediante selector de documentos.
- Backup periódico con WorkManager, sin prometer hora exacta.
- Snapshot consistente y formato versionado.
- Validación de integridad, versión y relaciones antes de importar.
- No sobrescribir datos actuales sin confirmación explícita.
