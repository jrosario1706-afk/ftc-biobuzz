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
public class BioBuzzTeleOpPro extends LinearOpMode {

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
    public static double AUTO_AIM_MAX_TURN = 0.45;
    public static double AUTO_AIM_TOLERANCE = 1.8;

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

    // =========================================================
    // RUN OP MODE
    // =========================================================

    @Override
    public void runOpMode() {

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

        limelight =
                hardwareMap.get(
                        Limelight3A.class,
                        "limelight"
                );

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

    private void driveRobot(
            boolean autoAiming,
            double targetTx
    ) {

        double y =
                -gamepad1.left_stick_y;

        double x =
                gamepad1.left_stick_x * 1.10;

        double rx =
                gamepad1.right_stick_x;

        // Deadband

        if (Math.abs(y) < 0.05) {
            y = 0;
        }

        if (Math.abs(x) < 0.05) {
            x = 0;
        }

        if (Math.abs(rx) < 0.05) {
            rx = 0;
        }

        // Slow mode

        double speed =
                gamepad1.left_trigger > 0.1
                        ? SLOW_DRIVE_SPEED
                        : DRIVE_SPEED;

        // Auto Aim

        if (autoAiming) {

            rx =
                    targetTx
                            * AUTO_AIM_KP;

            rx =
                    Range.clip(
                            rx,
                            -AUTO_AIM_MAX_TURN,
                            AUTO_AIM_MAX_TURN
                    );

            if (
                    Math.abs(targetTx)
                            < AUTO_AIM_TOLERANCE
            ) {

                rx = 0;
            }
        }

        // Mecanum

        double fl =
                y + x + rx;

        double bl =
                y - x + rx;

        double fr =
                y - x - rx;

        double br =
                y + x - rx;

        double max =
                Math.max(
                        Math.abs(fl),
                        Math.abs(bl)
                );

        max =
                Math.max(
                        max,
                        Math.abs(fr)
                );

        max =
                Math.max(
                        max,
                        Math.abs(br)
                );

        if (max > 1.0) {

            fl /= max;
            bl /= max;
            fr /= max;
            br /= max;
        }

        frontLeft.setPower(
                fl * speed
        );

        backLeft.setPower(
                bl * speed
        );

        frontRight.setPower(
                fr * speed
        );

        backRight.setPower(
                br * speed
        );
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

        if (manualTrigger > 0.1) {

            shooter.setVelocity(
                    manualTrigger
                            * MAX_SHOOTER_VELOCITY
            );

        } else if (
                shootButton
                        || aiShoot
        ) {

            shooter.setVelocity(
                    calculatedVelocity
            );

        } else {

            shooter.setVelocity(0);
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

        telemetry.addData(
                "Tiempo restante",
                "%.1f s",
                timeRemaining
        );

        telemetry.addData(
                "Modo drive",
                gamepad1.left_trigger > 0.1
                        ? "LENTO"
                        : "NORMAL"
        );

        telemetry.addData(
                "Pipeline",
                useNeuralPipeline
                        ? "IA Detector"
                        : "Color"
        );

        telemetry.addData(
                "Filtro",
                targetFilter
        );

        telemetry.addData(
                "Objetivo",
                vision.targetClass
        );

        telemetry.addData(
                "Confianza",
                "%.1f%%",
                vision.confidence * 100
        );

        telemetry.addData(
                "Target tx",
                "%.2f°",
                vision.tx
        );

        telemetry.addData(
                "Target ty",
                "%.2f°",
                vision.ty
        );

        telemetry.addData(
                "Auto Aim",
                autoAiming
                        ? "ACTIVO"
                        : "MANUAL"
        );

        telemetry.addData(
                "Distancia",
                "%.1f cm",
                vision.distanceCm
        );

        telemetry.addData(
                "Shooter objetivo",
                "%.0f",
                shooterVelocity
        );

        telemetry.addData(
                "Shooter actual",
                "%.0f",
                shooter.getVelocity()
        );

        telemetry.addData(
                "Polen",
                vision.pollenCount
        );

        telemetry.addData(
                "Nectar",
                vision.nectarCount
        );

        telemetry.addData(
                "AprilTags",
                vision.aprilTagCount
        );

        telemetry.addData(
                "Pose",
                vision.hasPose
                        ? String.format(
                        "X %.2f | Y %.2f | Yaw %.1f",
                        vision.robotX,
                        vision.robotY,
                        vision.robotYaw
                )
                        : "No disponible"
        );

        telemetry.update();
    }

    // =========================================================
    // SAFE STOP
    // =========================================================

    private void safeStop() {

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
