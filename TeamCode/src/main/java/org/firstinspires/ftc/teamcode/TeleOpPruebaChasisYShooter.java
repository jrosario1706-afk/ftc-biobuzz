package org.firstinspires.ftc.teamcode;

import com.acmerobotics.dashboard.FtcDashboard;
import com.acmerobotics.dashboard.config.Config;
import com.acmerobotics.dashboard.telemetry.MultipleTelemetry;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

/**
 * ============================================================================
 * TELEOP DE PRUEBA: CHASIS (4 MOTORES) + SHOOTER (1 MOTOR)
 * ============================================================================
 * Equipado con FTC Dashboard:
 *   - Puedes ver la telemetría en la pantalla del celular y en tu laptop
 *     abriendo en el navegador: http://192.168.43.1:8080/dash
 *   - Puedes ajustar las velocidades en vivo sin recompilar.
 *
 * Diseñado a prueba de errores: Si algún cable o motor está desconectado,
 * el programa NO se cierra; te avisa en pantalla exactamente qué cable falta.
 *
 * Nombres aceptados en la configuración de la Driver Station:
 *   - Rueda Delantera Izquierda: "frontLeft"  o "leftFront"
 *   - Rueda Delantera Derecha:   "frontRight" o "rightFront"
 *   - Rueda Trasera Izquierda:   "backLeft"   o "leftBack"
 *   - Rueda Trasera Derecha:    "backRight"  o "rightBack"
 *   - Motor del Shooter:         "shooter"    o "shooterMotor" o "flywheel"
 *
 * Controles (Gamepad 1):
 *   - Stick Izquierdo:  Avanzar / Retroceder / Desplazar lateralmente (Mecanum)
 *   - Stick Derecho X:  Girar izquierda / derecha
 *   - Botón A:          Modo Lento para precisión
 *   - Gatillo Der (RT): Acelerar Shooter progresivamente (0% a 100%)
 *   - Botón Y:          Disparar a velocidad fija (Toggle ON/OFF)
 * ============================================================================
 */
@Config
@TeleOp(name = "Test: Chasis + Shooter", group = "Pruebas")
public class TeleOpPruebaChasisYShooter extends LinearOpMode {

    // Variables ajustables en tiempo real desde FTC Dashboard (en la web):
    public static double VELOCIDAD_NORMAL = 0.85;
    public static double VELOCIDAD_LENTA = 0.35;
    public static double POTENCIA_SHOOTER_FIJA = 1.0;

    private DcMotor frontLeft = null;
    private DcMotor frontRight = null;
    private DcMotor backLeft = null;
    private DcMotor backRight = null;
    private DcMotor shooter = null;

    private boolean shooterEncendidoFijo = false;
    private boolean botonYPresionadoPrevio = false;

    @Override
    public void runOpMode() {
        // Enlazar telemetría con FTC Dashboard para ver los datos en la laptop
        telemetry = new MultipleTelemetry(telemetry, FtcDashboard.getInstance().getTelemetry());

        telemetry.addLine(">> Iniciando deteccion de hardware...");
        telemetry.update();

        // 1. Detección tolerante a fallos de los 4 motores del chasis
        frontLeft = buscarMotor(new String[]{"frontLeft", "leftFront", "fl"}, "Delantero Izquierdo");
        frontRight = buscarMotor(new String[]{"frontRight", "rightFront", "fr"}, "Delantero Derecho");
        backLeft = buscarMotor(new String[]{"backLeft", "leftBack", "bl", "leftRear"}, "Trasero Izquierdo");
        backRight = buscarMotor(new String[]{"backRight", "rightBack", "br", "rightRear"}, "Trasero Derecho");

        // 2. Detección del motor del Shooter
        shooter = buscarMotor(new String[]{"shooter", "shooterMotor", "flywheel", "lanzador"}, "Shooter");

        // 3. Configuración de sentido y frenado
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
            // FLOAT para que ruede libre y no frene de golpe
            shooter.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        }

