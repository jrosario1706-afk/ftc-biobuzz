package org.firstinspires.ftc.teamcode;

// ============================================================================
//  ARCHIVO GENERADO POR fyf_generator.py -- NO EDITAR A MANO
//  Constantes de BioBuzz (equipo 24096, 2026-2027)
//
//  Para cambiar un numero: edita tools/ftc/bio.robot.json y ejecuta
//      python tools/ftc/fyf_generator.py generar todo
// ============================================================================

import com.qualcomm.robotcore.hardware.HardwareMap;

public class BioBuzzConfig {

    /** velocidad normal del chasis */
    public static double DRIVE_SPEED = 0.9;

    /** modo lento */
    public static double SLOW_DRIVE_SPEED = 0.35;

    /** zona muerta de los sticks */
    public static double DEADBAND = 0.05;

    /** potencia del intake */
    public static double INTAKE_POWER = 0.9;

    /** ticks/s maximos del shooter */
    public static double MAX_SHOOTER_VELOCITY = 2000;

    /** Coeficientes PIDF de velocidad para DcMotorEx del shooter (REV Hub) */
    public static double SHOOTER_PIDF_P = 15.0;
    public static double SHOOTER_PIDF_I = 0.5;
    public static double SHOOTER_PIDF_D = 2.0;
    public static double SHOOTER_PIDF_F = 13.5;

    /** Multiplicador cinematico para compensar friccion lateral en ruedas Mecanum */
    public static double STRAFE_MULTIPLIER = 1.12;

    /** ticks/s base */
    public static double BASE_SHOOTER_VELOCITY = 1200;

    /** compensacion por distancia */
    public static double DISTANCE_VELOCITY_GAIN = 12;

    /** margen para considerar el shooter listo */
    public static double SHOOTER_TOLERANCIA_TICKS = 85;

    /** rampa del shooter en ticks/s^2 */
    public static double SHOOTER_ACELERACION_TICKS_S2 = 4200;

    /** confianza minima de deteccion */
    public static double MIN_CONFIDENCE = 0.40;

    /** ganancia proporcional del auto-aim */
    public static double AUTO_AIM_KP = 0.032;

    /** compensacion de friccion estatica (feedforward kS) */
    public static double AUTO_AIM_KS = 0.075;

    /** ganancia derivativa kD para amortiguar oscilaciones */
    public static double AUTO_AIM_KD = 0.0035;

    /** factor exponencial para suavizado de sticks (0 = lineal, 1 = cubico) */
    public static double DRIVE_EXPO_FACTOR = 0.65;

    /** filtro de aceleracion chasis (potencia/seg) para proteger traccion y evitar patinaje */
    public static double SLEW_RATE_LIMIT = 4.0;
    public static double SLEW_RATE_ACCEL = 4.0;

    /** limite de desaceleracion para frenado agil y sin derrape */
    public static double SLEW_RATE_DECEL = 10.0;

    /** Tension nominal de referencia para compensacion de bateria (V) */
    public static double VOLTAJE_NOMINAL = 12.0;

    /** Velocidad base de pre-giro (idle) del shooter durante transitos en autonomo */
    public static double IDLE_SHOOTER_VELOCITY = 850.0;

    /** factor de suavizado exponencial para Limelight (0.1..1.0) */
    public static double VISION_SMOOTHING_ALPHA = 0.75;

    /** giro maximo del auto-aim */
    public static double AUTO_AIM_MAX_TURN = 0.45;

    /** tolerancia de apuntado en grados */
    public static double AUTO_AIM_TOLERANCE = 1.8;

    /** altura de la camara */
    public static double LIMELIGHT_HEIGHT_CM = 25;

    /** altura del objetivo */
    public static double TARGET_HEIGHT_CM = 45;

    /** angulo de montaje de la camara */
    public static double LIMELIGHT_MOUNT_ANGLE_DEG = 15;

    /** duracion del TeleOp */
    public static double DURACION_PARTIDA_S = 120;

    /** duracion del autonomo */
    public static double DURACION_AUTONOMO_S = 30;

    /** distancia de tiro preferida */
    public static double DISTANCIA_IDEAL_CM = 180;

    /** margen de distancia aceptable */
    public static double MARGEN_DISTANCIA_CM = 12;

    /** margen de apuntado fino */
    public static double MARGEN_APUNTADO_GRADOS = 2.5;

    /** segundos finales en los que la IA se repliega */
    public static double REPLEGAR_ULTIMOS_S = 12;

    /** peso IA */
    public static double PESO_TENGO_PIEZA = 1.4;

    /** peso IA */
    public static double PESO_TENGO_OBJETIVO = 1.6;

    /** peso IA */
    public static double PESO_APUNTADO = 2;

    /** peso IA */
    public static double PESO_DISTANCIA_CORRECTA = 1.8;

    /** peso IA */
    public static double PESO_LANZADOR_LISTO = 2.4;

    /** peso IA */
    public static double PESO_TIEMPO_RESTANTE = 0.8;

    /** peso IA */
    public static double PESO_SIN_OBJETIVO = 1.2;

    /** Pipeline con el detector neuronal (pollen / nectar). */
    public static int PIPELINE_NEURAL_IA = 1;
    /** Pipeline de deteccion por color. */
    public static int PIPELINE_COLOR = 0;

