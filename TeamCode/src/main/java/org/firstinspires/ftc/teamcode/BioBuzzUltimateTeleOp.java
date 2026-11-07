package org.firstinspires.ftc.teamcode;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

import org.firstinspires.ftc.teamcode.hardware.RobotConfig;
import org.firstinspires.ftc.teamcode.hardware.RobotHardware;

/**
 * TeleOp de dos pilotos con manejo relativo a la cancha (field-centric).
 *
 * GAMEPAD 1 (piloto)
 *   Stick izq ........ mover (relativo a la cancha)   Stick der X ... girar
 *   Bumper izq ....... lento (30%)    Bumper der ... turbo (100%)    normal 75%
 *   Options .......... reiniciar heading (robot mirando al frente)
 *
 * GAMEPAD 2 (operador)
 *   Gatillo der/izq .. intake adentro / afuera
 *   D-pad arriba/abajo transfer adelante / atras
 *   Bumper der ....... encender shooter (vibra cuando esta a velocidad)
 *   Bumper izq ....... ALIMENTACION AUTOMATICA: enciende el shooter y solo
 *                      empuja la pieza cuando ya esta a velocidad
 */
@Config
@TeleOp(name = "BioBuzz Ultimate TeleOp", group = "BioBuzz")
public class BioBuzzUltimateTeleOp extends LinearOpMode {

    public static double SLOW_SPEED = 0.30;
    public static double NORMAL_SPEED = 0.75;
    public static double TURBO_SPEED = 1.0;

    @Override
    public void runOpMode() {
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

        // Este TeleOp no usa la camara: no se enciende para ahorrar recursos.
        RobotHardware robot = new RobotHardware(hardwareMap, false);

        telemetry.addLine("Robot listo");
        robot.reportTo(telemetry);
        if (!robot.hasImu()) telemetry.addLine("AVISO: sin IMU -> manejo relativo al robot");
        telemetry.update();

        waitForStart();

        boolean shooterWasReady = false;
        com.qualcomm.robotcore.util.ElapsedTime loopTimer = new com.qualcomm.robotcore.util.ElapsedTime();
        double loopHz = 0.0;

        try {
            while (opModeIsActive()) {
                double dt = loopTimer.seconds();
                loopTimer.reset();
                if (dt > 0) {
                    double currentHz = 1.0 / dt;
                    loopHz = loopHz == 0.0 ? currentHz : (0.90 * loopHz + 0.10 * currentHz);
                }

                // ===== CHASIS =====
                if (gamepad1.options) robot.resetHeading();

                double speed = gamepad1.left_bumper ? SLOW_SPEED
                        : gamepad1.right_bumper ? TURBO_SPEED
                        : NORMAL_SPEED;

                robot.driveFieldCentric(
                        -gamepad1.left_stick_y,
                        gamepad1.left_stick_x,
                        gamepad1.right_stick_x,
                        speed);

                // ===== SHOOTER =====
                boolean autoFeed = gamepad2.left_bumper;
                boolean shooterOn = gamepad2.right_bumper || autoFeed;
                // Antes era setPower(1.0) sin control: la velocidad bajaba con la bateria.
                // Ahora se mantiene constante con el encoder (ajustable en Dashboard).
                double shooterTarget = shooterOn ? RobotConfig.SHOOTER_MAX_VELOCITY : 0;
                robot.setShooterVelocity(shooterTarget);
                boolean shooterReady = shooterOn && robot.isShooterReady(shooterTarget);

                if (shooterReady && !shooterWasReady) gamepad2.rumble(200);
                shooterWasReady = shooterReady;

                // ===== INTAKE / TRANSFER =====
                if (autoFeed) {
                    // Antes se empujaba la pieza aunque el shooter estuviera lento,
                    // lo que hacia tiros cortos. Ahora espera a que este a velocidad.
                    robot.feed(shooterReady);
                } else {
                    if (gamepad2.right_trigger > 0.1) robot.setIntakePower(1.0);
                    else if (gamepad2.left_trigger > 0.1) robot.setIntakePower(-1.0);
                    else robot.setIntakePower(0);

                    if (gamepad2.dpad_up) robot.setTransferPower(1.0);
                    else if (gamepad2.dpad_down) robot.setTransferPower(-1.0);
                    else robot.setTransferPower(0);
                }

                // ===== TELEMETRIA =====
                telemetry.addData("Sistema", "%.0f Hz | Campo-Centrico: %s", loopHz, robot.hasImu() ? "IMU OK" : "RELATIVO");
                telemetry.addData("Heading", "%.1f deg", Math.toDegrees(robot.getHeadingRad()));
                telemetry.addData("Velocidad", "%.0f%%", speed * 100);
                telemetry.addData("Shooter", "%.0f / %.0f %s", robot.getShooterVelocity(),
                        shooterTarget, shooterReady ? "LISTO" : "");
                telemetry.addData("Auto feed", autoFeed ? (shooterReady ? "DISPARANDO" : "ESPERANDO VELOCIDAD") : "-");
                telemetry.update();
            }
        } finally {
            robot.shutdown();
        }
    }
}
