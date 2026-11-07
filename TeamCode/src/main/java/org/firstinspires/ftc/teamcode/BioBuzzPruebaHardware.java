package org.firstinspires.ftc.teamcode;

// ============================================================================
//  Verifica motor por motor, intake y shooter
//  ---------------------------------------------------------------------------
//  ARCHIVO GENERADO POR fyf_generator.py  --  NO EDITAR A MANO
//  Edita tools/ftc/bio.robot.json (o la plantilla) y vuelve a ejecutar:
//      python fyf_generator.py generar biobuzz
//
//  Equipo: biobuzz   (2026-2027)
//  Modo:   BioBuzz Prueba de Hardware
// ============================================================================

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;



@TeleOp(name = "BioBuzz Prueba de Hardware", group = "BioBuzz")
public class BioBuzzPruebaHardware extends LinearOpMode {

    private HardwareBioBuzz robot;

    @Override
    public void runOpMode() {
        robot = new HardwareBioBuzz(hardwareMap, telemetry);
        String informe = robot.inicializarTolerante();
        robot.inicializar();

        telemetry.addLine("PRUEBA DE HARDWARE");
        telemetry.addLine(informe);
        telemetry.addLine("X/A/B/Y = probar cada motor");
        telemetry.addLine("bumpers = intake   gatillo = shooter");
        telemetry.update();

        waitForStart();
        if (isStopRequested()) {
            robot.parar();
            return;
        }

        while (opModeIsActive()) {
            robot.leerControles(gamepad1, gamepad2);
            robot.controlarIntake(robot.botonIntakeEntra(), robot.botonIntakeSale());
            robot.actualizarLanzador(robot.gatilloLanzador() * 2000.0);

            double prueba = 0.0;
            String quien = "ninguno";
            if (gamepad1.x) { prueba = 0.35; quien = "frontLeft"; }
            if (gamepad1.a) { prueba = 0.35; quien = "backLeft"; }
            if (gamepad1.b) { prueba = 0.35; quien = "frontRight"; }
            if (gamepad1.y) { prueba = 0.35; quien = "backRight"; }
            robot.probarMotor(quien, prueba);

            telemetry.addData("Motor en prueba", quien);
            telemetry.addData("Shooter (ticks/s)", "%.0f", robot.velocidadLanzador());
            telemetry.update();
            idle();
        }

        robot.parar();
    }
}