    /** Pesos del selector de comportamiento de la IA. */
    public static Pesos pesosCerebro() {
        Pesos p = new Pesos();
        p.tengoPieza = PESO_TENGO_PIEZA;
        p.tengoObjetivo = PESO_TENGO_OBJETIVO;
        p.apuntado = PESO_APUNTADO;
        p.distanciaCorrecta = PESO_DISTANCIA_CORRECTA;
        p.lanzadorListo = PESO_LANZADOR_LISTO;
        p.tiempoRestante = PESO_TIEMPO_RESTANTE;
        p.sinObjetivo = PESO_SIN_OBJETIVO;
        p.distanciaIdealCm = DISTANCIA_IDEAL_CM;
        p.margenDistanciaCm = MARGEN_DISTANCIA_CM;
        p.margenApuntadoGrados = MARGEN_APUNTADO_GRADOS;
        p.replegarEnUltimosS = REPLEGAR_ULTIMOS_S;
        return p;
    }

    private static final java.io.File ARCHIVO =
            new java.io.File("/sdcard/FIRST/biobuzzconfig.txt");

    /** Copia los valores vivos del tuner a un archivo del robot. */
    public static void guardarPreferencias(HardwareMap hardwareMap) {
        try {
            java.io.File padre = ARCHIVO.getParentFile();
            if (padre != null && !padre.exists()) {
                padre.mkdirs();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("DRIVE_SPEED=").append(DRIVE_SPEED).append('\n');
            sb.append("SLOW_DRIVE_SPEED=").append(SLOW_DRIVE_SPEED).append('\n');
            sb.append("INTAKE_POWER=").append(INTAKE_POWER).append('\n');
            sb.append("MAX_SHOOTER_VELOCITY=").append(MAX_SHOOTER_VELOCITY).append('\n');
            sb.append("BASE_SHOOTER_VELOCITY=").append(BASE_SHOOTER_VELOCITY).append('\n');
            sb.append("DISTANCE_VELOCITY_GAIN=").append(DISTANCE_VELOCITY_GAIN).append('\n');
            sb.append("MIN_CONFIDENCE=").append(MIN_CONFIDENCE).append('\n');
            sb.append("AUTO_AIM_KP=").append(AUTO_AIM_KP).append('\n');
            sb.append("AUTO_AIM_MAX_TURN=").append(AUTO_AIM_MAX_TURN).append('\n');
            sb.append("AUTO_AIM_TOLERANCE=").append(AUTO_AIM_TOLERANCE).append('\n');
            sb.append("LIMELIGHT_HEIGHT_CM=").append(LIMELIGHT_HEIGHT_CM).append('\n');
            sb.append("TARGET_HEIGHT_CM=").append(TARGET_HEIGHT_CM).append('\n');
            sb.append("LIMELIGHT_MOUNT_ANGLE_DEG=")
                    .append(LIMELIGHT_MOUNT_ANGLE_DEG).append('\n');
            java.io.FileWriter w = new java.io.FileWriter(ARCHIVO, false);
            w.write(sb.toString());
            w.close();
        } catch (Exception ignored) {
            // si no se puede escribir, el tuner sigue funcionando en memoria
        }
    }

    /** Recupera los valores guardados por el tuner (si existen). */
    public static void cargarPreferencias(HardwareMap hardwareMap) {
        if (!ARCHIVO.exists()) {
            return;
        }
        try {
            java.io.BufferedReader r = new java.io.BufferedReader(
                    new java.io.FileReader(ARCHIVO));
            String linea;
            while ((linea = r.readLine()) != null) {
                int i = linea.indexOf('=');
                if (i <= 0) {
                    continue;
                }
                String clave = linea.substring(0, i).trim();
                double valor = Double.parseDouble(linea.substring(i + 1).trim());
                switch (clave) {
                    case "DRIVE_SPEED": DRIVE_SPEED = valor; break;
                    case "SLOW_DRIVE_SPEED": SLOW_DRIVE_SPEED = valor; break;
                    case "INTAKE_POWER": INTAKE_POWER = valor; break;
                    case "MAX_SHOOTER_VELOCITY": MAX_SHOOTER_VELOCITY = valor; break;
                    case "BASE_SHOOTER_VELOCITY": BASE_SHOOTER_VELOCITY = valor; break;
                    case "DISTANCE_VELOCITY_GAIN": DISTANCE_VELOCITY_GAIN = valor; break;
                    case "MIN_CONFIDENCE": MIN_CONFIDENCE = valor; break;
                    case "AUTO_AIM_KP": AUTO_AIM_KP = valor; break;
                    case "AUTO_AIM_MAX_TURN": AUTO_AIM_MAX_TURN = valor; break;
                    case "AUTO_AIM_TOLERANCE": AUTO_AIM_TOLERANCE = valor; break;
                    case "LIMELIGHT_HEIGHT_CM": LIMELIGHT_HEIGHT_CM = valor; break;
                    case "TARGET_HEIGHT_CM": TARGET_HEIGHT_CM = valor; break;
                    case "LIMELIGHT_MOUNT_ANGLE_DEG":
                        LIMELIGHT_MOUNT_ANGLE_DEG = valor; break;
                    default: break;
                }
            }
            r.close();
        } catch (Exception ignored) {
            // valores corruptos: se mantienen los del codigo
        }
    }
}
