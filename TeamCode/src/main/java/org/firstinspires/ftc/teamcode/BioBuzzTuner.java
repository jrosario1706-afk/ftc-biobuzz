package org.firstinspires.ftc.teamcode;

// ============================================================================
//  Ajuste de constantes con el gamepad 2 sin recompilar
//  ---------------------------------------------------------------------------
//  ARCHIVO GENERADO POR fyf_generator.py  --  NO EDITAR A MANO
//  Edita tools/ftc/bio.robot.json (o la plantilla) y vuelve a ejecutar:
//      python fyf_generator.py generar biobuzz
//
//  Equipo: biobuzz   (2026-2027)
//  Modo:   BioBuzz Tuner (constantes en vivo)
// ============================================================================

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;



@TeleOp(name = "BioBuzz Tuner (constantes en vivo)", group = "BioBuzz")
public class BioBuzzTuner extends LinearOpMode {

    private HardwareBioBuzz robot;

    // Parametros vivos que se ajustan con el gamepad 2
    private final double[] valores = {
            0.900, 0.350,
            0.900, 2000.0,
            0.400
    };
    private final String[] nombres = {
            "DRIVE_SPEED", "SLOW_DRIVE_SPEED", "INTAKE_POWER",
            "MAX_SHOOTER_VELOCITY", "MIN_CONFIDENCE"
    };
    private final double[] pasos = {0.02, 0.02, 0.02, 25.0, 0.02};
    private final double[] minimos = {0.10, 0.05, 0.10, 200.0, 0.05};
    private final double[] maximos = {1.00, 0.80, 1.00, 3000.0, 0.95};

    private int seleccion = 0;
    private boolean arribaAntes = false;
    private boolean abajoAntes = false;
    private boolean izqAntes = false;
    private boolean derAntes = false;
    private boolean aAntes = false;

    @Override
    public void runOpMode() {
        robot = new HardwareBioBuzz(hardwareMap, telemetry);
        robot.inicializar();

        telemetry.addLine("TUNER BIOBUZZ - gamepad2:");
        telemetry.addLine("  arriba/abajo = elegir parametro");
        telemetry.addLine("  izquierda/derecha = ajustar");
        telemetry.addLine("  A = guardar en BioBuzzConfig");
        telemetry.update();

        waitForStart();
        if (isStopRequested()) {
            robot.parar();
            return;
        }

        while (opModeIsActive()) {
            robot.leerControles(gamepad1, gamepad2);
            procesarControles();

            // Aplica el valor vivo
            BioBuzzConfig.DRIVE_SPEED = valores[0];
            BioBuzzConfig.SLOW_DRIVE_SPEED = valores[1];
            BioBuzzConfig.INTAKE_POWER = valores[2];
            BioBuzzConfig.MAX_SHOOTER_VELOCITY = valores[3];
            BioBuzzConfig.MIN_CONFIDENCE = valores[4];

            robot.conducir(robot.ejeY(), robot.ejeX(), robot.giro(), valores[0]);
            robot.controlarIntake(robot.botonIntakeEntra(), robot.botonIntakeSale());
            robot.actualizarLanzador(valores[3] * robot.gatilloLanzador());
            robot.actualizarVision(1, 0, valores[4]);

            telemetry.addData("Tiempo", "%.1f s", robot.tiempo());
            telemetry.addData(">> Parametro", nombres[seleccion] + " = " + fmt(valores[seleccion]));
            telemetry.addData("Shooter real", "%.0f", robot.velocidadLanzador());
            telemetry.addData("Objetivo", robot.objetivoTexto());
            robot.publicarTelemetria();
            telemetry.update();
            idle();
        }

        robot.parar();
    }

    private String fmt(double v) {
        return String.format("%.3f", v);
    }

    private void procesarControles() {
        boolean arriba = gamepad2.dpad_up;
        boolean abajo = gamepad2.dpad_down;
        boolean izq = gamepad2.dpad_left;
        boolean der = gamepad2.dpad_right;
        boolean a = gamepad2.a;

        if (arriba && !arribaAntes) seleccion = (seleccion + 1) % valores.length;
        if (abajo && !abajoAntes) seleccion = (seleccion - 1 + valores.length) % valores.length;
        if (izq && !izqAntes) ajustar(-1);
        if (der && !derAntes) ajustar(+1);
        if (a && !aAntes) BioBuzzConfig.guardarPreferencias(hardwareMap);

        arribaAntes = arriba;
        abajoAntes = abajo;
        izqAntes = izq;
        derAntes = der;
        aAntes = a;
    }

    private void ajustar(int signo) {
        valores[seleccion] = Math.max(minimos[seleccion],
                Math.min(maximos[seleccion],
                        valores[seleccion] + signo * pasos[seleccion]));
    }
}