        // 4. Mostrar reporte de estado
        telemetry.clearAll();
        telemetry.addLine("=== ESTADO DEL ROBOT DE PRUEBA ===");
        mostrarEstadoMotor("Front Left (Del. Izq)", frontLeft);
        mostrarEstadoMotor("Front Right (Del. Der)", frontRight);
        mostrarEstadoMotor("Back Left (Tras. Izq)", backLeft);
        mostrarEstadoMotor("Back Right (Tras. Der)", backRight);
        mostrarEstadoMotor("Shooter (Lanzador)", shooter);
        telemetry.addLine("----------------------------------");
        telemetry.addLine("Panel web: http://192.168.43.1:8080/dash");
        telemetry.addLine("Presiona START en el control cuando estes listo.");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            // -------------------------------------------------------------
            // CONTROL DEL CHASIS (MECANUM)
            // -------------------------------------------------------------
            double y = -gamepad1.left_stick_y; 
            double x = gamepad1.left_stick_x * 1.1; // Compensación de strafe
            double rx = gamepad1.right_stick_x;

            // Factor de velocidad (Normal o Lenta según botón A)
            double factorVelocidad = gamepad1.a ? VELOCIDAD_LENTA : VELOCIDAD_NORMAL;

            // Fórmulas estándar de cinemática Mecanum
            double denominador = Math.max(Math.abs(y) + Math.abs(x) + Math.abs(rx), 1.0);
            double potFl = ((y + x + rx) / denominador) * factorVelocidad;
            double potBl = ((y - x + rx) / denominador) * factorVelocidad;
            double potFr = ((y - x - rx) / denominador) * factorVelocidad;
            double potBr = ((y + x - rx) / denominador) * factorVelocidad;

            if (frontLeft != null) frontLeft.setPower(potFl);
            if (frontRight != null) frontRight.setPower(potFr);
            if (backLeft != null) backLeft.setPower(potBl);
            if (backRight != null) backRight.setPower(potBr);

            // -------------------------------------------------------------
            // CONTROL DEL SHOOTER
            // -------------------------------------------------------------
            if (gamepad1.y && !botonYPresionadoPrevio) {
                shooterEncendidoFijo = !shooterEncendidoFijo;
            }
            botonYPresionadoPrevio = gamepad1.y;

            double potShooter = 0.0;
            if (shooterEncendidoFijo) {
                potShooter = POTENCIA_SHOOTER_FIJA;
            } else if (gamepad1.right_trigger > 0.05) {
                potShooter = gamepad1.right_trigger;
            }

            if (shooter != null) {
                shooter.setPower(potShooter);
            }

            // -------------------------------------------------------------
            // TELEMETRÍA EN VIVO (DRIVER STATION + DASHBOARD EN LAPTOP)
            // -------------------------------------------------------------
            telemetry.addData(">> Modo", gamepad1.a ? "LENTO" : "NORMAL");
            telemetry.addData(">> Shooter", "%.0f%% %s", (potShooter * 100.0), shooterEncendidoFijo ? "[FIJO]" : "");
            telemetry.addData("Potencias FL/FR", "%.2f | %.2f", potFl, potFr);
            telemetry.addData("Potencias BL/BR", "%.2f | %.2f", potBl, potBr);

            // Si algún motor está desconectado, avisa permanentemente en pantalla
            if (frontLeft == null || frontRight == null || backLeft == null || backRight == null || shooter == null) {
                telemetry.addLine("\n[ATENCION - CABLE(S) FALTANTES]");
                if (frontLeft == null) telemetry.addLine("- Falta 'frontLeft'");
                if (frontRight == null) telemetry.addLine("- Falta 'frontRight'");
                if (backLeft == null) telemetry.addLine("- Falta 'backLeft'");
                if (backRight == null) telemetry.addLine("- Falta 'backRight'");
                if (shooter == null) telemetry.addLine("- Falta 'shooter'");
            }

            telemetry.update();
        }

        frenarTodo();
    }

    private DcMotor buscarMotor(String[] nombresPosibles, String descripcion) {
        for (String nombre : nombresPosibles) {
            try {
                DcMotor motor = hardwareMap.get(DcMotor.class, nombre);
                if (motor != null) {
                    return motor;
                }
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private void mostrarEstadoMotor(String etiqueta, DcMotor motor) {
        if (motor != null) {
            telemetry.addData(etiqueta, "[OK] Conectado");
        } else {
            telemetry.addData(etiqueta, "[DESCONECTADO O MAL CONFIGURADO]");
        }
    }

    private void frenarTodo() {
        if (frontLeft != null) frontLeft.setPower(0);
        if (frontRight != null) frontRight.setPower(0);
        if (backLeft != null) backLeft.setPower(0);
        if (backRight != null) backRight.setPower(0);
        if (shooter != null) shooter.setPower(0);
    }
}
