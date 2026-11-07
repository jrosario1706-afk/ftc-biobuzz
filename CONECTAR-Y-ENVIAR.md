# 🚀 Conectar el robot y enviar el código desde Android Studio

Guía corta y probada para que **Android Studio vea el Control Hub** y puedas
mandarle el código con el botón ▶️ **Run**.

---

## 1. La forma rápida (3 pasos)

En el **Escritorio** ya tienes tres accesos directos nuevos:

| Paso | Qué hacer |
|---|---|
| 1 | Doble clic en **Android Studio BioBuzz** (abre el Studio bueno con este proyecto). |
| 2 | Enciende el robot y el Driver Station. Doble clic en **1 Conectar robot**. Debe terminar con **ROBOT CONECTADO**. |
| 3 | En Android Studio, arriba a la derecha elige **`TeamCode`** y pulsa ▶️ **Run**. |

Si el paso 2 dice **EL ROBOT NO APARECE**, mira la sección 3.

> Alternativa sin accesos directos: `ABRIR-ANDROID-STUDIO.cmd` y `1-CONECTAR-ROBOT.cmd`
> dentro de la carpeta del proyecto (hacen lo mismo).

---

## 2. Cómo conectar el robot

### Opción A — WiFi (la que usabas: 192.168.43.1:5555)

1. Enciende el **Control Hub** y espera a que su LED parpadee.
2. En Windows, haz clic en el icono de WiFi y conéctate a la red **`FTC-xxxx`**
   (la del robot). Windows dirá *"Sin internet"* → es normal.
3. Cuando estés en esa red, tu laptop tendrá una IP `192.168.43.x`.
4. Ejecuta **`1-CONECTAR-ROBOT.cmd`**.

> Si tu laptop no cambia de red (Windows vuelve solo a la red de casa), desconecta
> la red de casa con "Olvidar" temporalmente o usa el cable USB.

### Opción B — Cable USB-C

1. Conecta el cable de la laptop al **Control Hub** (puerto USB-C).
2. Usa un cable **de datos**; los de solo carga no sirven.
3. Si el Driver Station pregunta **"¿Permitir depuración USB?"**, acepta.
4. Ejecuta **`1-CONECTAR-ROBOT.cmd -Modo usb`** o simplemente el `.cmd` normal.
5. La primera vez Windows puede tardar en instalar el driver (1-2 minutos).

---

## 3. Si el robot no aparece

Ejecuta `1-CONECTAR-ROBOT.cmd` y lee lo que dice. Los casos típicos:

| Lo que ves | Qué significa | Solución |
|---|---|---|
| `La laptop NO esta en la red del robot (192.168.43.x)` | No estás en el WiFi del robot | Conéctate a `FTC-xxxx` o usa USB |
| `El puerto 5555 no responde` | Estás en la red correcta pero el hub no atiende ADB | Apaga y enciende el Control Hub; si sigue, usa USB |
| `adb dice "offline"` | Conexión WiFi inestable | Desconecta/conecta el WiFi, o usa USB una vez |
| No aparece nada por USB | Cable de carga o driver | Prueba otro cable; revisa Administrador de dispositivos |
| `unauthorized` | Falta aceptar el permiso | Acepta el aviso en el Driver Station y repite |

Después de conectar, en Android Studio el desplegable de dispositivos (arriba a
la derecha) debe mostrar **`REV Robotics Control Hub v1.0`**. Si lo muestra, ▶️ Run.

---

## 4. Enviar el código desde Android Studio (detalle)

1. Abre el proyecto con **`ABRIR-ANDROID-STUDIO.cmd`**.
2. Espera a que termine el *Gradle sync* (barra abajo).
3. Arriba a la derecha: configuración **`TeamCode`** y dispositivo
   **`REV Robotics Control Hub v1.0`**.
4. ▶️ **Run** (o `Shift+F10`).
5. Abajo sale `BUILD SUCCESSFUL` y `Installing APK...`. El robot se reinicia la app.
6. En el **Driver Station**: `Init` → elige tu OpMode → `Play`.

### Sin abrir Android Studio

```powershell
# compila e instala en el robot
.\2-ENVIAR-CODIGO.cmd

# solo compilar (genera el APK)
.\2-ENVIAR-CODIGO.cmd -CompilarSolamente
```

El APK queda en `TeamCode\build\outputs\apk\debug\TeamCode-debug.apk`.

---

## 5. Lo que se arregló en este equipo

- **Tres carpetas de Android Studio**: `Android Studio` (73 MB) y
  `Android Studio1` (87 MB) están **incompletas** (les falta el Java y las
  librerías, por eso no abren). La buena es **`Android Studio2`** (3.4 GB,
  versión 2026.2.1). Los accesos directos nuevos apuntan ahí.
- **`JAVA_HOME` no existía**: por eso compilar desde la terminal fallaba.
  Ahora apunta al Java incluido en Android Studio2.
- **adb con conexiones "offline"**: `1-CONECTAR-ROBOT.cmd` reinicia el servicio
  adb y reintenta limpio (antes Studio arrastraba sesiones muertas).
- **La guía vieja apuntaba a `Android Studio1` (la rota)**: corregida en
  `GUIA-RAPIDA-FTC.md`.
- **Scripts nuevos**: conexión, envío y apertura del proyecto, sin depender de
  recordar comandos.

---

## 6. Reglas para que siga funcionando

1. Siempre abre el Studio con **`ABRIR-ANDROID-STUDIO.cmd`** (o el acceso directo
   **Android Studio BioBuzz**), no con las carpetas viejas.
2. Antes de dar ▶️ Run, ejecuta **`1-CONECTAR-ROBOT.cmd`** y espera
   `ROBOT CONECTADO`.
3. No borres `C:\Users\jhoan\.gradle` ni la caché de `org.firstinspires.ftc`:
   sin internet son las librerías que hacen que compile.
4. Mantén al menos **10 GB libres** en C: (ahora hay ~17 GB).
