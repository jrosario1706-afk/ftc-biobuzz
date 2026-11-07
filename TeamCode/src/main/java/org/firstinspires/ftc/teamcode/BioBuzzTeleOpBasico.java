package org.firstinspires.ftc.teamcode;

// ============================================================================
//  BioBuzz TeleOp Basico
//  ---------------------------------------------------------------------------
//  TeleOp minimo para manejar el robot a mano. Sin IA, sin auto-aim.
//
//  Controles (gamepad1):
//      Stick izquierdo  -> avanzar / retroceder / desplazar
//      Stick derecho X  -> girar
//      Boton A          -> modo lento (precision)
//      Bumper derecho   -> intake entra
//      Bumper izquierdo -> intake sale
//      Gatillo derecho  -> lanzador (shooter)
// ============================================================================

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;

@TeleOp(name = "BioBuzz TeleOp Basico", group = "BioBuzz")
public class BioBuzzTeleOpBasico extends LinearOpMode {

    private HardwareBioBuzz robot;

    @Override
    public void runOpMode() {
        robot = new HardwareBioBuzz(hardwareMap, telemetry);
        robot.inicializar();

        telemetry.addLine("==============================");
        telemetry.addLine(" BioBuzz TeleOp Basico");
        telemetry.addLine("==============================");
        telemetry.addLine("Stick izq: mover | Stick der X: girar");
        telemetry.addLine("A: modo lento | Bumpers: intake | Gatillo: shooter");
        telemetry.addData("Estado", robot.resumenHardware());
        telemetry.update();

        waitForStart();
        if (isStopRequested()) {
            robot.parar();
            return;
        }
        robot.reiniciarTiempo();

        while (opModeIsActive()) {
            robot.leerControles(gamepad1, gamepad2);

            // --- chasis mecanum ---------------------------------------------
            double velocidad = gamepad1.a ? 0.35 : 0.9;
            robot.conducir(
                    robot.ejeY(),
                    robot.ejeX(),
                    robot.giro(),
                    velocidad
            );

            // --- intake -----------------------------------------------------
            robot.controlarIntake(robot.botonIntakeEntra(), robot.botonIntakeSale());

            // --- lanzador ---------------------------------------------------
            double velocidadLanzador = robot.controlarLanzadorManual(
                    2000.0, robot.gatilloLanzador());
            robot.actualizarLanzador(velocidadLanzador);

            // --- telemetria -------------------------------------------------
            telemetry.addData("Velocidad", "%.2f", velocidad);
            telemetry.addData("Shooter (ticks/s)", "%.0f", robot.velocidadLanzador());
            telemetry.update();
        }

        robot.parar();
    }
}
