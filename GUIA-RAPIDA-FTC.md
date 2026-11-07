# 🛠️ Guía rápida — Programador FTC (BioBuzz)

Todo lo que usas en el día a día, en un solo lugar.

---

## 📁 Rutas importantes (copiar y pegar)

| Qué | Ruta |
|---|---|
| **Proyecto FTC** | `C:\Users\jhoan\OneDrive\Documents\deepseek-harness\default-workspace\FtcRobotController-BIOBUZZ` |
| **Tu código (OpModes)** | `...\FtcRobotController-BIOBUZZ\TeamCode\src\main\java\org\firstinspires\ftc\teamcode\` |
| **APK compilado** | `...\FtcRobotController-BIOBUZZ\TeamCode\build\outputs\apk\debug\TeamCode-debug.apk` |
| **Android Studio (el bueno)** | `C:\Program Files\Android\Android Studio2\bin\studio64.exe` |
| **Java (JDK)** | `C:\Program Files\Android\Android Studio2\jbr` |
| **Abrir el proyecto** | `...\FtcRobotController-BIOBUZZ\ABRIR-ANDROID-STUDIO.cmd` |
| **Guía de conexión** | `...\CONECTAR-Y-ENVIAR.md` |
| **Android SDK** | `C:\Users\jhoan\AppData\Local\Android\Sdk` |
| **ADB (conectar robot)** | `C:\Users\jhoan\AppData\Local\Android\Sdk\platform-tools\adb.exe` |
| **Guía de hardware** | `...\FtcRobotController-BIOBUZZ\CONFIGURACION-HARDWARE.md` |

> ⚠️ La carpeta buena de Android Studio es **`Android Studio2`**. Las carpetas
> `Android Studio` y `Android Studio1` están incompletas (no abren): no las uses.

---

## 🖥️ Tus OpModes

| Nombre en Driver Station | Archivo | Tipo |
|---|---|---|
| `BioBuzz TeleOp Competition` | `BioBuzzTeleOpPro.java` | TeleOp |
| `BioBuzz Autonomo` | `biobuzzautonomo.java` | Autonomous |

---

## ⚙️ Nombres de hardware (Driver Station)

| Nombre exacto | Tipo | Ojo |
|---|---|---|
| `frontLeft` | Motor | mecanum |
| `frontRight` | Motor | mecanum |
| `backLeft` | Motor | mecanum |
| `backRight` | Motor | mecanum |
| `intake` | Motor | |
| `shooter` | **Motor CON encoder** | ⚠️ debe ser DcMotorEx, no motor simple |
| `limelight` | Limelight 3A | |

---

## ▶️ Compilar e instalar al robot

### Método 1 — Android Studio (recomendado)
1. Doble clic en **`ABRIR-ANDROID-STUDIO.cmd`** (abre el Studio bueno).
2. Doble clic en **`1-CONECTAR-ROBOT.cmd`** y espera `ROBOT CONECTADO`.
3. Arriba a la derecha elige **`TeamCode`** y pulsa **▶️ Run**.

> Detalle paso a paso (WiFi y USB) en **`CONECTAR-Y-ENVIAR.md`**.

### Método 2 — Terminal (sin abrir Android Studio)

Doble clic en **`2-ENVIAR-CODIGO.cmd`**. O desde PowerShell:

```powershell
cd "C:\Users\jhoan\OneDrive\Documents\deepseek-harness\default-workspace\FtcRobotController-BIOBUZZ"
$env:JAVA_HOME = "C:\Program Files\Android\Android Studio2\jbr"

# Solo compilar (genera el APK)
.\gradlew.bat :TeamCode:assembleDebug

# Compilar Y subir al robot (robot ya conectado)
.\gradlew.bat :TeamCode:installDebug
```

### Método 3 — Instalar el APK directo con ADB

```powershell
& "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe" install -r "TeamCode\build\outputs\apk\debug\TeamCode-debug.apk"
```

### Ver si el robot está conectado

```powershell
.\1-CONECTAR-ROBOT.cmd
```
Ese script prueba USB y WiFi, reinicia adb si hace falta y te dice qué falla.
Si `adb devices` sale vacío → no hay robot conectado.

---

## 🐛 Problemas comunes

| Síntoma | Causa / Solución |
|---|---|
| `Unable to find device "shooter"` | Decláralo como motor **con encoder** en el Driver Station |
| `Unable to find device "limelight"` | No está añadida como Limelight 3A |
| Telemetría dice `Ninguno` | Las clases del detector no se llaman `pollen`/`nectar` |
| Build dice `could not resolve` | La red está bloqueada; dependencias en caché local |
| `out of memory` / `insufficient memory` | Disco lleno → libera espacio (mínimo 10 GB libres) |
| `adb devices` vacío | Conecta el Control Hub por USB |
| El robot gira en vez de avanzar | Direcciones de motor en el Driver Station |

---

## 💾 Reglas para que tu laptop no falle

1. **Mantén siempre 10-15 GB libres de disco.** Si baja de 10 GB, los builds fallan.
2. **No borres** la carpeta `.gradle` ni `org.firstinspires.ftc` — son las librerías FTC que funcionan sin internet.
3. **La red está bloqueada** en este equipo. Si un día pide descargar algo, es por eso (revisa el error antes de asustarte).
4. **`JAVA_HOME`** siempre debe apuntar a `C:\Program Files\Android\Android Studio1\jbr` al compilar por terminal.

---

## 🔄 Flujo de trabajo diario

1. Edita tu OpMode en `TeamCode\...\teamcode\`.
2. Compila (Android Studio ▶️ o `assembleDebug`).
3. Conecta el Control Hub por USB.
4. Sube al robot (`installDebug` o Run).
5. Configura/verifica los nombres en el Driver Station.
6. Prueba en el robot y ajusta las constantes al inicio de `BioBuzzTeleOpPro.java`.
