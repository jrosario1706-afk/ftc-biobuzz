package org.firstinspires.ftc.teamcode.calibration;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.jhoandri.pathing.JhoandriPathing;
import org.jhoandri.pathing.JhoandriPathingConstants;

/**
 * OpMode de calibracion para Jhoandri Pathing (avance, strafe y giro).
 *
 * Controles (Gamepad 1):
 *   A = avanzar TEST_DISTANCE_IN pulgadas
 *   B = strafe izquierda TEST_DISTANCE_IN pulgadas
 *   Y = girar TEST_TURN_DEG grados
 *   X = resetear pose a (0, 0, 0)
 */
@Config
@TeleOp(name = "Jhoandri Pathing Calibration", group = "BioBuzz")
public class JhoandriPathingCalibration extends LinearOpMode {

    public static double TEST_DISTANCE_IN = 48.0;
    public static double MEASURED_DISTANCE_IN = 48.0;
    public static double TEST_TURN_DEG = 90.0;

    private enum TestType { NONE, FORWARD, STRAFE, TURN }

    @Override
    public void runOpMode() {
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

        JhoandriPathing pathing = new JhoandriPathing(hardwareMap, false);

        boolean lastA = false, lastB = false, lastY = false, lastX = false;
        TestType lastTest = TestType.NONE;

        telemetry.addLine("JHOANDRI PATHING - CALIBRATION");
        telemetry.addLine("A = avanzar | B = strafe izq | Y = girar | X = reset");
        telemetry.update();

        try {
            waitForStart();
            if (isStopRequested()) return;

            while (opModeIsActive()) {
                boolean aPressed = gamepad1.a && !lastA;
                boolean bPressed = gamepad1.b && !lastB;
                boolean yPressed = gamepad1.y && !lastY;
                boolean xPressed = gamepad1.x && !lastX;

                if (!pathing.isBusy()) {
                    if (xPressed) {
                        pathing.resetPoseAndHeading(0, 0, 0);
                        lastTest = TestType.NONE;
                    } else if (aPressed && TEST_DISTANCE_IN > 0) {
                        pathing.resetPoseAndHeading(0, 0, 0);
                        pathing.setTarget(TEST_DISTANCE_IN, 0, 0);
                        lastTest = TestType.FORWARD;
                    } else if (bPressed && TEST_DISTANCE_IN > 0) {
                        pathing.resetPoseAndHeading(0, 0, 0);
                        pathing.setTarget(0, TEST_DISTANCE_IN, 0);
                        lastTest = TestType.STRAFE;
                    } else if (yPressed) {
                        pathing.resetPoseAndHeading(0, 0, 0);
                        pathing.setTarget(0, 0, Math.toRadians(TEST_TURN_DEG));
                        lastTest = TestType.TURN;
                    }
                }

                boolean arrived = pathing.update();
                int[] ticks = pathing.getDriveEncoderTicks();

                double x = pathing.getX();
                double y = pathing.getY();
                double hDeg = Math.toDegrees(pathing.getHeading());

                telemetry.addData("Estado", pathing.isBusy()
                        ? "MOVIENDO" : (arrived ? "OBJETIVO ALCANZADO" : "LISTO"));
                telemetry.addData("Ultima prueba", lastTest);
                telemetry.addData("Pose", "X %.2f in | Y %.2f in | H %.1f deg", x, y, hDeg);

                if (ticks != null && ticks.length >= 4) {
                    telemetry.addData("Encoders", "LF %d | LR %d | RF %d | RR %d",
                            ticks[0], ticks[1], ticks[2], ticks[3]);
                }

                if (TEST_DISTANCE_IN > 0 && MEASURED_DISTANCE_IN > 0) {
                    if (lastTest == TestType.STRAFE) {
                        telemetry.addData("STRAFE_MULTIPLIER sugerido", "%.5f",
                                JhoandriPathingConstants.STRAFE_MULTIPLIER
                                        * MEASURED_DISTANCE_IN / TEST_DISTANCE_IN);
                    } else {
                        telemetry.addData("FORWARD_MULTIPLIER sugerido", "%.5f",
                                JhoandriPathingConstants.FORWARD_MULTIPLIER
                                        * MEASURED_DISTANCE_IN / TEST_DISTANCE_IN);
                    }
                }

                telemetry.update();

                lastA = gamepad1.a;
                lastB = gamepad1.b;
                lastY = gamepad1.y;
                lastX = gamepad1.x;
                idle();
            }
        } finally {
            pathing.stop();
        }
    }
}
