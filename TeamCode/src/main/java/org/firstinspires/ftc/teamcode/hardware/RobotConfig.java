package org.firstinspires.ftc.teamcode.hardware;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;

/**
 * UNICO lugar donde se configuran los nombres de hardware y los valores del robot
 * BioBuzz 24096. Todos los OpModes leen de aqui, asi que un cambio se aplica a todos.
 *
 * Los valores "public static" (no final) se pueden ajustar en vivo desde FTC Dashboard
 * (http://192.168.43.1:8080/dash) sin volver a instalar la app.
 */
@Config
public final class RobotConfig {
    private RobotConfig() {
    }

    // =========================================================
    // NOMBRES EN LA CONFIGURACION DEL DRIVER STATION
    // Se prueba cada nombre en orden; el primero que exista se usa.
    // Asi funcionan tanto las configuraciones viejas como las nuevas.
    // =========================================================
    public static final String[] FRONT_LEFT  = {"frontLeft", "leftFront"};
    public static final String[] FRONT_RIGHT = {"frontRight", "rightFront"};
    public static final String[] BACK_LEFT   = {"backLeft", "leftBack", "leftRear"};
    public static final String[] BACK_RIGHT  = {"backRight", "rightBack", "rightRear"};

    public static final String[] INTAKE   = {"intake", "intakeMotor", "intake1"};
    public static final String[] TRANSFER = {"transferMotor", "transfer", "feederMotor"};
    public static final String[] SHOOTER  = {"shooter", "shooterMotor", "flywheel"};
    public static final String[] FEEDER_SERVO = {"feederServo", "feeder"};

    public static final String[] IMU_NAMES = {"imu"};
    public static final String[] LIMELIGHT = {"limelight"};

    // =========================================================
    // ORIENTACION DEL CONTROL HUB (debe coincidir con como esta montado)
    // =========================================================
    public static RevHubOrientationOnRobot.LogoFacingDirection HUB_LOGO =
            RevHubOrientationOnRobot.LogoFacingDirection.UP;
    public static RevHubOrientationOnRobot.UsbFacingDirection HUB_USB =
            RevHubOrientationOnRobot.UsbFacingDirection.RIGHT;

    // =========================================================
    // CHASIS
    // =========================================================
    /** Las ruedas mecanum se deslizan al hacer strafe; esto lo compensa. */
    public static double STRAFE_CORRECTION = 1.1;
    /** Ignora movimientos muy pequenos del joystick (evita que el robot "camine" solo). */
    public static double STICK_DEADBAND = 0.05;

    // =========================================================
    // MECANISMOS
    // =========================================================
    public static double INTAKE_POWER = 0.90;
    public static double TRANSFER_POWER = 1.0;

    /** Velocidad maxima del shooter en ticks/seg (TeleOp con gatillo). */
    public static double SHOOTER_MAX_VELOCITY = 2000;
    /** Velocidad de disparo fija para autonomos y disparo automatico. */
    public static double SHOOTER_TARGET_VELOCITY = 1850;
    /** Margen (ticks/seg) para considerar que el shooter esta listo. */
    public static double SHOOTER_READY_TOLERANCE = 100;

    public static double FEEDER_SERVO_RETRACTED = 0.0;
    public static double FEEDER_SERVO_PUSH = 0.7;

    // =========================================================
    // VISION (Limelight 3A)
    // =========================================================
    /** Pipeline del detector neuronal (polen / nectar). */
    public static int LIMELIGHT_DETECTOR_PIPELINE = 1;
    /** Confianza minima en escala 0..1. */
    public static double MIN_DETECTION_CONFIDENCE = 0.40;
    /** Resultados mas viejos que esto (ms) se descartan. */
    public static long MAX_VISION_STALENESS_MS = 250;
}
