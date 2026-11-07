package org.jhoandri.pathing;

import com.acmerobotics.dashboard.config.Config;

/**
 * Valores de arranque para el chasis BioBuzz. Se cambian desde FTC Dashboard
 * mientras corre el OpMode de calibracion. Los valores finales deben salir de
 * mediciones sobre la cancha, no de una estimacion en el codigo.
 */
@Config
public final class JhoandriPathingConstants {
    private JhoandriPathingConstants() {
    }

    // Encoder y rueda de traccion. Confirma TICKS_PER_REV con la etiqueta de tus motores.
    public static double WHEEL_RADIUS_IN = 1.8898; // rueda mecanum de 96 mm
    public static double WHEEL_ROTATIONS_PER_MOTOR_REV = 1.0;
    public static double TICKS_PER_REV = 537.7; // GoBILDA 312 RPM: valor inicial comun

    // Multiplicadores que se ajustan con cinta metrica en el OpMode de calibracion.
    public static double FORWARD_MULTIPLIER = 1.0;
    public static double STRAFE_MULTIPLIER = 1.0;
    public static double STRAFE_SIGN = 1.0;

    // Cambia estos signos solamente si la telemetria muestra un encoder invertido.
    public static double LEFT_FRONT_ENCODER_SIGN = 1.0;
    public static double LEFT_REAR_ENCODER_SIGN = 1.0;
    public static double RIGHT_FRONT_ENCODER_SIGN = 1.0;
    public static double RIGHT_REAR_ENCODER_SIGN = 1.0;

    // Control de posicion. Empieza lento y sube solo despues de calibrar la pose.
    // Control de posicion y velocidad optimizado para reducir tiempos de ciclo
    public static double TRANSLATION_KP = 0.085;
    public static double HEADING_KP = 1.80;
    public static double MIN_TRANSLATION_POWER = 0.12;
    public static double MAX_TRANSLATION_POWER = 0.75; // Incrementado de 0.45 a 0.75 para desplazamientos rapidos
    public static double MIN_TURN_POWER = 0.10;
    public static double MAX_TURN_POWER = 0.55;        // Incrementado de 0.35 a 0.55 para giros agiles

    public static double POSITION_TOLERANCE_IN = 0.85;
    public static double HEADING_TOLERANCE_RAD = Math.toRadians(2.0);
}
