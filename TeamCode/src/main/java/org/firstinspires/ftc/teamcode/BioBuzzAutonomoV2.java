package org.firstinspires.ftc.teamcode;

// ============================================================================
//  BioBuzzAutonomoV2 -- Autonomo optimizado para ciclo rapido:
//  1. Disparo veloz de pre-carga con Limelight Auto-Aiming.
//  2. Recoleccion frontal con intake + avance mecanum optimizado.
//  3. Retorno y segundo disparo asistido por vision.
//  4. Navegacion segura a zona de estacionamiento (Parking).
// ============================================================================

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

@Autonomous(name = "BioBuzz Autonomo V2 (Optimizado)", group = "BioBuzz")
public class BioBuzzAutonomoV2 extends LinearOpMode {

    private HardwareBioBuzz robot;
    private final ElapsedTime timerPaso = new ElapsedTime();

    @Override
    public void runOpMode() {
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        robot = new HardwareBioBuzz(hardwareMap, telemetry);
        robot.inicializar();

        telemetry.addLine("==============================");
        telemetry.addLine(" BIOBUZZ AUTONOMO V2 (PRO)");
        telemetry.addLine(" FTC TEAM 24096 - 2026/2027");
        telemetry.addLine("==============================");
        telemetry.addData("Hardware", robot.resumenHardware());
        telemetry.addData("Bateria", "%.2f V", robot.getVoltajeBateria());
        telemetry.addLine("Listo para iniciar.");
        telemetry.update();

        waitForStart();
        if (isStopRequested()) {
            robot.parar();
            return;
        }

        robot.reiniciarTiempo();

        // --------------------------------------------------------------------
        // PASO 1: Disparo de pre-carga con Auto-Aim Limelight 3A y Spin-up paralelo
        // --------------------------------------------------------------------
        robot.paso("1/4 Pre-carga: alineando y disparando");
        robot.actualizarLanzador(BioBuzzConfig.BASE_SHOOTER_VELOCITY);

        // Alineacion visual ultra-rapida con lazo PD + kS
        timerPaso.reset();
        while (opModeIsActive() && timerPaso.milliseconds() < 450) {
            VisionData v = robot.datosVision();
            robot.actualizarVision(BioBuzzConfig.PIPELINE_NEURAL_IA, BioBuzzConfig.PIPELINE_COLOR, BioBuzzConfig.MIN_CONFIDENCE);
            robot.actualizarLanzador(BioBuzzConfig.BASE_SHOOTER_VELOCITY);
            double giro = robot.calcularGiroAutoAim(v);
            if (v.tieneObjetivo && Math.abs(v.tx) <= BioBuzzConfig.AUTO_AIM_TOLERANCE) {
                robot.detenerChasis();
                break;
            }
            robot.conducir(0.0, 0.0, giro, 1.0);
            idle();
        }
        robot.detenerChasis();

        // Esperar estabilizacion fina de RPM (max 450ms gracias al PIDF optimizado)
        timerPaso.reset();
        while (opModeIsActive() && timerPaso.milliseconds() < 450 && !robot.lanzadorListo()) {
            robot.actualizarLanzador(BioBuzzConfig.BASE_SHOOTER_VELOCITY);
            idle();
        }

        // Alimentar pre-carga (pulso no-bloqueante con shooter activo)
        robot.setIntake(BioBuzzConfig.INTAKE_POWER);
        timerPaso.reset();
        while (opModeIsActive() && timerPaso.milliseconds() < 320) {
            robot.actualizarLanzador(BioBuzzConfig.BASE_SHOOTER_VELOCITY);
            idle();
        }
        robot.setIntake(0.0);
        // Pre-giro idle: mantiene inercia para acelerar el segundo ciclo
        robot.actualizarLanzador(BioBuzzConfig.IDLE_SHOOTER_VELOCITY);

        // --------------------------------------------------------------------
        // PASO 2: Recoleccion activa coordinada (avance con intake encendido)
        // --------------------------------------------------------------------
        robot.paso("2/4 Recoleccion activa de polen");
        robot.avanzarCoordinado(0.55, 0.0, 0.0, BioBuzzConfig.IDLE_SHOOTER_VELOCITY, BioBuzzConfig.INTAKE_POWER, 850);
        robot.setIntake(0.0);

        // --------------------------------------------------------------------
        // PASO 3: Retorno con pre-aceleracion de shooter en transito
        // --------------------------------------------------------------------
        robot.paso("3/4 Retorno y disparo pieza 2");
        // Durante el retroceso, el lanzador ya alcanza la velocidad objetivo desde velocidad idle
        robot.avanzarCoordinado(-0.50, 0.0, 0.0, BioBuzzConfig.BASE_SHOOTER_VELOCITY, 0.0, 700);

        // Alineacion final con vision (objetivo ya casi centrado)
        timerPaso.reset();
        while (opModeIsActive() && timerPaso.milliseconds() < 350) {
            VisionData v = robot.datosVision();
            robot.actualizarVision(BioBuzzConfig.PIPELINE_NEURAL_IA, BioBuzzConfig.PIPELINE_COLOR, BioBuzzConfig.MIN_CONFIDENCE);
            robot.actualizarLanzador(BioBuzzConfig.BASE_SHOOTER_VELOCITY);
            double giro = robot.calcularGiroAutoAim(v);
            if (v.tieneObjetivo && Math.abs(v.tx) <= BioBuzzConfig.AUTO_AIM_TOLERANCE) {
                robot.detenerChasis();
                break;
            }
            robot.conducir(0.0, 0.0, giro, 1.0);
            idle();
        }
        robot.detenerChasis();

        // Disparo de segunda pieza (como ya giro en transito, el lanzador ya esta listo)
        timerPaso.reset();
        while (opModeIsActive() && timerPaso.milliseconds() < 300 && !robot.lanzadorListo()) {
            robot.actualizarLanzador(BioBuzzConfig.BASE_SHOOTER_VELOCITY);
            idle();
        }
        robot.setIntake(BioBuzzConfig.INTAKE_POWER);
        timerPaso.reset();
        while (opModeIsActive() && timerPaso.milliseconds() < 320) {
            robot.actualizarLanzador(BioBuzzConfig.BASE_SHOOTER_VELOCITY);
            idle();
        }
        robot.setIntake(0.0);
        robot.actualizarLanzador(0.0);

        // --------------------------------------------------------------------
        // PASO 4: Estacionar (Parking)
        // --------------------------------------------------------------------
        robot.paso("4/4 Navegando a zona de estacionamiento");
        robot.avanzarCoordinado(0.55, 0.0, 0.0, 0.0, 0.0, 750);
        robot.detenerChasis();

        // Mantener telemetria de cierre
        while (opModeIsActive() && robot.tiempo() < BioBuzzConfig.DURACION_AUTONOMO_S) {
            robot.publicarTelemetria();
            telemetry.addData("Autonomo", "RUTINA COMPLETADA EN %.1fs", robot.tiempo());
            telemetry.update();
            idle();
        }

        robot.parar();
    }
}
