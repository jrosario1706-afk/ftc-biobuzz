package org.firstinspires.ftc.teamcode;

// ============================================================================
//  TeleOp de competencia con IA de decision a bordo
//  ---------------------------------------------------------------------------
//  ARCHIVO GENERADO POR fyf_generator.py  --  NO EDITAR A MANO
//  Edita tools/ftc/bio.robot.json (o la plantilla) y vuelve a ejecutar:
//      python fyf_generator.py generar biobuzz
//
//  Equipo: biobuzz   (2026-2027)
//  Modo:   BioBuzz TeleOp V2 (IA)
// ============================================================================

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;



@TeleOp(name = "BioBuzz TeleOp V2 (IA)", group = "BioBuzz")
public class BioBuzzTeleOpV2 extends LinearOpMode {

    private HardwareBioBuzz robot;

    @Override
    public void runOpMode() {
        robot = new HardwareBioBuzz(hardwareMap, telemetry);
        robot.inicializar();

        BioBuzzConfig.cargarPreferencias(hardwareMap);

        telemetry.addLine("==============================");
        telemetry.addLine(" BioBuzz TeleOp V2 (IA)");
        telemetry.addLine(" Equipo biobuzz - 2026-2027");
        telemetry.addLine("==============================");
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

            // --- chasis -----------------------------------------------------
            robot.conducir(
                    robot.ejeY(),
                    robot.ejeX(),
                    robot.giro(),
                    robot.modoLento() ? 0.35 : 0.9
            );

            // --- intake -----------------------------------------------------
            robot.controlarIntake(robot.botonIntakeEntra(), robot.botonIntakeSale());

            // --- lanzador ---------------------------------------------------
            double velocidadLanzador = robot.controlarLanzadorManual(
                    2000.0, robot.gatilloLanzador());
            robot.actualizarLanzador(velocidadLanzador);

            // --- vision -----------------------------------------------------
            robot.actualizarVision(1, 0,
                    0.4);

            // --- IA de decision a bordo -------------------------------------
            robot.pensar();

            // --- telemetria --------------------------------------------------
            robot.publicarTelemetria();
            telemetry.update();
            idle();
        }

        robot.parar();
    }
}
