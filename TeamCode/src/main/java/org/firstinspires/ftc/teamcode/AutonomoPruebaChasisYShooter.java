package org.firstinspires.ftc.teamcode;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.util.ElapsedTime;

/**
 * ============================================================================
 * BIOBUZZ - AUTÓNOMO SEGURO: DISPARO + AVANCE (PARKING)
 * ============================================================================
 * Diseñado especialmente para el robot en fase de pruebas:
 *   - NO crashea si falta algún cable o sensor.
 *   - Paso 1: Enciende el shooter y espera a que alcance máxima velocidad.
 *   - Paso 2: Dispara durante el tiempo configurado.
 *   - Paso 3: Apaga el shooter.
 *   - Paso 4: Avanza hacia adelante para salir de la pared y puntuar Parking.
 *   - Paso 5: Frena y termina con seguridad.
 *
 * Todas las duraciones y potencias son ajustables desde FTC Dashboard:
 *   http://192.168.43.1:8080/dash
 * ============================================================================
 */
@Config
@Autonomous(name = "BioBuzz: Auto Disparo + Parking", group = "BioBuzz")
public class AutonomoPruebaChasisYShooter extends LinearOpMode {

    // Variables ajustables en vivo desde FTC Dashboard:
    public static double POTENCIA_SHOOTER = 1.0;
    public static double SEGUNDOS_RAMPA_SHOOTER = 1.8;   // Tiempo para que el volante agarre inercia
    public static double SEGUNDOS_DISPARO = 2.0;         // Tiempo de disparo
    public static double POTENCIA_AVANCE = 0.50;         // 50% de potencia para no patinar
    public static double SEGUNDOS_AVANCE = 1.5;          // Tiempo de avance hacia la zona de estacionamiento

    private DcMotor frontLeft = null;
    private DcMotor frontRight = null;
    private DcMotor backLeft = null;
    private DcMotor backRight = null;
    private DcMotor shooter = null;

    private final ElapsedTime timer = new ElapsedTime();

    @Override
    public void runOpMode() {
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());
        telemetry.addLine(">> Detectando hardware...");
        telemetry.update();

        // 1. Detección tolerante a fallos
        frontLeft = buscarMotor(new String[]{"frontLeft", "leftFront", "fl"});
        frontRight = buscarMotor(new String[]{"frontRight", "rightFront", "fr"});
        backLeft = buscarMotor(new String[]{"backLeft", "leftBack", "bl", "leftRear"});
        backRight = buscarMotor(new String[]{"backRight", "rightBack", "br", "rightRear"});
        shooter = buscarMotor(new String[]{"shooter", "shooterMotor", "flywheel", "lanzador"});

        // 2. Configurar sentidos y frenado
        if (frontLeft != null) {
            frontLeft.setDirection(DcMotorSimple.Direction.FORWARD);
            frontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }
        if (backLeft != null) {
            backLeft.setDirection(DcMotorSimple.Direction.FORWARD);
            backLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }
        if (frontRight != null) {
            frontRight.setDirection(DcMotorSimple.Direction.REVERSE);
            frontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }
        if (backRight != null) {
            backRight.setDirection(DcMotorSimple.Direction.REVERSE);
            backRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }
        if (shooter != null) {
            shooter.setDirection(DcMotorSimple.Direction.FORWARD);
            shooter.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        }

        // 3. Telemetría de diagnóstico antes del inicio
        telemetry.clearAll();
        telemetry.addLine("=== BIOBUZZ AUTONOMO LISTO ===");
        telemetry.addData("Front Left", frontLeft != null ? "[OK]" : "[FALTA]");
        telemetry.addData("Front Right", frontRight != null ? "[OK]" : "[FALTA]");
        telemetry.addData("Back Left", backLeft != null ? "[OK]" : "[FALTA]");
        telemetry.addData("Back Right", backRight != null ? "[OK]" : "[FALTA]");
        telemetry.addData("Shooter", shooter != null ? "[OK]" : "[FALTA]");
        telemetry.addLine("------------------------------");
        telemetry.addLine("Presiona START cuando el juez de la orden.");
        telemetry.update();

        waitForStart();

        if (isStopRequested()) return;

        // -------------------------------------------------------------
        // ETAPA 1: ACELERAR EL SHOOTER
        // -------------------------------------------------------------
        telemetry.addLine(">> Etapa 1: Acelerando shooter a velocidad maxima...");
        telemetry.update();
        if (shooter != null) {
            shooter.setPower(POTENCIA_SHOOTER);
        }
        timer.reset();
        while (opModeIsActive() && timer.seconds() < SEGUNDOS_RAMPA_SHOOTER) {
            telemetry.addData("Shooter acelerando", "%.1f s", timer.seconds());
            telemetry.update();
            idle();
        }

        // -------------------------------------------------------------
        // ETAPA 2: DISPARO ACTIVO
        // -------------------------------------------------------------
        telemetry.addLine(">> Etapa 2: Disparando elemento pre-cargado...");
        telemetry.update();
        timer.reset();
        while (opModeIsActive() && timer.seconds() < SEGUNDOS_DISPARO) {
            telemetry.addData("Disparando", "%.1f s", timer.seconds());
            telemetry.update();
            idle();
        }

        // -------------------------------------------------------------
        // ETAPA 3: APAGAR SHOOTER
        // -------------------------------------------------------------
        telemetry.addLine(">> Etapa 3: Apagando shooter...");
        telemetry.update();
        if (shooter != null) {
            shooter.setPower(0);
        }
        sleep(200);

        // -------------------------------------------------------------
        // ETAPA 4: AVANZAR HACIA EL PARKING
        // -------------------------------------------------------------
        telemetry.addLine(">> Etapa 4: Avanzando para estacionar...");
        telemetry.update();

        moverChasis(POTENCIA_AVANCE);

        timer.reset();
        while (opModeIsActive() && timer.seconds() < SEGUNDOS_AVANCE) {
            telemetry.addData("Avanzando chasis", "%.1f / %.1f s", timer.seconds(), SEGUNDOS_AVANCE);
            telemetry.update();
            idle();
        }

        // -------------------------------------------------------------
        // ETAPA 5: DETENER TODO
        // -------------------------------------------------------------
        detenerTodo();
        telemetry.addLine(">> AUTONOMO COMPLETADO CON EXITO.");
        telemetry.update();
    }

    private void moverChasis(double potencia) {
        if (frontLeft != null) frontLeft.setPower(potencia);
        if (frontRight != null) frontRight.setPower(potencia);
        if (backLeft != null) backLeft.setPower(potencia);
        if (backRight != null) backRight.setPower(potencia);
    }

    private void detenerTodo() {
        moverChasis(0);
        if (shooter != null) shooter.setPower(0);
    }

    private DcMotor buscarMotor(String[] nombres) {
        for (String nombre : nombres) {
            try {
                DcMotor m = hardwareMap.get(DcMotor.class, nombre);
                if (m != null) return m;
            } catch (Exception ignored) {}
        }
        return null;
    }
}
