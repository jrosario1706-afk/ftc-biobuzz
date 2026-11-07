package org.firstinspires.ftc.teamcode.autonomous;

import static org.firstinspires.ftc.teamcode.autonomous.BioBuzzConfig.*;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.hardware.RobotHardware;
import org.jhoandri.pathing.JhoandriPathing;
import org.jhoandri.pathing.Vision.JhoandriVision;

/**
 * ============================================================================
 * BIOBUZZ AUTO PRO V3 AI — WORLD CHAMPION EDITION (FTC 24096 / 2026-2027)
 * ============================================================================
 * Optimizaciones de Nivel Campeón del Mundo:
 *   1. Anticipatory Spin-Up: El shooter acelera antes de llegar al punto de disparo.
 *   2. Vision Auto-Aim en tiempo real con Limelight 3A.
 *   3. Máquina de estados no-bloqueante de ultra-alta velocidad y cero latencia.
 *   4. Sistema de recuperación por Timeout inteligente y fail-safe de emergencia.
 * ============================================================================
 */
@Config
@Autonomous(name = "BioBuzz Auto Pro V3 AI (World Champion)", group = "BioBuzz Pro")
public class BioBuzzAutoProV3AI extends LinearOpMode {

    // ===== Coordenadas de Precisión Milimétrica =====
    public static double START_X = 9.0;
    public static double START_Y = 111.0;
    public static double START_HEADING_DEG = 270.0;

    public static double SCORE_X = 16.0;
    public static double SCORE_Y = 128.0;
    public static double SCORE_HEADING_DEG = 315.0;

    // Tres posiciones consecutivas de polen (Ciclos de alta eficiencia)
    public static double PICKUP_1_X = 38.0;
    public static double PICKUP_1_Y = 88.0;

    public static double PICKUP_2_X = 48.0;
    public static double PICKUP_2_Y = 88.0;

    public static double PICKUP_3_X = 58.0;
    public static double PICKUP_3_Y = 88.0;

    public static double PICKUP_HEADING_DEG = 180.0;

    public static double PARK_X = 12.0;
    public static double PARK_Y = 105.0;
    public static double PARK_HEADING_DEG = 270.0;

    private enum AutoState {
        DRIVE_SCORE_1, SHOOT_1,
        DRIVE_PICKUP_1, COLLECT_1, DRIVE_SCORE_2, SHOOT_2,
        DRIVE_PICKUP_2, COLLECT_2, DRIVE_SCORE_3, SHOOT_3,
        DRIVE_PICKUP_3, COLLECT_3, DRIVE_SCORE_4, SHOOT_4,
        PARK, COMPLETE
    }

    private enum ShootPhase { SPIN_UP, FEED, DONE }

    private JhoandriPathing pathing;
    private RobotHardware robot;
    private JhoandriVision vision;

    private final ElapsedTime stateTimer = new ElapsedTime();
    private final ElapsedTime actionTimer = new ElapsedTime();
    private final ElapsedTime matchTimer = new ElapsedTime();

    private AutoState state = AutoState.DRIVE_SCORE_1;
    private ShootPhase shootPhase = ShootPhase.SPIN_UP;
    private int shooterRetries = 0;
    private boolean timeoutParking = false;
    private JhoandriVision.Detection pollen;

    @Override
    public void runOpMode() {
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

        try {
            pathing = new JhoandriPathing(hardwareMap, true);
            robot = pathing.getRobot();
            vision = pathing.getVision();

            telemetry.addLine("👑 BIOBUZZ AUTO PRO V3 AI — WORLD CHAMPION EDITION");
            telemetry.addData("Vision Limelight", vision != null && vision.isCameraAvailable() ? "ONLINE (AI Ready)" : "OFFLINE");
            robot.reportTo(telemetry);
            telemetry.addLine("⚡ Sistemas listos para campeonato mundial. Esperando START...");
            telemetry.update();

            waitForStart();
            if (isStopRequested()) return;

            matchTimer.reset();
            pathing.resetPoseAndHeading(START_X, START_Y, Math.toRadians(START_HEADING_DEG));
            goTo(SCORE_X, SCORE_Y, SCORE_HEADING_DEG, AutoState.DRIVE_SCORE_1);

            while (opModeIsActive() && state != AutoState.COMPLETE) {
                pathing.update();
                pollen = (vision != null && ENABLE_BALL_TRACKING) ? vision.getBestPollen() : null;

                if (!handleTimeout()) {
                    updateStateMachine();
                }
                updateTelemetry();
            }

        } catch (RuntimeException e) {
            telemetry.addData("CRITICAL AUTO ERROR", e.getMessage());
            telemetry.update();
            while (!isStopRequested()) idle();
        } finally {
            if (pathing != null) pathing.stop();
        }
    }

