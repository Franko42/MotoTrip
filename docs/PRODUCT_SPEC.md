# Especificación de producto — MotoTrip v1

## Propósito

Registrar de manera fiable los viajes personales en moto: recorrido, duración, paradas manuales, combustible y otros gastos. La app debe priorizar la operación offline y la privacidad local.

## Usuarios

Un único usuario principal, sin cuentas ni roles. La interfaz se optimiza para uso personal y registro rápido, no para interacción mientras se conduce.

## Funcionalidades incluidas en el MVP

### Viajes
- Crear e iniciar un viaje con hora automática y nombre opcional.
- Origen y destino opcionales.
- Pausar, reanudar y finalizar.
- Persistir el estado para recuperar viajes tras cierre inesperado.
- Editar metadatos del viaje.
- Consultar historial y detalle.

### GPS y recorrido
- Capturar puntos con latitud, longitud, timestamp, precisión y velocidad cuando esté disponible.
- Mostrar el recorrido registrado en un mapa.
- Calcular distancia estimada a partir de puntos válidos.
- Continuar con la pantalla apagada mediante el mecanismo Android permitido para seguimiento activo.
- Registrar interrupciones y huecos de datos.
- No inventar puntos durante pérdida de señal.

### Paradas
- Registro exclusivamente manual en v1.
- Tipo: combustible, descanso, comida, alojamiento, reparación, turismo u otro.
- Hora, ubicación opcional, nombre y notas.
- Permitir editar los datos de la parada.
- Asociar cargas de combustible y gastos a una parada de forma opcional.

### Combustible y gastos
- Registrar importe, litros opcionales, precio por litro derivado, fecha, estación opcional, odómetro opcional y si fue tanque lleno.
- Registrar otros gastos por categoría y descripción.
- Mostrar totales por categoría y viaje.
- Calcular costo por km si la distancia es válida.
- Calcular consumo solo si los datos permiten una estimación razonable.

### Historial y exportación
- Lista de viajes y detalle cronológico.
- Resumen de distancia, duración, tiempos, paradas y gastos.
- Exportar viaje o historial en CSV y JSON.
- Generar respaldo periódico local y permitir exportación manual.
- Validar una copia antes de restaurarla y no sobrescribir silenciosamente datos existentes.

### Mapas offline
- Priorizar mapas descargados/locales.
- El registro GPS debe seguir funcionando sin mapas disponibles.
- La navegación giro a giro y el cálculo de rutas no forman parte de v1.

## No incluido en v1

- Backend, cuentas o sincronización en la nube.
- Compartir viajes con otras personas.
- Navegación giro a giro.
- Detección automática de paradas.
- Integraciones con bancos, estaciones o servicios externos.
- Telemetría, publicidad o analítica externa.

## Requerimientos no funcionales

- Local-first: los datos personales permanecen en el dispositivo por defecto.
- Offline-first: registrar viajes y gastos no depende de internet.
- Persistencia transaccional y recuperación ante interrupciones.
- Uso razonable de batería y almacenamiento.
- Permisos mínimos, explicados al usuario.
- UI legible y con acciones grandes; no requiere interacción mientras se conduce.
- Los cálculos deben exponer incertidumbre cuando falten datos.
- Exportaciones y respaldos deben poder validarse antes de restaurar.
