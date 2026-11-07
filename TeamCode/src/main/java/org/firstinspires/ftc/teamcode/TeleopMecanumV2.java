package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

/**
 * TeleOp Mecanum V2.
 *
 * Usa EXCLUSIVAMENTE el hardware real del robot:
 *   frontLeft, frontRight, backLeft, backRight  (DcMotor, chasis mecanum)
 *   intake                                      (DcMotor)
 *   shooter                                     (DcMotorEx)
 *
 * Controles (gamepad1):
 *   Stick izquierdo  -> avanzar/retroceder y desplazar
 *   Stick derecho X  -> girar
 *   Bumper derecho   -> intake entra
 *   Bumper izquierdo -> intake sale
 *   Gatillo derecho  -> shooter (potencia proporcional al gatillo)
 *
 * Convencion de ejes (coherente con HardwareBioBuzz y CONFIGURACION-HARDWARE.md):
 *   +Y = avanzar, +X = desplazar a la derecha, +giro = horario.
 */
@TeleOp(name = "Teleop Mecanum V2", group = "Drive")
public class TeleopMecanumV2 extends LinearOpMode {

    // ---- Constantes (coherentes con BioBuzzConfig) ----
    private static final double DEADBAND      = 0.05;
    private static final double INTAKE_POWER  = 0.90;
    private static final double TRIGGER_UMBRAL = 0.05;

    // ---- Hardware ----
    private DcMotor frontLeft;
    private DcMotor frontRight;
    private DcMotor backLeft;
    private DcMotor backRight;
    private DcMotor intake;
    private DcMotorEx shooter;

    @Override
    public void runOpMode() {
        // ---- 1) Obtener hardware real (nombres del Driver Station) ----
        frontLeft  = hardwareMap.get(DcMotor.class,   "frontLeft");
        frontRight = hardwareMap.get(DcMotor.class,   "frontRight");
        backLeft   = hardwareMap.get(DcMotor.class,   "backLeft");
        backRight  = hardwareMap.get(DcMotor.class,   "backRight");
        intake     = hardwareMap.get(DcMotor.class,   "intake");
        shooter    = hardwareMap.get(DcMotorEx.class, "shooter");

        // ---- 2) Direcciones del chasis (segun CONFIGURACION-HARDWARE.md) ----
        frontLeft.setDirection(DcMotorSimple.Direction.REVERSE);
        backLeft.setDirection(DcMotorSimple.Direction.REVERSE);
        frontRight.setDirection(DcMotorSimple.Direction.FORWARD);
        backRight.setDirection(DcMotorSimple.Direction.FORWARD);

        // ---- 3) Comportamiento en parada ----
        // BRAKE solo en el chasis; intake BRAKE; shooter FLOAT para que no frene en seco.
        frontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        frontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        intake.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        shooter.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);

        // ---- 4) Telemetria inicial y espera de START ----
        telemetry.addLine("Teleop Mecanum V2 listo");
        telemetry.addLine("Stick izq: mover | Stick der X: girar");
        telemetry.addLine("Bumpers: intake | Gatillo derecho: shooter");
        telemetry.update();
        waitForStart();

        // ---- 5) Bucle principal ----
        while (opModeIsActive()) {

            // --- 5.1) Lectura de sticks con deadband ---
            double axial   = aplicarDeadband(-gamepad1.left_stick_y); // + = avanzar
            double lateral = aplicarDeadband( gamepad1.left_stick_x); // + = derecha
            double yaw     = aplicarDeadband( gamepad1.right_stick_x); // + = horario

            // --- 5.2) Cinematica mecanum ---
            double fl = axial + lateral + yaw;
            double fr = axial - lateral - yaw;
            double bl = axial - lateral + yaw;
            double br = axial + lateral - yaw;

            // --- 5.3) Normalizacion por el maximo (preserva la direccion) ---
            double max = Math.max(
                    Math.max(Math.abs(fl), Math.abs(fr)),
                    Math.max(Math.abs(bl), Math.abs(br)));
            if (max > 1.0) {
                fl /= max;
                fr /= max;
                bl /= max;
                br /= max;
            }

            // --- 5.4) Aplicar potencia a los motores ---
            frontLeft.setPower(fl);
            frontRight.setPower(fr);
            backLeft.setPower(bl);
            backRight.setPower(br);

            // --- 5.5) Intake con bumpers ---
            if (gamepad1.right_bumper) {
                intake.setPower(INTAKE_POWER);   // entra
            } else if (gamepad1.left_bumper) {
                intake.setPower(-INTAKE_POWER);  // sale
            } else {
                intake.setPower(0.0);
            }

            // --- 5.6) Shooter con gatillo derecho (potencia proporcional) ---
            double trigger = gamepad1.right_trigger;
            if (trigger > TRIGGER_UMBRAL) {
                shooter.setPower(trigger);
            } else {
                shooter.setPower(0.0);
            }

            // --- 5.7) Telemetria ---
            telemetry.addData("FL/FR/BL/BR", "%.2f %.2f %.2f %.2f", fl, fr, bl, br);
            telemetry.addData("Intake", "%.2f", intake.getPower());
            telemetry.addData("Shooter", "%.2f", shooter.getPower());
            telemetry.update();
        }

        // ---- 6) Parada segura al salir ----
        frontLeft.setPower(0);
        frontRight.setPower(0);
        backLeft.setPower(0);
        backRight.setPower(0);
        intake.setPower(0);
        shooter.setPower(0);
    }

    /** Aplica la zona muerta a un eje del stick. */
    private double aplicarDeadband(double v) {
        return Math.abs(v) < DEADBAND ? 0.0 : v;
    }
}
