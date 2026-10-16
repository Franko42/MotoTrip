# Prompt 03 — Seguimiento GPS

Leer `AGENTS.md`, `docs/ANDROID_PERMISSIONS.md`, `docs/ARCHITECTURE.md`, `docs/USE_CASES.md` y `docs/TEST_PLAN.md`.

Implementar únicamente el seguimiento GPS de la Fase 2:
- Permisos solicitados en contexto.
- Servicio en primer plano compatible con el SDK seleccionado.
- Notificación persistente.
- Captura y persistencia de puntos GPS.
- Estados de seguimiento y eventos de interrupción.
- Recuperación segura sin inventar puntos faltantes.
- Pruebas unitarias de filtrado y pruebas de integración donde sean posibles.

Verificar documentación Android vigente para restricciones de foreground services y permisos. No asumir que un servicio se puede iniciar libremente desde segundo plano.
No implementar detección automática de paradas.
Antes de modificar, presentar plan y riesgos; al terminar, ejecutar verificaciones y reportar límites de pruebas en dispositivo real.
