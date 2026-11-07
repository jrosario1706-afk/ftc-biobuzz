package org.firstinspires.ftc.teamcode;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.teamcode.hardware.RobotConfig;
import org.firstinspires.ftc.teamcode.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.hardware.VisionTarget;

/**
 * TeleOp principal con auto-apuntado por Limelight.
 *
 * GAMEPAD 1 (piloto)
 *   Stick izq ........ mover          Stick der X ... girar
 *   X (mantener) ..... auto-apuntar al objeto mas cercano
 *   Bumper der/izq ... intake adentro / afuera
 *   Gatillo der ...... velocidad del shooter (vibra cuando esta listo)
 *   Gatillo izq ...... modo preciso (lento)
 *   Back ............. alternar manejo relativo a la cancha (field-centric)
 *   Options .......... reiniciar heading (mirar al frente y presionar)
 *
 * GAMEPAD 2 (operador)
 *   D-pad arriba/abajo  transfer adelante / atras
 */
@Config
@TeleOp(name = "BioBuzz TeleOp Pro", group = "BioBuzz")
public class BioBuzzTeleOpPro extends LinearOpMode {

    // ===== Ajustables desde FTC Dashboard =====
    public static double DRIVE_SPEED = 0.90;
    public static double PRECISION_SPEED = 0.35;
    public static double AUTO_AIM_KP = 0.035;
    public static double AUTO_AIM_MAX_TURN = 0.45;
    public static double AUTO_AIM_TOLERANCE_DEG = 1.8;

    @Override
    public void runOpMode() {
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

        RobotHardware robot = new RobotHardware(hardwareMap);

        telemetry.addLine("BioBuzz TeleOp Pro listo");
        robot.reportTo(telemetry);
        telemetry.update();

        waitForStart();

        boolean fieldCentric = false;
        boolean lastBack = false;
        boolean shooterWasReady = false;
        ElapsedTime loopTimer = new ElapsedTime();

        try {
            while (opModeIsActive()) {

                // ===== VISION =====
                VisionTarget target = robot.getClosestTarget();

                // ===== MODOS =====
                if (gamepad1.back && !lastBack) fieldCentric = !fieldCentric;
                lastBack = gamepad1.back;
                if (gamepad1.options) robot.resetHeading();

                // ===== CHASIS + AUTO-APUNTADO =====
                double forward = -gamepad1.left_stick_y;
                double strafe = gamepad1.left_stick_x;
                double turn = gamepad1.right_stick_x;

                boolean autoAim = gamepad1.x && target != null;
                if (autoAim) {
                    turn = Math.abs(target.tx) < AUTO_AIM_TOLERANCE_DEG
                            ? 0.0
                            : Range.clip(target.tx * AUTO_AIM_KP,
                                    -AUTO_AIM_MAX_TURN, AUTO_AIM_MAX_TURN);
                }

                double speed = gamepad1.left_trigger > 0.3 ? PRECISION_SPEED : DRIVE_SPEED;
                if (fieldCentric && robot.hasImu()) {
                    robot.driveFieldCentric(forward, strafe, turn, speed);
                } else {
                    robot.drive(forward, strafe, turn, speed);
                }

                // ===== INTAKE =====
                if (gamepad1.right_bumper) robot.setIntakePower(RobotConfig.INTAKE_POWER);
                else if (gamepad1.left_bumper) robot.setIntakePower(-RobotConfig.INTAKE_POWER);
                else robot.setIntakePower(0);

                // ===== TRANSFER =====
                if (gamepad2.dpad_up) robot.setTransferPower(RobotConfig.TRANSFER_POWER);
                else if (gamepad2.dpad_down) robot.setTransferPower(-RobotConfig.TRANSFER_POWER);
                else robot.setTransferPower(0);

                // ===== SHOOTER (control de velocidad con encoder) =====
                double shooterTarget = gamepad1.right_trigger * RobotConfig.SHOOTER_MAX_VELOCITY;
                robot.setShooterVelocity(shooterTarget);

                boolean shooterReady = gamepad1.right_trigger > 0.1
                        && robot.isShooterReady(shooterTarget);
                if (shooterReady && !shooterWasReady) {
                    gamepad1.rumble(200); // aviso al piloto: ya puede disparar
                }
                shooterWasReady = shooterReady;

                // ===== TELEMETRIA =====
                telemetry.addData("Modo", fieldCentric ? "CANCHA (field-centric)" : "ROBOT");
                telemetry.addData("Auto-Aim", autoAim ? "ON" : "OFF");
                if (target != null) {
                    telemetry.addData("Objeto cercano", target.name);
                    telemetry.addData("tx", "%.1f deg", target.tx);
                    telemetry.addData("Confianza", "%.0f%%", target.confidence * 100);
                    telemetry.addData("Polen / Nectar", "%d / %d",
                            target.pollenCount, target.nectarCount);
                } else {
                    telemetry.addData("Objeto cercano", "Ninguno");
                }
                telemetry.addData("Shooter", "%.0f / %.0f %s", robot.getShooterVelocity(),
                        shooterTarget, shooterReady ? "LISTO" : "");
                telemetry.addData("Heading", "%.1f deg", Math.toDegrees(robot.getHeadingRad()));
                telemetry.addData("Loop", "%.0f ms", loopTimer.milliseconds());
                loopTimer.reset();
                telemetry.update();
            }
        } finally {
            robot.shutdown();
        }
    }
}
