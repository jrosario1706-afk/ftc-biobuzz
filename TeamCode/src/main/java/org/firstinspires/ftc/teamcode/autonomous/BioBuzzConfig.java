package org.firstinspires.ftc.teamcode.autonomous;

import com.acmerobotics.dashboard.config.Config;

@Config
public class BioBuzzConfig {

    // =========================================================
    // CONFIGURACIÓN DE MECANISMOS (Shooter & Intake)
    // =========================================================

    public static double SHOOTER_VELOCITY = 1850;
    public static double SHOOTER_TOLERANCE = 85;
    public static double INTAKE_POWER = 0.90;

    // Tiempos optimizados de ciclo para maximizar puntuacion en autonomo
    public static long SHOOT_TIME_MS = 450;                 // Reducido de 900ms a 450ms
    public static long INTAKE_TIME_MS = 850;                // Reducido de 1200ms a 850ms
    public static long SHOOTER_SPINUP_TIMEOUT_MS = 800;     // Reducido de 1500ms a 800ms
    public static long PATH_TIMEOUT_MS = 6500;

    // =========================================================
    // CONFIGURACIÓN DE LOCALIZACIÓN Y VISIÓN (Limelight 3A / MegaTag2)
    // =========================================================

    public static boolean ENABLE_VISION_CORRECTION = true;
    public static boolean ENABLE_MEGA_TAG2 = true;
    public static double MAX_POSE_JUMP = 15.0;      // Pulgadas
    public static double MAX_HEADING_JUMP = 35.0;   // Grados

    public static boolean ENABLE_DECISION_ENGINE = true;
    public static int MAX_SHOOT_RETRIES = 2;

    // =========================================================
    // CONFIGURACIÓN DE SEGUIMIENTO DE BOLAS AMARILLAS
    // =========================================================

    public static boolean ENABLE_BALL_TRACKING = true;
    public static int BALL_PIPELINE_INDEX = 1;      // Pipeline de Limelight para esferas amarillas
    public static double BALL_TX_TOLERANCE = 1.5;    // Tolerancia horizontal (grados)
    public static double BALL_ALIGN_GAIN = 0.5;      // Ganancia proporcional de giro
    public static long BALL_SEARCH_TIMEOUT_MS = 600; // Timeout de búsqueda visual
    public static double BALL_APPROACH_EXTRA_TIME_MS = 1000;
}
