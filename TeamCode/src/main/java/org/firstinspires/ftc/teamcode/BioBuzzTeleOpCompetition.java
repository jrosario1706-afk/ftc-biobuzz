package org.firstinspires.ftc.teamcode;
import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;

import java.util.List;

@TeleOp(
        name = "BioBuzz TeleOp Competition",
        group = "BioBuzz"
)
public class BioBuzzTeleOpCompetition extends LinearOpMode {

    // =========================================================
    // CONFIGURACIÓN
    // =========================================================

    public static double DRIVE_SPEED = 0.90;
    public static double SLOW_DRIVE_SPEED = 0.35;

    public static double INTAKE_POWER = 0.90;

    public static double MAX_SHOOTER_VELOCITY = 2000.0;

    public static double MIN_CONFIDENCE = 0.40;

    // Auto Aim
    public static double AUTO_AIM_KP = 0.035;
    public static double AUTO_AIM_KS = 0.075;
    public static double AUTO_AIM_KD = 0.0035;
    public static double AUTO_AIM_MAX_TURN = 0.45;
    public static double AUTO_AIM_TOLERANCE = 1.8;

    // Cinematica Mecanum y Suavizado
    public static double DRIVE_EXPO_FACTOR = 0.65;
    public static double SLEW_RATE_LIMIT = 4.0;
    public static double SLEW_RATE_ACCEL = 4.0;
    public static double SLEW_RATE_DECEL = 10.0;
    public static double STRAFE_CORRECTION = 1.12;
    public static double SHOOTER_RAMP_TICKS_S2 = 4200.0;

    // Distancia -> Shooter
    public static double LIMELIGHT_HEIGHT_CM = 25.0;
    public static double TARGET_HEIGHT_CM = 45.0;
    public static double LIMELIGHT_MOUNT_ANGLE_DEG = 15.0;

    public static double BASE_SHOOTER_VELOCITY = 1200.0;
    public static double DISTANCE_VELOCITY_GAIN = 12.0;

    // Limelight
    public static int PIPELINE_NEURAL_IA = 1;
    public static int PIPELINE_COLOR = 0;

    // =========================================================
    // HARDWARE
    // =========================================================

    private DcMotor frontLeft;
    private DcMotor frontRight;
    private DcMotor backLeft;
    private DcMotor backRight;

    private DcMotor intake;

    private DcMotorEx shooter;

    private Limelight3A limelight;

    // =========================================================
    // ESTADO
    // =========================================================

    private String targetFilter = "TODOS";

    private boolean useNeuralPipeline = true;

    private boolean yPrevious = false;
    private boolean dpadRightPrevious = false;

    private boolean targetLockedPrevious = false;

    private boolean warned30Seconds = false;
    private boolean warned10Seconds = false;

    private final ElapsedTime matchTimer =
            new ElapsedTime();

    // Filtros de dinamica de chasis y auto-aim
    private double lastFl = 0.0, lastBl = 0.0, lastFr = 0.0, lastBr = 0.0;
    private final ElapsedTime chassisTimer = new ElapsedTime();
    private final ElapsedTime aimTimer = new ElapsedTime();
    private double lastAimError = 0.0;
    private double lastAimDerivative = 0.0;
    private boolean hasPreviousAimError = false;

    // Diagnostico de loop y sensor de bateria
    private com.qualcomm.robotcore.hardware.VoltageSensor voltageSensor;
    private final ElapsedTime loopTimer = new ElapsedTime();
    private double loopFrequencyHz = 0.0;

    // Rampa suave de velocidad del shooter
    private double currentShooterCmd = 0.0;
    private final ElapsedTime shooterTimer = new ElapsedTime();

    // =========================================================
    // RUN OP MODE
    // =========================================================

