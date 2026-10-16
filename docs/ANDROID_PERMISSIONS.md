# Permisos y ciclo de vida Android

## Objetivo

Registrar ubicación durante un viaje iniciado explícitamente por el usuario, incluso con la pantalla apagada, dentro de las restricciones de Android.

## Permisos candidatos

- `ACCESS_COARSE_LOCATION`
- `ACCESS_FINE_LOCATION`
- `FOREGROUND_SERVICE`
- `FOREGROUND_SERVICE_LOCATION` para las versiones de Android/target SDK que lo exigen.
- `POST_NOTIFICATIONS` en Android 13+ cuando corresponda.
- `RECEIVE_BOOT_COMPLETED` solo si se justifica una función específica de recuperación tras reinicio; no iniciar seguimiento automáticamente.

El manifiesto definitivo depende del `minSdk`, `targetSdk` y la versión de las bibliotecas. Verificar documentación oficial vigente antes de implementarlo.

## Reglas de implementación

- Pedir permisos en contexto, cuando el usuario inicia el seguimiento.
- Explicar para qué se utiliza la ubicación.
- No solicitar permiso de ubicación en segundo plano por defecto.
- Iniciar el servicio de ubicación desde una acción visible del usuario y cumplir las restricciones de inicio de servicios en primer plano.
- Mantener una notificación visible y comprensible mientras el seguimiento está activo.
- Si se pierde el permiso o se desactiva la ubicación, guardar el evento y avisar; no fingir que el registro continúa.
- No asumir que todos los fabricantes permiten el mismo comportamiento de batería.
- Probar con pantalla apagada y modo ahorro de batería.

## Ejemplo orientativo de declaración

```xml
<uses-permission android:name="android.permission.ACCESS_COARSE_LOCATION" />
<uses-permission android:name="android.permission.ACCESS_FINE_LOCATION" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
<uses-permission android:name="android.permission.FOREGROUND_SERVICE_LOCATION" />

<application>
    <service
        android:name=".service.TripTrackingService"
        android:exported="false"
        android:foregroundServiceType="location" />
</application>
```

No copiar este fragmento sin comprobar compatibilidad con el SDK seleccionado. Los permisos de internet y notificaciones deben agregarse solo si el diseño final los requiere.
