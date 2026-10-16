# Design System & Visual Authority: MotoTrip

## Visual Theme: "Asphalt & Safety Amber"

MotoTrip uses an intentional, rugged design language inspired by motorcycle cockpit instrumentation, rally navigation computers, and high-visibility road safety standards.

### Color Tokens (Material 3)

| Role | Light Theme | Dark Theme | Purpose / Usage |
|---|---|---|---|
| **Primary** | `#D97706` (Amber 600) | `#FFB300` (Safety Amber) | Brand identity, primary actions, active highlights |
| **On Primary** | `#FFFFFF` | `#000000` | Text/icons over primary color |
| **Primary Container** | `#FEF3C7` | `#451A03` | Highlighted surfaces, hero badges |
| **Secondary** | `#0284C7` (Electric Blue) | `#38BDF8` (Sky 400) | Secondary actions, route polyline overlay |
| **Tertiary** | `#059669` (Emerald) | `#34D399` (Electric Emerald) | Active GPS recording state, positive metrics |
| **Background** | `#F8FAFC` (Slate 50) | `#0B0D0F` (OLED Deep Asphalt) | Screen background |
| **Surface** | `#FFFFFF` | `#13161A` (Asphalt Dark) | Cards, sheets, top app bar |
| **Surface Variant** | `#E2E8F0` | `#1E232A` (Slate Tonal) | Secondary cards, metric tiles |
| **On Surface** | `#0F172A` | `#F1F5F9` (Off-white) | High contrast primary text |
| **Error** | `#DC2626` | `#F87171` | Emergency actions (Finish Trip, Delete) |

### Typography Scale
- **Display Large / Medium**: Used exclusively for primary glanceable cockpit metrics (Speed, Distance, Duration). Bold, monospaced numerals.
- **Headline / Title**: Screen titles, section anchors, dialog headers.
- **Body Large / Medium**: Labels, descriptions, stop names. `sp` units to honor Android accessibility font scaling.

### Component Guidelines
- **Cockpit Action Buttons**: Minimum `56dp` height with prominent icons and high-contrast labels.
- **Cards**: Single elevation level without nested cards. Clear border strokes (`1dp`) in dark theme for spatial separation without heavy drop shadows.
- **Polyline & Map View**:
  - Polyline width: `6dp` in MapLibre with casing.
  - Start marker: Emerald Pin (`#00E676`).
  - Stop markers: Tonal Amber Pins with stop type indicators.
  - Current location / End marker: Vibrant Blue / Red Pin.
  - Interactive bounds fitting protecting against single-point coordinate bounds.