    @Override
    public void runOpMode() {
        telemetry = new com.acmerobotics.dashboard.telemetry.MultipleTelemetry(
                telemetry,
                com.acmerobotics.dashboard.FtcDashboard.getInstance().getTelemetry()
        );

        initializeHardware();

        initializeLimelight();

        telemetry.addLine("==============================");
        telemetry.addLine(" BIOBUZZ TELEOP COMPETITION");
        telemetry.addLine(" FTC TEAM 24096");
        telemetry.addLine("==============================");
        telemetry.addLine("Limelight: Inicializando...");
        telemetry.update();

        waitForStart();

        if (isStopRequested()) {
            safeStop();
            return;
        }

        matchTimer.reset();

        while (opModeIsActive()) {

            // =================================================
            // TIEMPO
            // =================================================

            double matchTime =
                    matchTimer.seconds();

            double timeRemaining =
                    Math.max(
                            0.0,
                            120.0 - matchTime
                    );

            handleMatchWarnings(
                    timeRemaining
            );

            // =================================================
            // FILTROS DE VISIÓN
            // =================================================

            updateTargetFilter();

            updateAlliancePipeline();

            updateNeuralPipeline();

            // =================================================
            // LIMELIGHT
            // =================================================

            VisionData vision =
                    readVision();

            // =================================================
            // AUTO AIM
            // =================================================

            boolean autoAiming =
                    gamepad1.x
                            && vision.hasTarget;

            // =================================================
            // DRIVE
            // =================================================

            driveRobot(
                    autoAiming,
                    vision.tx
            );

            // =================================================
            // INTAKE
            // =================================================

            controlIntake();

            // =================================================
            // SHOOTER
            // =================================================

            double shooterVelocity =
                    calculateShooterVelocity(
                            vision
                    );

            controlShooter(
                    shooterVelocity,
                    autoAiming,
                    vision.hasTarget
            );

            // =================================================
            // HAPTICS
            // =================================================

            updateTargetHaptics(
                    vision.hasTarget,
                    vision.tx
            );

            // =================================================
            // TELEMETRY
            // =================================================

            updateTelemetry(
                    timeRemaining,
                    vision,
                    shooterVelocity,
                    autoAiming
            );

            idle();
        }

        safeStop();
    }

    // =========================================================
    // HARDWARE
    // =========================================================

    private void initializeHardware() {

        frontLeft =
                hardwareMap.get(
                        DcMotor.class,
                        "frontLeft"
                );

        frontRight =
                hardwareMap.get(
                        DcMotor.class,
                        "frontRight"
                );

        backLeft =
                hardwareMap.get(
                        DcMotor.class,
                        "backLeft"
                );

        backRight =
                hardwareMap.get(
                        DcMotor.class,
                        "backRight"
                );

        intake =
                hardwareMap.get(
                        DcMotor.class,
                        "intake"
                );

        shooter =
                hardwareMap.get(
                        DcMotorEx.class,
                        "shooter"
                );

        try { limelight = hardwareMap.get(Limelight3A.class, "limelight"); } catch (Exception e) { limelight = null; }

        // -----------------------------------------------------
        // DIRECCIONES
        // -----------------------------------------------------

        frontLeft.setDirection(
                DcMotorSimple.Direction.REVERSE
        );

        backLeft.setDirection(
                DcMotorSimple.Direction.REVERSE
        );

        frontRight.setDirection(
                DcMotorSimple.Direction.FORWARD
        );

        backRight.setDirection(
                DcMotorSimple.Direction.FORWARD
        );

        intake.setDirection(
                DcMotorSimple.Direction.FORWARD
        );

        shooter.setDirection(
                DcMotorSimple.Direction.FORWARD
        );

        // -----------------------------------------------------
        // BRAKE
        // -----------------------------------------------------

        frontLeft.setZeroPowerBehavior(
                DcMotor.ZeroPowerBehavior.BRAKE
        );

        frontRight.setZeroPowerBehavior(
                DcMotor.ZeroPowerBehavior.BRAKE
        );

        backLeft.setZeroPowerBehavior(
                DcMotor.ZeroPowerBehavior.BRAKE
        );

        backRight.setZeroPowerBehavior(
                DcMotor.ZeroPowerBehavior.BRAKE
        );

        intake.setZeroPowerBehavior(
                DcMotor.ZeroPowerBehavior.BRAKE
        );

        shooter.setZeroPowerBehavior(
                DcMotor.ZeroPowerBehavior.FLOAT
        );

        // -----------------------------------------------------
        // MODOS
        // -----------------------------------------------------

        frontLeft.setMode(
                DcMotor.RunMode.RUN_WITHOUT_ENCODER
        );

        frontRight.setMode(
                DcMotor.RunMode.RUN_WITHOUT_ENCODER
        );

        backLeft.setMode(
                DcMotor.RunMode.RUN_WITHOUT_ENCODER
        );

        backRight.setMode(
                DcMotor.RunMode.RUN_WITHOUT_ENCODER
        );

        intake.setMode(
                DcMotor.RunMode.RUN_WITHOUT_ENCODER
        );

        shooter.setMode(
                DcMotor.RunMode.STOP_AND_RESET_ENCODER
        );

        shooter.setMode(
                DcMotor.RunMode.RUN_USING_ENCODER
        );

        try {
            com.qualcomm.robotcore.hardware.PIDFCoefficients pidf =
                    new com.qualcomm.robotcore.hardware.PIDFCoefficients(
                            BioBuzzConfig.SHOOTER_PIDF_P,
                            BioBuzzConfig.SHOOTER_PIDF_I,
                            BioBuzzConfig.SHOOTER_PIDF_D,
                            BioBuzzConfig.SHOOTER_PIDF_F
                    );
            shooter.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, pidf);
        } catch (Exception ignored) {
        }

