package org.firstinspires.ftc.teamcode;

// ============================================================================
//  Depura el cerebro de decision sin tocar el robot
//  ---------------------------------------------------------------------------
//  ARCHIVO GENERADO POR fyf_generator.py  --  NO EDITAR A MANO
//  Edita tools/ftc/bio.robot.json (o la plantilla) y vuelve a ejecutar:
//      python fyf_generator.py generar biobuzz
//
//  Equipo: biobuzz   (2026-2027)
//  Modo:   BioBuzz IA (observador)
// ============================================================================

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;



@TeleOp(name = "BioBuzz IA (observador)", group = "BioBuzz")
public class BioBuzzIAObservador extends LinearOpMode {

    private HardwareBioBuzz robot;

    @Override
    public void runOpMode() {
        robot = new HardwareBioBuzz(hardwareMap, telemetry);
        robot.inicializar();

        telemetry.addLine("IA DE DECISION - modo observador");
        telemetry.addLine("La IA decide, el driver solo mira.");
        telemetry.addLine("B = forzar disparo   START = reiniciar cerebro");
        telemetry.update();

        waitForStart();
        if (isStopRequested()) {
            robot.parar();
            return;
        }
        robot.reiniciarTiempo();

        while (opModeIsActive()) {
            robot.leerControles(gamepad1, gamepad2);
            robot.actualizarVision(1, 0,
                    0.4);

            if (gamepad2.start) robot.reiniciarCerebro();
            robot.estadoForzado(gamepad2.b);

            robot.pensar();
            robot.publicarTelemetria();
            telemetry.addData("Cerebro", robot.cerebroResumen());
            telemetry.update();
            idle();
        }

        robot.parar();
    }
}
