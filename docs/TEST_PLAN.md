# Plan de pruebas

## Unitarias
- Distancia entre coordenadas conocidas.
- Puntos duplicados, fuera de rango, baja precisión y saltos anómalos.
- Huecos de GPS y tiempos desconocidos.
- Máquina de estados ACTIVE/PAUSED/COMPLETED.
- Duración descontando pausas explícitas.
- Totales de gastos por moneda.
- Validación de importes y litros.
- Costo por km y precio por litro.
- Consumo entre cargas completas y casos insuficientes.

## Room e integración
- CRUD de cada entidad.
- Claves foráneas y asociaciones opcionales.
- Orden cronológico de puntos.
- Transacciones al finalizar o eliminar un viaje.
- Migraciones desde cada versión soportada.
- Recuperación de viaje activo después de recrear el proceso.
- Exportar/importar conservando relaciones y UUID.
- Rechazar backup corrupto o de versión incompatible.

## Android
- Permisos concedidos, denegados y revocados.
- Ubicación desactivada durante viaje.
- Pantalla apagada durante un periodo prolongado.
- Cambio a otra app.
- Modo avión y pérdida temporal de señal.
- Proceso detenido por el sistema.
- Reinicio del dispositivo: no reiniciar tracking automáticamente.
- Notificación del servicio y acción de detener/reabrir.
- WorkManager retrasado por restricciones de batería.
- Mapas disponibles y mapas no descargados.

## Pruebas de ruta
- Trayecto corto con parada manual.
- Viaje de varias horas con pantalla apagada.
- Viaje sin conectividad.
- Comparación con odómetro como referencia, sin asumir que el GPS será idéntico.
- Registrar carga de combustible y otros gastos.
- Medir batería y espacio ocupado.
- Revisar puntos perdidos, saltos y exactitud de tiempos.

## Criterios de aceptación
1. Un viaje puede iniciarse, pausarse, reanudarse y finalizar respetando sus estados.
2. La app recupera los datos persistidos tras cerrar y volver a abrir.
3. Los datos ya registrados no dependen de internet.
4. Las paradas solo se crean manualmente en v1.
5. Los resúmenes no inventan tiempos o distancias ante huecos GPS.
6. Los respaldos se validan antes de restaurar.
7. No se envían datos a servicios externos no aprobados.
8. Las pruebas relevantes pasan o sus limitaciones quedan documentadas.
