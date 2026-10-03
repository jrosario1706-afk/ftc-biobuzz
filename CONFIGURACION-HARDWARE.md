# Configuración de hardware — BIOBUZZ 2026-2027

Guía para nombrar los dispositivos en el **Driver Station** con el nombre exacto
que buscan los OpModes. Si un nombre no coincide, el OpMode falla al iniciar con
`Unable to find device`.

> Origen: los nombres salen directamente del código en
> `TeamCode/src/main/java/org/firstinspires/ftc/teamcode/BioBuzzTeleOpPro.java`
> (`initializeHardware()`).

---

## 1. Crear la configuración en el Driver Station

1. Conecta el Driver Station al Control Hub (USB o WiFi).
2. Abre la app **FTC Driver Station** → ⚙️ **Configure Robot**.
3. Pulsa **New**, nómbrala por ejemplo `BIOBUZZ` y guarda.
4. Añade cada dispositivo de la tabla de abajo, **respetando mayúsculas y minúsculas**.

---

## 2. Dispositivos requeridos

### Motores del chasis — mecanum (4)

| Nombre exacto | Tipo | Puerto sugerido | Dirección en código |
|---|---|---|---|
| `frontLeft`  | Motor | 0 | REVERSE |
| `frontRight` | Motor | 1 | FORWARD |
| `backLeft`   | Motor | 2 | REVERSE |
| `backRight`  | Motor | 3 | FORWARD |

> ⚠️ Las direcciones ya están fijadas en el código (`setDirection`), **no** las
> cambies en el Driver Station. Si el robot gira sobre sí mismo en vez de avanzar,
> intercambia en el Driver Station los pares izquierda/derecha, no el código.

### Intake

| Nombre exacto | Tipo | Puerto sugerido | Comportamiento |
|---|---|---|---|
| `intake` | Motor | 0 | BRAKE, FORWARD |

### Shooter / lanzador

| Nombre exacto | Tipo | Puerto sugerido | Comportamiento |
|---|---|---|---|
| `shooter` | **Motor con encoder** | 1 | FLOAT + RUN_USING_ENCODER |

> ⚠️ **Importante:** el código lo lee como `DcMotorEx` y usa `setVelocity()` y
> `getVelocity()`. Si lo declaras como **Motor** simple (sin encoder), el OpMode
> **falla al iniciar**. Debe ser **Motor Ex** / con encoder.
> Además, `shooter` debe estar en un puerto que soporte encoder.

### Cámara Limelight 3A

| Nombre exacto | Tipo | Notas |
|---|---|---|
| `limelight` | **Limelight3A** | Debe estar en la misma red que el Control Hub |

En el Driver Station el control se añade como **Limelight 3A** (no como webcam).
El nombre por defecto suele ser `limelight`; si el tuyo es distinto, cámbialo a
`limelight` o edita la constante en el código.

---

## 3. Pipelines de Limelight

El código alterna entre dos pipelines (botón **Y** del gamepad):

| Constante en código | Valor | Uso |
|---|---|---|
| `PIPELINE_NEURAL_IA` | `1` | Detección de POLEN / NECTAR (neural detector) |
| `PIPELINE_COLOR` | `0` | Detección por color |

Debes crear esos pipelines en **limelight.local:5801** y asegurarte de que las
clases detectadas se llamen exactamente:

- `pollen` (el código también acepta `polen`)
- `nectar`

Si tus clases tienen otro nombre, la telemetría mostrará `Ninguno` y el auto-aim
no encontrará objetivo.

---

## 4. Constantes de calibración

Están al inicio de `BioBuzzTeleOpPro.java`. Ajústalas a tu robot real:

| Constante | Valor actual | Qué es |
|---|---|---|
| `DRIVE_SPEED` | 0.90 | Velocidad normal de chasis |
| `SLOW_DRIVE_SPEED` | 0.35 | Velocidad en modo lento (bumper izq.) |
| `INTAKE_POWER` | 0.90 | Potencia del intake |
| `MAX_SHOOTER_VELOCITY` | 2000.0 | Velocidad máxima del shooter (ticks/s) |
| `MIN_CONFIDENCE` | 0.40 | Confianza mínima para aceptar una detección |
| `AUTO_AIM_KP` | 0.035 | Ganancia proporcional del auto-aim |
| `AUTO_AIM_MAX_TURN` | 0.45 | Giro máximo del auto-aim |
| `AUTO_AIM_TOLERANCE` | 1.8 | Tolerancia en grados |
| `LIMELIGHT_HEIGHT_CM` | 25.0 | Altura de la Limelight sobre el piso |
| `TARGET_HEIGHT_CM` | 45.0 | Altura del objetivo |
| `LIMELIGHT_MOUNT_ANGLE_DEG` | 15.0 | Ángulo de montaje de la cámara |
| `BASE_SHOOTER_VELOCITY` | 1200.0 | Velocidad base del shooter |
| `DISTANCE_VELOCITY_GAIN` | 12.0 | Compensación por distancia |

> 🔧 **`LIMELIGHT_HEIGHT_CM`, `TARGET_HEIGHT_CM` y `LIMELIGHT_MOUNT_ANGLE_DEG`
> son los más importantes**: si están mal, la distancia calculada es incorrecta y
> el shooter disparará con la velocidad equivocada. Mídelos con cinta métrica.

---

## 5. Controles (gamepad 1 y 2)

| Control | Acción |
|---|---|
| Stick izquierdo | Movimiento (adelante/atrás/lateral) |
| Stick derecho | Giro |
| Bumper izquierdo | Modo lento |
| Botón **X** | Auto-aim (apunta al objetivo y ajusta velocidad) |
| Botón **Y** | Cambiar pipeline neural ↔ color |
| D-pad arriba | Filtrar objetivo: POLEN |
| D-pad abajo | Filtrar objetivo: NECTAR |
| D-pad izquierda | Filtrar objetivo: TODOS |

---

## 6. OpModes disponibles

| Nombre en Driver Station | Clase | Tipo |
|---|---|---|
| `BioBuzz TeleOp Competition` | `BioBuzzTeleOpPro` | TeleOp |
| `BioBuzz Autonomo` | `biobuzzautonomo` | Autonomous |

---

## 7. Si algo no funciona

| Síntoma | Causa probable |
|---|---|
| `Unable to find device "shooter"` | No está declarado como motor con encoder |
| `Unable to find device "limelight"` | Cámara no añadida como Limelight 3A |
| Telemetría muestra `Ninguno` | Nombres de clase del detector ≠ `pollen`/`nectar` |
| El robot gira en vez de avanzar | Direcciones de motor; ajústalas en Driver Station |
| Distancia del shooter incorrecta | Revisa las 3 constantes de altura/ángulo |
| El APK no aparece en Driver Station | Se subió a la app equivocada o falló la instalación |
