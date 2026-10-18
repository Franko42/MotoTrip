# AGENTS.md — reglas obligatorias para el agente

## 1. Objetivo del proyecto

Construir MotoTrip, una aplicación Android nativa de uso personal para registrar viajes en moto. La especificación de producto está en `docs/PRODUCT_SPEC.md`; la arquitectura está en `docs/ARCHITECTURE.md`.

Antes de implementar, leer todos los documentos de `docs/` relevantes para la tarea.

## 2. Forma de trabajar

1. Trabajar en fases pequeñas y verificables.
2. Antes de modificar archivos, explicar brevemente el objetivo, los archivos afectados y cómo se comprobará el resultado.
3. Implementar únicamente la fase solicitada. No adelantar funciones de fases posteriores.
4. Después de los cambios, ejecutar las pruebas y verificaciones disponibles; informar exactamente cuáles se ejecutaron y cuáles no.
5. Si una dependencia, API o permiso depende de la versión actual de Android, verificar documentación oficial vigente. No inventar nombres de APIs ni versiones.
6. Si hay una ambigüedad que afecta arquitectura, privacidad, persistencia o comportamiento, detenerse y preguntar antes de asumir.
7. Mantener el README y la documentación actualizados cuando una decisión cambie.
8. No afirmar que algo compila o funciona sin haberlo verificado.
9. **Compilación y APKs exclusivamente en GitHub Actions**: No intentar compilar localmente descargando Java/JDK, Gradle o imágenes Docker de desarrollo. El proyecto utiliza GitHub Actions (`.github/workflows/android.yml`) para compilar, correr tests unitarios y generar los APKs. Las verificaciones de compilación se realizan monitoreando los workflows de GitHub Actions o revisando las ramas `ci-status` / `ci-log`.

## 3. Restricciones técnicas

- Kotlin y Android nativo.
- Jetpack Compose para UI.
- Room/SQLite para datos persistentes.
- Separar UI, dominio y datos.
- Usar repositorios y casos de uso; no ejecutar SQL ni lógica de negocio dentro de composables.
- El seguimiento GPS debe vivir en un servicio apropiado, no en una pantalla ni en un bucle ligado a la UI.
- Preferir `Flow`/`StateFlow` para observar datos y estados.
- Las operaciones de base de datos deben ejecutarse fuera del hilo principal.
- Usar claves foráneas e índices apropiados en Room.
- Manejar cambios de esquema con migraciones explícitas; no descartar datos mediante destructive migration.
- Mantener el producto local-first y offline-first.

## 4. Privacidad y límites

- No crear backend, login, sincronización en la nube, publicidad, analytics ni telemetría.
- No enviar ubicaciones, recorridos, gastos ni datos del dispositivo a terceros.
- Solicitar solo permisos imprescindibles y en el contexto adecuado.
- No pedir permiso de ubicación en segundo plano automáticamente. Revisar primero los requisitos reales y las políticas de Android.
- Los mapas offline pueden requerir descargar datos cartográficos; documentar proveedor, licencias, atribución, almacenamiento y límites.
- No incluir secretos, claves API privadas ni credenciales en el repositorio.
- No inventar puntos GPS ni interpolar largos intervalos como si fueran datos reales.
- Marcar los cálculos afectados por datos incompletos como estimados o desconocidos.

## 5. Datos y cálculos

- Los importes monetarios deben representarse sin errores de coma flotante: usar unidades menores enteras cuando la moneda lo permita o una representación decimal exacta.
- Persistir timestamps como instantes UTC (por ejemplo, epoch en milisegundos) y presentarlos en la zona horaria local.
- Mantener separados los puntos GPS originales y las correcciones manuales.
- La distancia acumulada es derivada y debe poder recalcularse a partir de puntos válidos.
- No sumar sin filtro todas las variaciones GPS.
- Una parada solo se crea por acción manual del usuario en la v1.
- El consumo L/100 km no se calcula como exacto si no existen datos de litros y distancias adecuados, preferiblemente entre cargas completas.
- Evitar borrar registros relacionados de forma parcial. Usar transacciones cuando una operación abarque varias entidades.

## 6. Calidad y cambios

- Añadir pruebas para reglas de negocio y casos límite.
- No agregar dependencias por conveniencia sin justificar su necesidad.
- No reescribir archivos completos si basta un cambio pequeño.
- No cambiar el stack, la estructura o los requisitos sin explicar el motivo y actualizar `docs/DECISIONS.md`.
- No usar datos reales de ubicación del usuario en tests, capturas o fixtures.
- No incorporar mapas o tiles de proveedores que prohíban su descarga o uso offline.

## 7. Definición de terminado por tarea

Una tarea solo se considera terminada cuando:
- el cambio respeta la especificación;
- compila y pasa pruebas en GitHub Actions CI (sin descargar herramientas locales de compilación);
- las pruebas relevantes pasan, o se documenta el estado verificado en CI;
- no se añadieron funciones fuera de alcance;
- se resumen los archivos modificados y las verificaciones realizadas.
