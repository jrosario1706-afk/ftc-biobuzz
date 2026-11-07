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
 * Autonomo "BIOBUZZ AI": tira la pre-carga y luego recorre puntos de busqueda.
 * Si la camara ve POLEN, lo recoge y vuelve a tirar. El NECTAR se ignora
 * automaticamente (JhoandriVision solo reporta la clase "pollen").
 */
@Config
@Autonomous(name = "BioBuzz Auto AI", group = "BioBuzz")
public class BioBuzzAuto extends LinearOpMode {

    public static double START_X = 9.0, START_Y = 111.0, START_H = 270.0;
    public static double SCORE_X = 16.0, SCORE_Y = 128.0, SCORE_H = 315.0;
    public static double SEARCH_X = 38.0, SEARCH_Y = 88.0, SEARCH_H = 180.0;
    public static double PARK_X = 12.0, PARK_Y = 105.0, PARK_H = 270.0;
    /** Cuantos ciclos de recoger + tirar intentar despues de la pre-carga. */
    public static int MAX_CYCLES = 2;
    /** Tiempo total del autonomo FTC menos un margen para estacionar. */
    public static double PARK_AT_SECONDS = 25.0;

    private enum AiState { DRIVE_SCORE, SPIN_UP, FEED, DRIVE_SEARCH, COLLECT, PARK, DONE }

    private JhoandriPathing pathing;
    private RobotHardware robot;
    private JhoandriVision vision;

    private AiState state = AiState.DRIVE_SCORE;
    private final ElapsedTime stateTimer = new ElapsedTime();
    private final ElapsedTime matchTimer = new ElapsedTime();
    private int cycles = 0;

    @Override
    public void runOpMode() {
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

        try {
            pathing = new JhoandriPathing(hardwareMap, true);
            robot = pathing.getRobot();
            vision = pathing.getVision();

            telemetry.addLine("BIOBUZZ AI listo");
            robot.reportTo(telemetry);
            telemetry.update();

            waitForStart();
            if (isStopRequested()) return;

            matchTimer.reset();
            pathing.resetPoseAndHeading(START_X, START_Y, Math.toRadians(START_H));
            goTo(SCORE_X, SCORE_Y, SCORE_H, AiState.DRIVE_SCORE);

            while (opModeIsActive() && state != AiState.DONE) {
                pathing.update();
                JhoandriVision.Detection pollen = vision != null ? vision.getBestPollen() : null;

                // Seguridad: siempre estacionar antes de que acabe el periodo.
                if (state != AiState.PARK && matchTimer.seconds() > PARK_AT_SECONDS) {
                    robot.stopMechanisms();
                    goTo(PARK_X, PARK_Y, PARK_H, AiState.PARK);
                }

                switch (state) {
                    case DRIVE_SCORE:
                        if (!pathing.isBusy() || stateTimer.milliseconds() > PATH_TIMEOUT_MS) {
                            robot.setShooterVelocity(SHOOTER_VELOCITY);
                            setState(AiState.SPIN_UP);
                        }
                        break;

                    case SPIN_UP:
                        if (robot.isShooterReady(SHOOTER_VELOCITY)
                                || stateTimer.milliseconds() > SHOOTER_SPINUP_TIMEOUT_MS) {
                            robot.feed(true);
                            setState(AiState.FEED);
                        }
                        break;

                    case FEED:
                        if (stateTimer.milliseconds() > SHOOT_TIME_MS) {
                            robot.feed(false);
                            robot.setShooterVelocity(0);
                            if (cycles < MAX_CYCLES) {
                                cycles++;
                                if (vision != null) vision.setPipeline(BALL_PIPELINE_INDEX);
                                goTo(SEARCH_X, SEARCH_Y, SEARCH_H, AiState.DRIVE_SEARCH);
                            } else {
                                goTo(PARK_X, PARK_Y, PARK_H, AiState.PARK);
                            }
                        }
                        break;

                    case DRIVE_SEARCH:
                        // Recoger en cuanto se vea polen, o al llegar al punto.
                        if (pollen != null || !pathing.isBusy()
                                || stateTimer.milliseconds() > PATH_TIMEOUT_MS) {
                            robot.setIntakePower(INTAKE_POWER);
                            setState(AiState.COLLECT);
                        }
                        break;

                    case COLLECT:
                        if (stateTimer.milliseconds() > INTAKE_TIME_MS) {
                            robot.setIntakePower(0);
                            if (vision != null) vision.setPipeline(0);
                            goTo(SCORE_X, SCORE_Y, SCORE_H, AiState.DRIVE_SCORE);
                        }
                        break;

                    case PARK:
                        if (!pathing.isBusy()) setState(AiState.DONE);
                        break;

                    case DONE:
                        break;
                }

                telemetry.addData("Estado AI", state);
                telemetry.addData("Ciclo", "%d / %d", cycles, MAX_CYCLES);
                telemetry.addData("Polen visible", pollen != null);
                telemetry.addData("Pose", "X %.1f  Y %.1f  H %.1f deg",
                        pathing.getX(), pathing.getY(), Math.toDegrees(pathing.getHeading()));
                telemetry.addData("Tiempo", "%.1f s", matchTimer.seconds());
                telemetry.update();
            }
        } catch (RuntimeException e) {
            telemetry.addData("AUTO ERROR", e.getMessage());
            telemetry.update();
            while (!isStopRequested()) idle();
        } finally {
            if (pathing != null) pathing.stop();
        }
    }

    private void goTo(double x, double y, double headingDeg, AiState next) {
        pathing.setTarget(x, y, Math.toRadians(headingDeg));
        setState(next);
    }

    private void setState(AiState next) {
        state = next;
        stateTimer.reset();
    }
}
