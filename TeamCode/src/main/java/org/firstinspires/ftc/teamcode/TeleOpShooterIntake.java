package org.firstinspires.ftc.teamcode;

// ============================================================================
//  TeleOpShooterIntake -- TeleOp Oficial de CompeticiÃ³n BIOBUZZ 24096.
//  Especializado en: Chasis Mecanum + Shooter Encoder PID + Intake.
//
//  Hardware mapeado exactamente al Driver Station:
//      frontLeft, frontRight, backLeft, backRight  (DcMotor, chasis mecanum)
//      intake                                      (DcMotor)
//      shooter                                     (DcMotorEx, encoder de alta velocidad)
//
//  Controles (gamepad1):
//      Stick izquierdo  -> Avanzar / Retroceder y Desplazar (Strafe)
//      Stick derecho X  -> Girar sobre su eje
//      BotÃ³n A          -> Intake adentro (tragar piezas)
//      BotÃ³n B          -> Intake reversa (expulsar)
//      Gatillo Derecho  -> Shooter a velocidad constante PID (BioBuzzConfig)
//      Gatillo Izquierdo-> Detener Shooter
// ============================================================================

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.util.Range;

@TeleOp(name = "TeleOpShooterIntake", group = "BIOBUZZ")
public class TeleOpShooterIntake extends OpMode {

    // ---- Motores del Robot ----
    private DcMotor frontLeft;
    private DcMotor frontRight;
    private DcMotor backLeft;
    private DcMotor backRight;
    private DcMotor intake;
    private DcMotorEx shooter;

    // ---- Constantes Oficiales BioBuzz 24096 ----
    private static final double DEADBAND               = 0.05;
    private static final double INTAKE_POWER           = 0.90;
    private static final double BASE_SHOOTER_VELOCITY  = 1200.0; // Ticks/s constante
    private static final double MAX_SHOOTER_VELOCITY   = 2000.0;
    private static final double TRIGGER_UMBRAL         = 0.05;

    @Override
    public void init() {
        // ---- 1. AsignaciÃ³n de Hardware ----
        frontLeft  = hardwareMap.get(DcMotor.class, "frontLeft");
        frontRight = hardwareMap.get(DcMotor.class, "frontRight");
        backLeft   = hardwareMap.get(DcMotor.class, "backLeft");
        backRight  = hardwareMap.get(DcMotor.class, "backRight");
        intake     = hardwareMap.get(DcMotor.class, "intake");
        shooter    = hardwareMap.get(DcMotorEx.class, "shooter");

        // ---- 2. Sentidos de Giro (ConvenciÃ³n Mecanum BioBuzz) ----
        frontLeft.setDirection(DcMotor.Direction.REVERSE);
        backLeft.setDirection(DcMotor.Direction.REVERSE);
        frontRight.setDirection(DcMotor.Direction.FORWARD);
        backRight.setDirection(DcMotor.Direction.FORWARD);

        // ---- 3. Freno Activo en Chasis ----
        for (DcMotor m : new DcMotor[]{frontLeft, frontRight, backLeft, backRight}) {
            m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }

        // ---- 4. ConfiguraciÃ³n de Intake y Shooter ----
        intake.setDirection(DcMotor.Direction.FORWARD);
        intake.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        shooter.setDirection(DcMotor.Direction.FORWARD);
        shooter.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        shooter.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);

        telemetry.addLine("=== BIOBUZZ 24096 ===");
        telemetry.addLine("Robot listo para competir. Presione START.");
        telemetry.update();
    }

    @Override
    public void loop() {
        // ---- 1. Lectura del Mando y CinemÃ¡tica Mecanum ----
        double y  = -gamepad1.left_stick_y; // Invertir para que arriba sea avanzar
        double x  =  gamepad1.left_stick_x * 1.1; // Compensar fricciÃ³n en strafe
        double rx =  gamepad1.right_stick_x;

        // Zona muerta para proteger contra drift
        if (Math.abs(y)  < DEADBAND) y  = 0.0;
        if (Math.abs(x)  < DEADBAND) x  = 0.0;
        if (Math.abs(rx) < DEADBAND) rx = 0.0;

        // NormalizaciÃ³n exacta de denominador
        double denominator = Math.max(Math.abs(y) + Math.abs(x) + Math.abs(rx), 1.0);
        frontLeft.setPower((y + x + rx) / denominator);
        frontRight.setPower((y - x - rx) / denominator);
        backLeft.setPower((y - x + rx) / denominator);
        backRight.setPower((y + x - rx) / denominator);

        // ---- 2. Control del Intake ----
        if (gamepad1.a) {
            intake.setPower(INTAKE_POWER);  // Tragar elemento
        } else if (gamepad1.b) {
            intake.setPower(-INTAKE_POWER); // Expulsar
        } else {
            intake.setPower(0.0);
        }

        // ---- 3. Control del Shooter PID ----
        if (gamepad1.right_trigger > TRIGGER_UMBRAL) {
            shooter.setVelocity(BASE_SHOOTER_VELOCITY);
        } else if (gamepad1.left_trigger > TRIGGER_UMBRAL) {
            shooter.setPower(0.0); // Freno por flotaciÃ³n para cuidar engranajes
        }

        // ---- 4. TelemetrÃ­a en Vivo para Driver Station ----
        telemetry.addData("Shooter Vel (Ticks/s)", "%.0f", shooter.getVelocity());
        telemetry.addData("Shooter Objetivo", "%.0f", BASE_SHOOTER_VELOCITY);
        telemetry.addData("Intake Potencia", "%.2f", intake.getPower());
        telemetry.update();
    }

    @Override
    public void stop() {
        // Apagado seguro de actuadores
        for (DcMotor m : new DcMotor[]{frontLeft, frontRight, backLeft, backRight, intake}) {
            m.setPower(0.0);
        }
        if (shooter != null) {
            shooter.setVelocity(0.0);
            shooter.setPower(0.0);
        }
    }
}
