# MotoTrip — paquete inicial para desarrollo agéntico

MotoTrip es una aplicación Android personal para registrar viajes en moto: recorrido GPS, tiempos, paradas manuales, combustible, gastos e historial. La aplicación es **local-first**, no requiere cuenta ni backend y prioriza mapas offline.

## Estado de este paquete

Este paquete contiene la especificación y las reglas de trabajo para que un agente de desarrollo pueda empezar el proyecto de forma controlada. **No es todavía una aplicación Android compilable**: las decisiones de versiones concretas de Gradle, Android SDK y dependencias deben verificarse al crear el proyecto.

## Documentos

- `AGENTS.md`: instrucciones persistentes para el agente de desarrollo.
- `docs/PRODUCT_SPEC.md`: alcance funcional, no funcional y límites del MVP.
- `docs/ARCHITECTURE.md`: arquitectura y estructura propuesta.
- `docs/DATA_MODEL.md`: entidades Room, relaciones y reglas de integridad.
- `docs/ANDROID_PERMISSIONS.md`: permisos, servicio de ubicación y restricciones Android.
- `docs/USE_CASES.md`: casos de uso y reglas de negocio.
- `docs/TEST_PLAN.md`: estrategia y criterios de aceptación.
- `docs/IMPLEMENTATION_PLAN.md`: fases sugeridas y definición de terminado.
- `docs/DECISIONS.md`: decisiones tomadas y puntos que aún requieren validación.
- `prompts/` contiene prompts acotados para iniciar el trabajo del agente.

## Cómo empezar

1. Creá un repositorio Git vacío llamado `MotoTrip`.
2. Copiá el contenido de este paquete en la raíz del repositorio.
3. Abrí la carpeta del repositorio en tu agente de desarrollo.
4. Pedile que lea `AGENTS.md` y todos los documentos referenciados antes de modificar archivos.
5. Empezá con `prompts/01-bootstrap-project.md`. Pedile que proponga un plan primero y que no implemente fases posteriores todavía.
6. Revisá el diff y ejecutá las pruebas antes de aceptar cada fase.

## Stack propuesto

- Kotlin
- Android nativo
- Jetpack Compose
- MVVM con casos de uso y repositorios
- Room / SQLite
- Fused Location Provider para ubicación
- Foreground Service para seguimiento activo
- MapLibre Native como candidato para mapas offline
- WorkManager para copias periódicas locales

Las versiones exactas deben elegirse compatibles entre sí y con el Android SDK instalado. El agente debe verificar documentación oficial actualizada antes de fijarlas.

## Alcance de seguridad

- No añadir backend, autenticación, analítica, publicidad ni telemetría.
- No subir ubicaciones ni gastos a servicios externos.
- No inventar datos GPS durante interrupciones.
- No eliminar ni reemplazar datos existentes durante una restauración sin confirmación explícita.
