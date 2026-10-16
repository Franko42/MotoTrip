# Product

<!-- impeccable:product-schema 1 -->

## Platform

android

## Users

Motorcyclists, adventure riders, touring enthusiasts, and daily commuters who track their journeys, fuel consumption, and travel expenses directly from a phone mounted on their motorcycle handlebars or stored in a tank bag.

## Product Purpose

MotoTrip provides a high-reliability, distraction-free, 100% offline-first motorcycle journey logger. It captures precise GPS track points, manual road stops, fuel fill-ups (with consumption calculation), and trip expenses with zero reliance on cloud servers, accounts, or cellular connectivity.

## Positioning

Unlike commercial subscription apps (Calimoto, Rever) that demand logins and send rider location traces to remote servers, MotoTrip provides complete data sovereignty: 100% local-first, zero telemetry, zero accounts, battery-friendly foreground service tracking, and glove-friendly cockpit ergonomics designed for sunlight legibility and high vibrations.

## Operating Context

- Handheld during route planning or at rest stops.
- Mounted on motorcycle handlebars or tank bag during rides, exposed to direct sunlight, vibrations, weather, and operated with motorcycle gloves.
- Often operated in remote areas with zero cellular connectivity (mountains, deserts, backcountry roads).

## Capabilities and Constraints

- Foreground GPS tracking service with Android 14/15/16 compliance (`FOREGROUND_SERVICE_LOCATION`).
- Strict local SQLite database via Room with atomic transactions and explicit foreign keys.
- Never interpolates or fabricates missing GPS points.
- MapLibre Native SDK map rendering with pure offline vector canvas fallback (`TrackPolylineCanvas`).
- Glove-friendly UI with minimum 48-56dp touch targets and generous spacing.
- Full backup and restore using JSON (v1 schema) and CSV export via Android Storage Access Framework (SAF).
- No cloud backend, no login, no ads, no trackers, no third-party telemetry.

## Brand Commitments

- Name: **MotoTrip**
- Aesthetics: "Asphalt & Safety Amber" — tactical, high-contrast, functional, rugged.
- Voice: Direct, concise, technical, no marketing fluff.

## Product Principles

1. **Rider Safety First**: Minimize cognitive load; vital metrics must be readable in a 1-second glance at speed.
2. **Absolute Data Sovereignty**: All data resides on the device; backup and export are owned by the user via SAF.
3. **Rock-solid Resilience**: Survives process death, low battery, and tunnel GPS loss without losing already recorded data.
4. **Offline Autonomy**: Every feature works seamlessly with zero cellular reception.
