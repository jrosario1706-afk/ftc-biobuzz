package org.firstinspires.ftc.teamcode.hardware;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Hardware compartido del robot BioBuzz 24096.
 *
 * Antes cada OpMode configuraba sus propios motores (y con nombres distintos:
 * "intake" vs "intakeMotor", "shooter" vs "shooterMotor"), por lo que algunos
 * OpModes fallaban al dar INIT. Ahora todos usan esta clase.
 *
 * - Chasis: obligatorio (si falta, el error dice exactamente que nombre falta).
 * - Mecanismos, IMU y Limelight: opcionales (si faltan quedan en null y el
 *   resto del robot sigue funcionando).
 */
public class RobotHardware {

    // ----- Chasis (obligatorio) -----
    public final DcMotorEx frontLeft;
    public final DcMotorEx frontRight;
    public final DcMotorEx backLeft;
    public final DcMotorEx backRight;

    // ----- Mecanismos (opcionales: pueden ser null) -----
    public final DcMotorEx intake;
    public final DcMotorEx transfer;
    public final DcMotorEx shooter;
    public final Servo feederServo;

    // ----- Sensores (opcionales: pueden ser null) -----
    public final IMU imu;
    public final Limelight3A limelight;

    private final List<String> report = new ArrayList<>();

    public RobotHardware(HardwareMap hw) {
        this(hw, true);
    }

    /**
     * @param enableLimelight false para no iniciar la camara (por ejemplo si otra
     *                        clase como JhoandriVision ya la administra).
     */
    public RobotHardware(HardwareMap hw, boolean enableLimelight) {
        frontLeft  = required(hw, DcMotorEx.class, "frontLeft", RobotConfig.FRONT_LEFT);
        frontRight = required(hw, DcMotorEx.class, "frontRight", RobotConfig.FRONT_RIGHT);
        backLeft   = required(hw, DcMotorEx.class, "backLeft", RobotConfig.BACK_LEFT);
        backRight  = required(hw, DcMotorEx.class, "backRight", RobotConfig.BACK_RIGHT);

        intake      = optional(hw, DcMotorEx.class, "Intake", RobotConfig.INTAKE);
        transfer    = optional(hw, DcMotorEx.class, "Transfer", RobotConfig.TRANSFER);
        shooter     = optional(hw, DcMotorEx.class, "Shooter", RobotConfig.SHOOTER);
        feederServo = optional(hw, Servo.class, "Feeder servo", RobotConfig.FEEDER_SERVO);
        imu         = optional(hw, IMU.class, "IMU", RobotConfig.IMU_NAMES);
        limelight   = enableLimelight
                ? optional(hw, Limelight3A.class, "Limelight", RobotConfig.LIMELIGHT)
                : null;

        // Chasis mecanum: lado izquierdo invertido.
        frontLeft.setDirection(DcMotorSimple.Direction.REVERSE);
        backLeft.setDirection(DcMotorSimple.Direction.REVERSE);
        frontRight.setDirection(DcMotorSimple.Direction.FORWARD);
        backRight.setDirection(DcMotorSimple.Direction.FORWARD);
        for (DcMotorEx m : new DcMotorEx[]{frontLeft, frontRight, backLeft, backRight}) {
            m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            m.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            m.setPower(0);
        }

        if (intake != null) {
            intake.setDirection(DcMotorSimple.Direction.FORWARD);
            intake.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            intake.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            intake.setPower(0);
        }
        if (transfer != null) {
            transfer.setDirection(DcMotorSimple.Direction.FORWARD);
            transfer.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            transfer.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
            transfer.setPower(0);
        }
        if (shooter != null) {
            shooter.setDirection(DcMotorSimple.Direction.FORWARD);
            // FLOAT: el volante se detiene solo, sin frenar bruscamente la caja.
            shooter.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
            shooter.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            shooter.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            try {
                com.qualcomm.robotcore.hardware.PIDFCoefficients pidf =
                        new com.qualcomm.robotcore.hardware.PIDFCoefficients(
                                15.0, 0.5, 2.0, 13.5
                        );
                shooter.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, pidf);
            } catch (Exception ignored) {
            }
        }
        if (feederServo != null) {
            feederServo.setPosition(RobotConfig.FEEDER_SERVO_RETRACTED);
        }

        if (imu != null) {
            imu.initialize(new IMU.Parameters(new RevHubOrientationOnRobot(
                    RobotConfig.HUB_LOGO, RobotConfig.HUB_USB)));
            imu.resetYaw();
        }

