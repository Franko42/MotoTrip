# Análisis Comercial y de Producto: MotoTrip

## 1. Posicionamiento en el Mercado

### Panorama Competitivo
En el ecosistema de aplicaciones para motociclistas predominan tres categorías:
1. **Planificadores de rutas curvas con suscripción premium** (*Calimoto*, *Kurviger*): enfocados en generar algoritmos de carreteras sinuosas, pero pesados, dependientes de conectividad a servidores y con suscripciones recurrentes costosas.
2. **Redes sociales y telemetría de comunidad** (*Rever*, *Detecht*): exigen login obligatorio, rastrean la posición continuamente a servidores en la nube y monetizan con planes mensuales.
3. **Navegadores offline genéricos** (*OsmAnd*, *Organic Maps*): excelentes en cartografía libre pero no optimizados para la ergonomía del motociclista en marcha (botones pequeños, menús anidados, sin registro ágil de combustible ni gastos de viaje).

### La Propuesta de Valor Única (Killer Value) de MotoTrip
MotoTrip ocupa un espacio desatendido pero altamente demandado por motociclistas de ruta y viaje:
* **100% Offline-First y Local-First**: Funciona en medio de la estepa patagónica, cordilleras o rutas remotas sin señal celular. No requiere cuenta, ni login, ni tarjeta de crédito.
* **Soberanía y Privacidad Absoluta**: Cero telemetría, cero analíticas, cero venta de datos. Tus rutas y gastos pertenecen exclusivamente a tu teléfono.
* **Consumo de Batería Mínimo**: Registro GPS por servicio foreground optimizado con filtro de jitter inteligente (evita despertar el procesador innecesariamente y no satura la base de datos).
* **Ergonomía de Cabina (Cockpit)**: Diseñado para ser operado con guantes, bajo sol directo o en lluvia, sobre un soporte de manillar.

---

## 2. Ergonomía del Motociclista y Factores Humanos

### Contexto de Operación Real
* **Vibraciones del motor y la calzada**: Los toques no son precisos. Un botón estándar de 36-40dp provoca pulsaciones erróneas. Se requiere un **área táctil mínima de 56dp** con espaciado de seguridad.
* **Luz solar directa (Sunlight Glare)**: Los esquemas de color pasteles o de bajo contraste se vuelven invisibles con reflejos en la pantalla o la mica del casco. Se exige **alto contraste tipográfico** (mínimo 7:1) y fondos oscuros OLED que ahorren batería y eviten reflejos deslumbrantes al anochecer.
* **Regla del "Glance" (1 segundo)**: El conductor no puede desviar la vista del camino por más de un segundo. La información crítica (velocidad instantánea, distancia acumulada, estado de grabación) debe tener jerarquía visual Display/Headline legible a 60 cm de distancia.
* **Uso con guantes**: Acciones primarias (Pausa, Carga de Combustible, Parada) accesibles en la mitad inferior de la pantalla con confirmaciones claras y hápticas/visuales inmediatas.

---

## 3. Arquitectura de Mapas y Visualización de Recorrido

### Visualización del Trazado GPS
1. **MapLibre Native (Vector Tiles Offline/Online)**:
   * Renderizado de la ruta en color ámbar de alta visibilidad o azul eléctrico con borde contrastante (stroke casing) para destacar sobre cualquier terreno.
   * Marcador de Inicio (verde esmeralda), paradas manuales intermedias (chips interactivos) y punto final o posición actual.
   * Auto-ajuste de cámara (bounds fitting) protegiendo casos límite (rutas de 1 solo punto o puntos duplicados).
2. **Fallback Autónomo (`TrackPolylineCanvas`)**:
   * Si no hay conexión o no hay paquetes de mapa descargados, la app no muestra una pantalla en blanco.
   * Renderiza el trazado vectorial puro sobre canvas con cuadrícula técnica milimétrica, norte geográfico, escala métrica y leyenda de kilometraje.

---

## 4. Sistema de Diseño "Asphalt & Safety Amber"

* **Superficie Principal**: Deep Asphalt (`#111315`) y Pure Dark OLED (`#0A0C0E`).
* **Acento Primario**: Safety Amber / Signal Orange (`#FF9100` / `#FF6D00`), color normativo de señalización vial internacional de máxima detección visual.
* **Estado Grabando (Activo)**: Electric Emerald (`#00E676`), visible instantáneamente para certificar que el GPS está registrando.
* **Estado Pausa**: Caution Yellow (`#FFD600`).
* **Superficies de Tarjeta**: Tonal Slate (`#1A1D21` y `#22262B`).
* **Texto Primario**: High-Contrast Off-White (`#F1F3F5`).