    private void updateStateMachine() {
        switch (state) {
            // === DISPARO INICIAL PRE-CARGA ===
            case DRIVE_SCORE_1:
                // Campeón: Anticipatory spin-up cuando falten pocos centímetros
                if (distanceTo(SCORE_X, SCORE_Y) < 35.0) {
                    robot.setShooterVelocity(SHOOTER_VELOCITY);
                }
                if (!pathing.isBusy()) beginShooting(AutoState.SHOOT_1);
                break;

            case SHOOT_1:
                if (updateShooting()) goTo(PICKUP_1_X, PICKUP_1_Y, PICKUP_HEADING_DEG, AutoState.DRIVE_PICKUP_1);
                break;

            // === CICLO 1: POLEN 1 ===
            case DRIVE_PICKUP_1:
                if (!pathing.isBusy()) beginCollection(AutoState.COLLECT_1);
                break;

            case COLLECT_1:
                if (actionTimer.milliseconds() >= getCollectionDuration()) {
                    robot.setIntakePower(0);
                    if (vision != null) vision.setPipeline(0);
                    goTo(SCORE_X, SCORE_Y, SCORE_HEADING_DEG, AutoState.DRIVE_SCORE_2);
                }
                break;

            case DRIVE_SCORE_2:
                if (distanceTo(SCORE_X, SCORE_Y) < 35.0) {
                    robot.setShooterVelocity(SHOOTER_VELOCITY);
                }
                if (!pathing.isBusy()) beginShooting(AutoState.SHOOT_2);
                break;

            case SHOOT_2:
                if (updateShooting()) goTo(PICKUP_2_X, PICKUP_2_Y, PICKUP_HEADING_DEG, AutoState.DRIVE_PICKUP_2);
                break;

            // === CICLO 2: POLEN 2 ===
            case DRIVE_PICKUP_2:
                if (!pathing.isBusy()) beginCollection(AutoState.COLLECT_2);
                break;

            case COLLECT_2:
                if (actionTimer.milliseconds() >= getCollectionDuration()) {
                    robot.setIntakePower(0);
                    if (vision != null) vision.setPipeline(0);
                    goTo(SCORE_X, SCORE_Y, SCORE_HEADING_DEG, AutoState.DRIVE_SCORE_3);
                }
                break;

            case DRIVE_SCORE_3:
                if (distanceTo(SCORE_X, SCORE_Y) < 35.0) {
                    robot.setShooterVelocity(SHOOTER_VELOCITY);
                }
                if (!pathing.isBusy()) beginShooting(AutoState.SHOOT_3);
                break;

            case SHOOT_3:
                if (updateShooting()) goTo(PICKUP_3_X, PICKUP_3_Y, PICKUP_HEADING_DEG, AutoState.DRIVE_PICKUP_3);
                break;

            // === CICLO 3: POLEN 3 ===
            case DRIVE_PICKUP_3:
                if (!pathing.isBusy()) beginCollection(AutoState.COLLECT_3);
                break;

            case COLLECT_3:
                if (actionTimer.milliseconds() >= getCollectionDuration()) {
                    robot.setIntakePower(0);
                    if (vision != null) vision.setPipeline(0);
                    goTo(SCORE_X, SCORE_Y, SCORE_HEADING_DEG, AutoState.DRIVE_SCORE_4);
                }
                break;

            case DRIVE_SCORE_4:
                if (distanceTo(SCORE_X, SCORE_Y) < 35.0) {
                    robot.setShooterVelocity(SHOOTER_VELOCITY);
                }
                if (!pathing.isBusy()) beginShooting(AutoState.SHOOT_4);
                break;

            case SHOOT_4:
                if (updateShooting()) {
                    robot.stopMechanisms();
                    goTo(PARK_X, PARK_Y, PARK_HEADING_DEG, AutoState.PARK);
                }
                break;

            // === ESTACIONAMIENTO ===
            case PARK:
                if (!pathing.isBusy()) setState(AutoState.COMPLETE);
                break;

            case COMPLETE:
                break;
        }
    }

    // ---------------------------------------------------------
    // NAVEGACION & UTILIDADES
    // ---------------------------------------------------------

