package org.firstinspires.ftc.teamcode.autonomous;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.util.ElapsedTime;

import org.firstinspires.ftc.teamcode.hardware.RobotConfig;
import org.firstinspires.ftc.teamcode.hardware.RobotHardware;

/**
 * Base para autonomos SIMPLES por tiempo (no necesitan odometria calibrada).
 * Las subclases solo escriben la secuencia en {@link #secuencia()}.
 *
 * Cada accion devuelve false si se presiono STOP, para salir de inmediato:
 *     if (!avanzar(0.45, 1.0)) return;
 */
public abstract class TimedAutoBase extends LinearOpMode {

    protected RobotHardware robot;
    private final ElapsedTime timer = new ElapsedTime();

    /** Nombre que se muestra en la telemetria. */
    protected abstract String nombre();

    /** Pasos del autonomo. */
    protected abstract void secuencia();

    @Override
    public final void runOpMode() {
        try {
            robot = new RobotHardware(hardwareMap, false);

            telemetry.addLine("=== BIOBUZZ 24096 ===");
            telemetry.addLine(nombre());
            robot.reportTo(telemetry);
            telemetry.addLine("Alinee el robot y presione START");
            telemetry.update();

            waitForStart();
            if (isStopRequested()) return;

            secuencia();

            if (opModeIsActive()) {
                telemetry.addLine("=== AUTONOMO COMPLETADO ===");
                telemetry.update();
            }
        } catch (RuntimeException e) {
            telemetry.addData("ERROR", e.getMessage());
            telemetry.update();
            while (!isStopRequested()) idle();
        } finally {
            if (robot != null) robot.shutdown();
        }
    }

    // =========================================================
    // ACCIONES
    // =========================================================

    protected void paso(String descripcion) {
        telemetry.addLine("=== EJECUTANDO ===");
        telemetry.addLine(descripcion);
        telemetry.update();
    }

    protected boolean avanzar(double potencia, double segundos) {
        return mover(potencia, 0, 0, segundos);
    }

    protected boolean retroceder(double potencia, double segundos) {
        return mover(-potencia, 0, 0, segundos);
    }

    protected boolean strafeDerecha(double potencia, double segundos) {
        return mover(0, potencia, 0, segundos);
    }

    protected boolean strafeIzquierda(double potencia, double segundos) {
        return mover(0, -potencia, 0, segundos);
    }

    protected boolean girarDerecha(double potencia, double segundos) {
        return mover(0, 0, potencia, segundos);
    }

    protected boolean girarIzquierda(double potencia, double segundos) {
        return mover(0, 0, -potencia, segundos);
    }

    /** Avanza con el intake encendido para recoger piezas. */
    protected boolean recogerAvanzando(double potencia, double segundos) {
        robot.setIntakePower(RobotConfig.INTAKE_POWER);
        try {
            return mover(potencia, 0, 0, segundos);
        } finally {
            robot.setIntakePower(0);
        }
    }

    /**
     * Dispara: espera a que el shooter llegue a velocidad (maximo
     * {@code maxSpinUpSeg}) y LUEGO empuja las piezas durante {@code feedSeg}.
     * Antes solo se encendia el shooter y nunca se alimentaba la pieza.
     */
    protected boolean disparar(double velocidad, double maxSpinUpSeg, double feedSeg) {
        if (robot.shooter == null) {
            telemetry.addLine("AVISO: shooter no detectado, se omite el disparo");
            telemetry.update();
            return opModeIsActive();
        }
        try {
            robot.setShooterVelocity(velocidad);
            timer.reset();
            while (opModeIsActive() && timer.seconds() < maxSpinUpSeg
                    && !robot.isShooterReady(velocidad)) {
                idle();
            }
            robot.feed(true);
            return esperar(feedSeg);
        } finally {
            robot.feed(false);
            robot.setShooterVelocity(0);
        }
    }

    protected boolean esperar(double segundos) {
        timer.reset();
        while (opModeIsActive() && timer.seconds() < segundos) {
            idle();
        }
        return opModeIsActive();
    }

    private boolean mover(double adelante, double derecha, double giro, double segundos) {
        if (!opModeIsActive()) return false;
        try {
            robot.drive(adelante, derecha, giro, 1.0);
            return esperar(segundos);
        } finally {
            robot.stopDrive();
        }
    }
}
