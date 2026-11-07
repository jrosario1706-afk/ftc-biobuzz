package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.util.ElapsedTime;

// ============================================================================
//  AutonomoAvanzar1Metro
//  Avanza exactamente 1 metro usando los encoders del chasis (mecanum).
//
//  Calculo de TICKS_POR_METRO:
//    ticks_por_rev_motor * relacion_engranaje
//    ----------------------------------------  = ticks por metro
//    pi * diametro_rueda_metros
//
//  Valores por defecto (goBILDA 5202 312 RPM, rueda 96 mm):
//    ticks_por_rev_motor = 537.7
//    diametro_rueda      = 0.096 m
//    relacion            = 1.0
//    => ~1783 ticks/m  (ajusta si tu robot es distinto)
// ============================================================================

@Autonomous(name = "Avanzar 1 Metro (Encoders)", group = "BioBuzz")
public class AutonomoAvanzar1Metro extends LinearOpMode {

    // --- Parametros de calibracion ---
    private static final double TICKS_POR_METRO   = 1783.0; // ajusta a tu robot
    private static final double DISTANCIA_METROS  = 1.0;
    private static final double VELOCIDAD         = 0.5;    // 0..1
    private static final double TOLERANCIA_TICKS  = 20.0;
    private static final long   TIMEOUT_MS        = 8000;

    // --- Nombres de hardware (deben coincidir con el Driver Station) ---
    private static final String NOMBRE_FRONT_LEFT  = "frontLeft";
    private static final String NOMBRE_FRONT_RIGHT = "frontRight";
    private static final String NOMBRE_BACK_LEFT   = "backLeft";
    private static final String NOMBRE_BACK_RIGHT  = "backRight";

    private DcMotor frontLeft, frontRight, backLeft, backRight;

    @Override
    public void runOpMode() {
        // --- Inicializar hardware ---
        frontLeft  = hardwareMap.get(DcMotor.class, NOMBRE_FRONT_LEFT);
        frontRight = hardwareMap.get(DcMotor.class, NOMBRE_FRONT_RIGHT);
        backLeft   = hardwareMap.get(DcMotor.class, NOMBRE_BACK_LEFT);
        backRight  = hardwareMap.get(DcMotor.class, NOMBRE_BACK_RIGHT);

        // Motores del lado izquierdo invertidos (convencion estandar FTC)
        frontLeft.setDirection(DcMotorSimple.Direction.REVERSE);
        backLeft.setDirection(DcMotorSimple.Direction.REVERSE);
        frontRight.setDirection(DcMotorSimple.Direction.FORWARD);
        backRight.setDirection(DcMotorSimple.Direction.FORWARD);

        // Modo de frenado y uso de encoders
        frontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        frontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        frontLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        frontRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        backLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        backRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        frontLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        frontRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        backLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        backRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        telemetry.addData("Estado", "Listo. Pulsa START para avanzar 1 m");
        telemetry.addData("Ticks objetivo", DISTANCIA_METROS * TICKS_POR_METRO);
        telemetry.update();

        waitForStart();
        if (isStopRequested()) return;

        // --- Calcular ticks objetivo ---
        final int ticksObjetivo = (int) Math.round(DISTANCIA_METROS * TICKS_POR_METRO);

        // Resetear encoders justo antes de moverse
        frontLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        frontRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        backLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        backRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);

        frontLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        frontRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        backLeft.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        backRight.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        ElapsedTime timer = new ElapsedTime();
        timer.reset();

        // --- Bucle de control: avanzar hasta alcanzar ticks objetivo ---
        while (opModeIsActive() && timer.milliseconds() < TIMEOUT_MS) {
            int posActual = promedioTicks();
            int restante  = ticksObjetivo - posActual;

            telemetry.addData("Ticks actuales", posActual);
            telemetry.addData("Ticks objetivo", ticksObjetivo);
            telemetry.addData("Restante", restante);
            telemetry.update();

            if (Math.abs(restante) <= TOLERANCIA_TICKS) {
                break;
            }

            // Control proporcional simple para suavizar la llegada
            double potencia = VELOCIDAD;
            if (Math.abs(restante) < 200) {
                potencia = Math.max(0.15, VELOCIDAD * (Math.abs(restante) / 200.0));
            }
            if (restante < 0) potencia = -potencia;

            aplicarPotencia(potencia);
        }

        // --- Detener motores ---
        aplicarPotencia(0.0);

        telemetry.addData("Estado", "Avance completado");
        telemetry.addData("Ticks finales", promedioTicks());
        telemetry.update();
        sleep(500);
    }

    /** Aplica la misma potencia a los 4 motores para avanzar recto. */
    private void aplicarPotencia(double p) {
        frontLeft.setPower(p);
        frontRight.setPower(p);
        backLeft.setPower(p);
        backRight.setPower(p);
    }

    /** Promedio de los ticks de los 4 encoders (mas robusto que uno solo). */
    private int promedioTicks() {
        return (Math.abs(frontLeft.getCurrentPosition())
              + Math.abs(frontRight.getCurrentPosition())
              + Math.abs(backLeft.getCurrentPosition())
              + Math.abs(backRight.getCurrentPosition())) / 4;
    }
}