        if (limelight != null) {
            try {
                limelight.pipelineSwitch(RobotConfig.LIMELIGHT_DETECTOR_PIPELINE);
                limelight.start();
            } catch (RuntimeException e) {
                report.add("Limelight: error al iniciar (" + e.getMessage() + ")");
            }
        }
    }

    // =========================================================
    // CHASIS
    // =========================================================

    /**
     * Manejo relativo al robot.
     *
     * @param forward adelante positivo (-gamepad.left_stick_y)
     * @param strafe  derecha positivo (gamepad.left_stick_x)
     * @param turn    horario positivo (gamepad.right_stick_x)
     * @param speed   multiplicador 0..1
     */
    public void drive(double forward, double strafe, double turn, double speed) {
        forward = deadband(forward);
        strafe = deadband(strafe) * RobotConfig.STRAFE_CORRECTION;
        turn = deadband(turn);

        double fl = forward + strafe + turn;
        double bl = forward - strafe + turn;
        double fr = forward - strafe - turn;
        double br = forward + strafe - turn;

        // Normaliza para que ninguna rueda pase de 1.0 sin perder la direccion.
        double max = Math.max(1.0, Math.max(Math.max(Math.abs(fl), Math.abs(bl)),
                Math.max(Math.abs(fr), Math.abs(br))));
        speed = Range.clip(speed, 0.0, 1.0);

        frontLeft.setPower(fl / max * speed);
        backLeft.setPower(bl / max * speed);
        frontRight.setPower(fr / max * speed);
        backRight.setPower(br / max * speed);
    }

    /** Manejo relativo a la cancha: "adelante" siempre es lejos del piloto. */
    public void driveFieldCentric(double forward, double strafe, double turn, double speed) {
        double heading = getHeadingRad();
        double rotStrafe = strafe * Math.cos(-heading) - forward * Math.sin(-heading);
        double rotForward = strafe * Math.sin(-heading) + forward * Math.cos(-heading);
        drive(rotForward, rotStrafe, turn, speed);
    }

    public void stopDrive() {
        frontLeft.setPower(0);
        frontRight.setPower(0);
        backLeft.setPower(0);
        backRight.setPower(0);
    }

    /** Heading en radianes (antihorario positivo). 0 si no hay IMU. */
    public double getHeadingRad() {
        if (imu == null) return 0.0;
        return imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.RADIANS);
    }

    public void resetHeading() {
        if (imu != null) imu.resetYaw();
    }

    public boolean hasImu() {
        return imu != null;
    }

    // =========================================================
    // MECANISMOS (seguros aunque el motor no exista)
    // =========================================================

    public void setIntakePower(double power) {
        if (intake != null) intake.setPower(Range.clip(power, -1, 1));
    }

    public void setTransferPower(double power) {
        if (transfer != null) transfer.setPower(Range.clip(power, -1, 1));
    }

    public void setShooterVelocity(double ticksPerSecond) {
        if (shooter != null) {
            if (ticksPerSecond <= 0.0) {
                shooter.setVelocity(0.0);
                shooter.setPower(0.0);
            } else {
                shooter.setVelocity(ticksPerSecond);
            }
        }
    }

    public double getShooterVelocity() {
        return shooter == null ? 0.0 : shooter.getVelocity();
    }

    /** true cuando el shooter esta girando a la velocidad pedida (+- tolerancia). */
    public boolean isShooterReady(double targetVelocity) {
        return shooter != null && targetVelocity > 0
                && Math.abs(shooter.getVelocity() - targetVelocity)
                <= RobotConfig.SHOOTER_READY_TOLERANCE;
    }

    public void setFeeder(boolean push) {
        if (feederServo != null) {
            feederServo.setPosition(push
                    ? RobotConfig.FEEDER_SERVO_PUSH
                    : RobotConfig.FEEDER_SERVO_RETRACTED);
        }
    }

    /** Empuja la pieza hacia el shooter con todo lo que exista (intake, transfer, servo). */
    public void feed(boolean on) {
        setIntakePower(on ? RobotConfig.INTAKE_POWER : 0);
        setTransferPower(on ? RobotConfig.TRANSFER_POWER : 0);
        setFeeder(on);
    }

    public void stopMechanisms() {
        setIntakePower(0);
        setTransferPower(0);
        setShooterVelocity(0);
        setFeeder(false);
    }

    public void stopAll() {
        stopDrive();
        stopMechanisms();
    }

    /** Llamar al final del OpMode. */
    public void shutdown() {
        try {
            stopAll();
        } catch (RuntimeException ignored) {
            // Nunca impedir que el OpMode termine.
        }
        if (limelight != null) {
            try {
                limelight.stop();
            } catch (RuntimeException ignored) {
                // Idem.
            }
        }
    }

    // =========================================================
    // VISION
    // =========================================================

    /** Resultado mas cercano al centro de la imagen, o null si no hay nada fiable. */
    public VisionTarget getClosestTarget() {
        LLResult result = getFreshResult();
        if (result == null) return null;

        List<LLResultTypes.DetectorResult> detections = result.getDetectorResults();
        if (detections == null) return null;

        VisionTarget best = null;
        int pollen = 0;
        int nectar = 0;
        double bestDist = Double.MAX_VALUE;

        for (LLResultTypes.DetectorResult det : detections) {
            if (det == null) continue;
            double conf = normalizeConfidence(det.getConfidence());
            if (conf < RobotConfig.MIN_DETECTION_CONFIDENCE) continue;

            String name = det.getClassName() == null
                    ? "" : det.getClassName().toLowerCase(Locale.ROOT);
            if (VisionTarget.isPollen(name)) pollen++;
            if (VisionTarget.isNectar(name)) nectar++;

            double tx = det.getTargetXDegrees();
            double ty = det.getTargetYDegrees();
            double dist = Math.abs(tx) + Math.abs(ty);
            if (dist < bestDist) {
                bestDist = dist;
                best = new VisionTarget(name, tx, ty, conf, det.getTargetArea());
            }
        }

        if (best != null) {
            best.pollenCount = pollen;
            best.nectarCount = nectar;
        }
        return best;
    }

    /** Ultimo resultado valido de la Limelight que no sea viejo. */
    public LLResult getFreshResult() {
        if (limelight == null) return null;
        try {
            LLResult r = limelight.getLatestResult();
            if (r == null || !r.isValid()) return null;
            // getStaleness() mide la edad real del dato (antes se comparaba con
            // un reloj distinto y casi siempre se descartaba o aceptaba mal).
            if (r.getStaleness() > RobotConfig.MAX_VISION_STALENESS_MS) return null;
            return r;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** La Limelight puede reportar confianza 0..1 o 0..100; siempre devuelve 0..1. */
    public static double normalizeConfidence(double conf) {
        if (!Double.isFinite(conf)) return 0.0;
        return conf > 1.0 ? conf / 100.0 : conf;
    }

    // =========================================================
    // TELEMETRIA / DIAGNOSTICO
    // =========================================================

    /** Muestra que hardware se encontro y con que nombre. Util en INIT. */
    public void reportTo(Telemetry telemetry) {
        for (String line : report) telemetry.addLine(line);
    }

    // =========================================================
    // BUSQUEDA DE HARDWARE POR NOMBRES ALTERNATIVOS
    // =========================================================

    private <T> T required(HardwareMap hw, Class<T> type, String label, String[] names) {
        T device = find(hw, type, names);
        if (device == null) {
            throw new IllegalStateException("Falta el motor '" + label
                    + "' en la configuracion del Driver Station. Nombres aceptados: "
                    + join(", ", names));
        }
        return device;
    }

    private <T> T optional(HardwareMap hw, Class<T> type, String label, String[] names) {
        T device = find(hw, type, names);
        report.add(label + ": " + (device == null
                ? "NO encontrado (" + join("/", names) + ")"
                : "OK"));
        return device;
    }

    private static <T> T find(HardwareMap hw, Class<T> type, String[] names) {
        for (String name : names) {
            T device = hw.tryGet(type, name);
            if (device != null) return device;
        }
        return null;
    }

    /** String.join no existe en Android 7 (Control Hub), por eso se hace a mano. */
    private static String join(String sep, String[] parts) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < parts.length; i++) {
            if (i > 0) sb.append(sep);
            sb.append(parts[i]);
        }
        return sb.toString();
    }

    /** Zona muerta combinada con curva de respuesta exponencial para control fino */
    private static double deadband(double v) {
        if (Math.abs(v) < RobotConfig.STICK_DEADBAND) return 0.0;
        double abs = Math.abs(v);
        double out = 0.35 * abs + 0.65 * (abs * abs * abs);
        return Math.copySign(out, v);
    }
}
