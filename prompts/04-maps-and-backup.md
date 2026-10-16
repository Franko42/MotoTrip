# Prompt 04 — Mapas offline y respaldo

Leer `AGENTS.md`, `docs/ARCHITECTURE.md`, `docs/PRODUCT_SPEC.md` y `docs/TEST_PLAN.md`.

Implementar mapas offline y exportación/respaldo como tareas separadas si el alcance resulta demasiado grande.

Mapas:
- Verificar licencia y condiciones de uso offline del proveedor.
- Mostrar recorrido guardado con datos cartográficos disponibles localmente.
- La ausencia de mapas no debe impedir registrar GPS.
- No implementar navegación giro a giro.

Backup:
- Exportación manual mediante Storage Access Framework.
- Backup periódico local con WorkManager, sin prometer hora exacta.
- Formato versionado, snapshot consistente y validación antes de restaurar.
- No sobrescribir la base de datos sin confirmación explícita.
- Probar archivos corruptos e incompatibles.

Antes de implementar, presentar opciones, licencias, dependencias y plan de pruebas. No descargar ni incluir paquetes cartográficos de origen/licencia no verificados.