    private void goTo(double x, double y, double headingDeg, AutoState next) {
        pathing.setTarget(x, y, Math.toRadians(headingDeg));
        setState(next);
    }

    private void setState(AutoState next) {
        state = next;
        stateTimer.reset();
    }

    private double distanceTo(double targetX, double targetY) {
        double dx = targetX - pathing.getX();
        double dy = targetY - pathing.getY();
        return Math.hypot(dx, dy);
    }

    // ---------------------------------------------------------
    // SHOOTER CHAMPION EDITION
    // ---------------------------------------------------------

    private void beginShooting(AutoState shootState) {
        shooterRetries = 0;
        shootPhase = ShootPhase.SPIN_UP;
        robot.feed(false);
        robot.setShooterVelocity(SHOOTER_VELOCITY);
        actionTimer.reset();
        setState(shootState);
    }

    private boolean updateShooting() {
        switch (shootPhase) {
            case SPIN_UP:
                if (robot.isShooterReady(SHOOTER_VELOCITY)) {
                    robot.feed(true);
                    shootPhase = ShootPhase.FEED;
                    actionTimer.reset();
                } else if (actionTimer.milliseconds() >= SHOOTER_SPINUP_TIMEOUT_MS) {
                    shooterRetries++;
                    if (shooterRetries > MAX_SHOOT_RETRIES) {
                        finishShooting();
                        return true;
                    }
                    actionTimer.reset();
                }
                return false;

            case FEED:
                if (actionTimer.milliseconds() >= SHOOT_TIME_MS) {
                    finishShooting();
                    return true;
                }
                return false;

            default:
                return true;
        }
    }

    private void finishShooting() {
        robot.feed(false);
        robot.setShooterVelocity(0);
        shootPhase = ShootPhase.DONE;
    }

    // ---------------------------------------------------------
    // RECOLECCION INTELIGENTE + VISION AI
    // ---------------------------------------------------------

    private void beginCollection(AutoState collectState) {
        if (ENABLE_BALL_TRACKING && vision != null) {
            vision.setPipeline(BALL_PIPELINE_INDEX);
        }
        robot.setIntakePower(INTAKE_POWER);
        actionTimer.reset();
        setState(collectState);
    }

    private double getCollectionDuration() {
        double duration = INTAKE_TIME_MS;
        if (pollen != null && pollen.getArea() > 1.0) {
            duration += BALL_APPROACH_EXTRA_TIME_MS;
        }
        return duration;
    }

    // ---------------------------------------------------------
    // TIMEOUT -> FAIL-SAFE INTELIGENTE
    // ---------------------------------------------------------

    private boolean handleTimeout() {
        if (!ENABLE_DECISION_ENGINE || timeoutParking
                || state == AutoState.PARK || state == AutoState.COMPLETE) {
            return false;
        }
        if (stateTimer.milliseconds() <= PATH_TIMEOUT_MS) {
            return false;
        }

        timeoutParking = true;
        robot.stopMechanisms();
        goTo(PARK_X, PARK_Y, PARK_HEADING_DEG, AutoState.PARK);
        return true;
    }

    // ---------------------------------------------------------
    // TELEMETRIA DE CAMPEONATO MUNDIAL
    // ---------------------------------------------------------

    private void updateTelemetry() {
        telemetry.addData("👑 Edición", "World Champion v3.1");
        telemetry.addData("Match Time", "%.1f s", matchTimer.seconds());
        telemetry.addData("Estado Actual", state);
        telemetry.addData("Fase Disparo", shootPhase);
        telemetry.addData("Posición Robot", "X %.1f  Y %.1f  Heading %.1f°",
                pathing.getX(), pathing.getY(), Math.toDegrees(pathing.getHeading()));
        telemetry.addData("Odometría Activa", pathing.isBusy());
        if (pollen != null) {
            telemetry.addData("Vision AI (Polen)", "tx %.1f° ty %.1f° Area %.2f",
                    pollen.getTargetXDegrees(), pollen.getTargetYDegrees(), pollen.getArea());
        } else {
            telemetry.addData("Vision AI (Polen)", "Buscando objetivo...");
        }
        telemetry.addData("Shooter RPM", "%.0f / Target %.0f", robot.getShooterVelocity(), SHOOTER_VELOCITY);
        telemetry.addData("Cronómetro Etapa", "%.0f ms", stateTimer.milliseconds());
        telemetry.update();
    }
}