        for (com.qualcomm.robotcore.hardware.VoltageSensor vs : hardwareMap.voltageSensor) {
            voltageSensor = vs;
            break;
        }
    }

    // =========================================================
    // LIMELIGHT
    // =========================================================

    private void initializeLimelight() {

        try {

            limelight.pipelineSwitch(
                    PIPELINE_NEURAL_IA
            );

            limelight.start();

        } catch (Exception e) {

            telemetry.addData(
                    "Limelight ERROR",
                    e.getMessage()
            );

            telemetry.update();
        }
    }

    // =========================================================
    // FILTRO POLEN / NECTAR
    // =========================================================

    private void updateTargetFilter() {

        if (gamepad1.dpad_up
                || gamepad2.dpad_up) {

            targetFilter = "POLEN";
        }

        if (gamepad1.dpad_down
                || gamepad2.dpad_down) {

            targetFilter = "NECTAR";
        }

        if (gamepad1.dpad_left
                || gamepad2.dpad_left) {

            targetFilter = "TODOS";
        }
    }

    private void updateAlliancePipeline() {
        // Opcional / Alianza
    }

    // =========================================================
    // CAMBIO DE PIPELINE
    // =========================================================

    private void updateNeuralPipeline() {

        boolean yCurrent =
                gamepad1.y
                        || gamepad2.y;

        if (yCurrent && !yPrevious) {

            useNeuralPipeline =
                    !useNeuralPipeline;

            try {

                limelight.pipelineSwitch(
                        useNeuralPipeline
                                ? PIPELINE_NEURAL_IA
                                : PIPELINE_COLOR
                );

            } catch (Exception ignored) {
            }
        }

        yPrevious = yCurrent;
    }

    // =========================================================
    // VISION DATA
    // =========================================================

    private static class VisionData {

        boolean hasTarget = false;

        String targetClass = "Ninguno";

        double tx = 0.0;
        double ty = 0.0;
        double confidence = 0.0;

        double distanceCm = 0.0;

        boolean hasPose = false;

        double robotX = 0.0;
        double robotY = 0.0;
        double robotYaw = 0.0;

        int aprilTagCount = 0;

        int pollenCount = 0;
        int nectarCount = 0;
    }

    // =========================================================
    // LEER LIMELIGHT
    // =========================================================

    private VisionData readVision() {

        VisionData data =
                new VisionData();

        try {

            LLResult result =
                    limelight.getLatestResult();

            if (result == null
                    || !result.isValid()) {

                return data;
            }

            List<LLResultTypes.DetectorResult>
                    detections =
                    result.getDetectorResults();

            double closestDistance =
                    Double.MAX_VALUE;

            for (
                    LLResultTypes.DetectorResult detection
                    : detections
            ) {

                double confidence =
                        detection.getConfidence();

                if (confidence
                        < MIN_CONFIDENCE) {

                    continue;
                }

                String className =
                        detection
                                .getClassName()
                                .toLowerCase();

                double tx =
                        detection
                                .getTargetXDegrees();

                double ty =
                        detection
                                .getTargetYDegrees();

                boolean pollen =
                        className.equals("pollen")
                                || className.equals("polen");

                boolean nectar =
                        className.equals("nectar");

                if (pollen) {
                    data.pollenCount++;
                }

                if (nectar) {
                    data.nectarCount++;
                }

                boolean accepted;

                if (targetFilter.equals("POLEN")) {

                    accepted = pollen;

                } else if (
                        targetFilter.equals("NECTAR")
                ) {

                    accepted = nectar;

                } else {

                    accepted =
                            pollen || nectar;
                }

                if (!accepted) {
                    continue;
                }

                double distance =
                        Math.hypot(
                                tx,
                                ty
                        );

                if (distance
                        < closestDistance) {

                    closestDistance =
                            distance;

                    data.hasTarget = true;

                    data.targetClass =
                            pollen
                                    ? "POLEN"
                                    : "NECTAR";

                    data.tx = tx;
                    data.ty = ty;
                    data.confidence =
                            confidence;
                }
            }

            if (!data.hasTarget && !useNeuralPipeline && result.getTa() > 0.05) {
                data.hasTarget = true;
                data.targetClass = "COLOR";
                data.tx = result.getTx();
                data.ty = result.getTy();
                data.confidence = 0.90;
            }

            // -------------------------------------------------
            // APRILTAGS / POSE
            // -------------------------------------------------

            Pose3D pose =
                    result.getBotpose();

            if (pose != null) {

                data.hasPose = true;

                data.robotX =
                        pose.getPosition().x;

                data.robotY =
                        pose.getPosition().y;

                data.robotYaw =
                        pose.getOrientation()
                                .getYaw();
            }

            List<LLResultTypes.FiducialResult>
                    fiducials =
                    result.getFiducialResults();

            if (fiducials != null) {

                data.aprilTagCount =
                        fiducials.size();
            }

        } catch (Exception ignored) {
            // Mantener el TeleOp funcionando
            // aunque la visión falle.
        }

        // -----------------------------------------------------
        // DISTANCIA
        // -----------------------------------------------------

        if (data.hasTarget) {

            double angle =
                    Math.toRadians(
                            LIMELIGHT_MOUNT_ANGLE_DEG
                                    + data.ty
                    );

            double tangent =
                    Math.tan(angle);

            if (tangent > 0.05) {

                data.distanceCm =
                        (
                                TARGET_HEIGHT_CM
                                        - LIMELIGHT_HEIGHT_CM
                        )
                                / tangent;

                if (data.distanceCm < 0) {
                    data.distanceCm = 0;
                }
            }
        }

        return data;
    }

    // =========================================================
    // DRIVE
    // =========================================================

    private double stickCurve(double v) {
        if (Math.abs(v) < 0.05) return 0.0;
        double expo = DRIVE_EXPO_FACTOR;
        double abs = Math.abs(v);
        double out = (1.0 - expo) * abs + expo * (abs * abs * abs);
        return Math.copySign(out, v);
    }

    private void driveRobot(
            boolean autoAiming,
            double targetTx
    ) {

        double y = stickCurve(-gamepad1.left_stick_y);
        double x = stickCurve(gamepad1.left_stick_x) * STRAFE_CORRECTION;
        double rx = stickCurve(gamepad1.right_stick_x);

        double speed = gamepad1.left_trigger > 0.1 ? SLOW_DRIVE_SPEED : DRIVE_SPEED;

        // Auto Aim con lazo cerrado PD + feedforward kS y filtro de derivada
        if (autoAiming) {
            if (Math.abs(targetTx) <= AUTO_AIM_TOLERANCE) {
                rx = 0.0;
                hasPreviousAimError = false;
            } else {
                double dt = Math.max(0.005, Math.min(0.1, aimTimer.seconds()));
                aimTimer.reset();

                double derivada = 0.0;
                if (hasPreviousAimError && dt < 0.1) {
                    double rawDerivada = (targetTx - lastAimError) / dt;
                    derivada = 0.70 * rawDerivada + 0.30 * lastAimDerivative;
                    lastAimDerivative = derivada;
                } else {
                    hasPreviousAimError = true;
                    lastAimDerivative = 0.0;
                }
                lastAimError = targetTx;

                // Suavizado continuo de friccion estatica (kS) para eliminar oscilacion tipo chatter
                double factorFriccion = Math.min(1.0, Math.max(0.0, (Math.abs(targetTx) - AUTO_AIM_TOLERANCE) / 1.2));
                double kS = Math.signum(targetTx) * AUTO_AIM_KS * factorFriccion;
                double kP = targetTx * AUTO_AIM_KP;
                double kD = Range.clip(derivada * AUTO_AIM_KD, -0.15, 0.15);

                rx = Range.clip(kS + kP + kD, -AUTO_AIM_MAX_TURN, AUTO_AIM_MAX_TURN);
            }
        } else {
            hasPreviousAimError = false;
        }

        // Cinemática Mecanum
        double fl = y + x + rx;
        double bl = y - x + rx;
        double fr = y - x - rx;
        double br = y + x - rx;

        double max = Math.max(Math.max(Math.abs(fl), Math.abs(bl)),
                Math.max(Math.abs(fr), Math.abs(br)));
        if (max > 1.0) {
            fl /= max;
            bl /= max;
            fr /= max;
            br /= max;
        }

        // Filtro de rampa asimetrico (aceleracion controlada, frenado inmediato)
        double dtChassis = Math.max(0.001, Math.min(0.1, chassisTimer.seconds()));
        chassisTimer.reset();

        fl = calculateRateStep(fl * speed, lastFl, dtChassis);
        bl = calculateRateStep(bl * speed, lastBl, dtChassis);
        fr = calculateRateStep(fr * speed, lastFr, dtChassis);
        br = calculateRateStep(br * speed, lastBr, dtChassis);

        lastFl = fl; lastBl = bl; lastFr = fr; lastBr = br;

        frontLeft.setPower(fl);
        backLeft.setPower(bl);
        frontRight.setPower(fr);
        backRight.setPower(br);
    }

    private double calculateRateStep(double target, double current, double dt) {
        boolean braking = Math.abs(target) < Math.abs(current) || (target * current < 0.0);
        double maxRate = braking ? SLEW_RATE_DECEL : SLEW_RATE_ACCEL;
        double maxStep = maxRate * dt;
        return Range.clip(target, current - maxStep, current + maxStep);
    }

    // =========================================================
    // INTAKE
    // =========================================================

    private void controlIntake() {

        boolean intakeIn =
                gamepad2.right_bumper
                        || gamepad1.right_bumper;

        boolean intakeOut =
                gamepad2.left_bumper
                        || gamepad1.left_bumper;

        if (intakeIn) {

            intake.setPower(
                    INTAKE_POWER
            );

        } else if (intakeOut) {

            intake.setPower(
                    -INTAKE_POWER
            );

        } else {

            intake.setPower(0);
        }
    }

    // =========================================================
    // SHOOTER
    // =========================================================

    private void controlShooter(
            double calculatedVelocity,
            boolean autoAiming,
            boolean hasTarget
    ) {

        double manualTrigger =
                Math.max(
                        gamepad1.right_trigger,
                        gamepad2.right_trigger
                );

        boolean shootButton =
                gamepad1.b
                        || gamepad2.b;

        boolean aiShoot =
                autoAiming
                        && hasTarget
                        && Math.abs(
                        calculatedVelocity
                ) > 0;

        double targetVelocity = 0.0;
        if (manualTrigger > 0.1) {
            targetVelocity = manualTrigger * MAX_SHOOTER_VELOCITY;
        } else if (shootButton || aiShoot) {
            targetVelocity = calculatedVelocity;
        }

        if (targetVelocity <= 0.0) {
            currentShooterCmd = 0.0;
            shooter.setVelocity(0);
            shooter.setPower(0);
        } else {
            double dt = Math.min(0.2, Math.max(0.001, shooterTimer.seconds()));
            shooterTimer.reset();
            double delta = SHOOTER_RAMP_TICKS_S2 * dt;
            if (currentShooterCmd < targetVelocity) {
                currentShooterCmd = Math.min(targetVelocity, currentShooterCmd + delta);
            } else {
                currentShooterCmd = Math.max(targetVelocity, currentShooterCmd - delta);
            }
            shooter.setVelocity(currentShooterCmd);
        }
    }

    // =========================================================
    // SHOOTER VELOCITY
    // =========================================================

    private double calculateShooterVelocity(
            VisionData vision
    ) {

        if (!vision.hasTarget) {

            return BASE_SHOOTER_VELOCITY;
        }

        double velocity =
                BASE_SHOOTER_VELOCITY
                        + (
                        vision.distanceCm
                                * DISTANCE_VELOCITY_GAIN
                );

        return Range.clip(
                velocity,
                500,
                MAX_SHOOTER_VELOCITY
        );
    }

    // =========================================================
    // HAPTICS
    // =========================================================

    private void updateTargetHaptics(
            boolean hasTarget,
            double tx
    ) {

        boolean centered =
                hasTarget
                        && Math.abs(tx)
                        < AUTO_AIM_TOLERANCE;

        if (
                centered
                        && !targetLockedPrevious
        ) {

            gamepad1.rumble(
                    0.5,
                    0.5,
                    150
            );

            gamepad2.rumble(
                    0.5,
                    0.5,
                    150
            );
        }

        targetLockedPrevious =
                centered;
    }

    // =========================================================
    // MATCH WARNINGS
    // =========================================================

    private void handleMatchWarnings(
            double timeRemaining
    ) {

        if (
                timeRemaining <= 30
                        && !warned30Seconds
        ) {

            gamepad1.rumbleBlips(2);
            gamepad2.rumbleBlips(2);

            warned30Seconds = true;
        }

        if (
                timeRemaining <= 10
                        && !warned10Seconds
        ) {

            gamepad1.rumbleBlips(3);
            gamepad2.rumbleBlips(3);

            warned10Seconds = true;
        }
    }

    // =========================================================
    // TELEMETRY
    // =========================================================

    private void updateTelemetry(
            double timeRemaining,
            VisionData vision,
            double shooterVelocity,
            boolean autoAiming
    ) {
        double dtLoop = loopTimer.seconds();
        loopTimer.reset();
        if (dtLoop > 0.0) {
            double hzCurrent = 1.0 / dtLoop;
            loopFrequencyHz = loopFrequencyHz == 0.0 ? hzCurrent : 0.90 * loopFrequencyHz + 0.10 * hzCurrent;
        }
        double batteryVoltage = voltageSensor != null ? voltageSensor.getVoltage() : 0.0;

        telemetry.addData("[MATCH]", "%.1fs | Bateria: %.2fV | Loop: %.0fHz", timeRemaining, batteryVoltage, loopFrequencyHz);
        telemetry.addData("[DRIVE]", "%s | Expo: %.2f | Slew: %.1f",
                gamepad1.left_trigger > 0.1 ? "LENTO (35%)" : "NORMAL (90%)", DRIVE_EXPO_FACTOR, SLEW_RATE_LIMIT);
        telemetry.addData("[VISION 3A]", "%s (%s) | Conf: %.1f%%",
                vision.targetClass, useNeuralPipeline ? "NEURAL" : "COLOR", vision.confidence * 100);
        telemetry.addData("[AUTO-AIM]", "%s | tx: %.2f° | ty: %.2f° | d: %.1fcm",
                autoAiming ? (Math.abs(vision.tx) <= AUTO_AIM_TOLERANCE ? "[LOCKED]" : "ALINEANDO") : "OFF",
                vision.tx, vision.ty, vision.distanceCm);
        telemetry.addData("[SHOOTER]", "Obj: %.0f | Real: %.0f %s",
                shooterVelocity, shooter.getVelocity(),
                Math.abs(shooter.getVelocity() - shooterVelocity) < 85 && shooterVelocity > 100 ? "[LISTO]" : "");
        telemetry.addData("[PIEZAS]", "Polen: %d | Nectar: %d | Tags: %d",
                vision.pollenCount, vision.nectarCount, vision.aprilTagCount);

        telemetry.update();
    }

    // =========================================================
    // SAFE STOP
    // =========================================================

    private void safeStop() {
        lastFl = 0.0;
        lastBl = 0.0;
        lastFr = 0.0;
        lastBr = 0.0;

        try {
            frontLeft.setPower(0);
            frontRight.setPower(0);
            backLeft.setPower(0);
            backRight.setPower(0);
        } catch (Exception ignored) {
        }

        try {
            intake.setPower(0);
        } catch (Exception ignored) {
        }

        try {
            shooter.setVelocity(0);
        } catch (Exception ignored) {
        }

        try {
            limelight.stop();
        } catch (Exception ignored) {
        }
    }
}

