package org.firstinspires.ftc.teamcode;

// ============================================================================
//  TeleOpShooter -- TeleOp simple de chasis mecanum + intake + shooter.
//
//  Usa EXCLUSIVAMENTE el hardware real del robot (memoria del equipo):
//      frontLeft, frontRight, backLeft, backRight  (DcMotor, chasis mecanum)
//      intake                                      (DcMotor)
//      shooter                                     (DcMotorEx, con encoder)
//
//  NOTA: el servo 'feeder' NO existe en la configuracion de hardware del
//  equipo, por eso se ha eliminado. Si se anade fisicamente, hay que darlo
//  de alta en el Driver Station y en HardwareBioBuzz antes de usarlo aqui.
//
//  Controles (gamepad1):
//      Stick izquierdo  -> avanzar/retroceder y desplazar
//      Stick derecho X  -> girar
//      A                -> intake entra
//      B                -> intake sale
//      Gatillo derecho  -> shooter a velocidad base
//      Gatillo izquierdo-> parar shooter
// ============================================================================

import com.qualcomm.robotcore.eventloop.opmode.OpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;

@TeleOp(name = "TeleOp Shooter Intake", group = "Drive")
public class TeleOpShooter extends OpMode {

    // ---- Hardware ----
    private DcMotor frontLeft;
    private DcMotor frontRight;
    private DcMotor backLeft;
    private DcMotor backRight;
    private DcMotor intake;
    private DcMotorEx shooter;

    // ---- Constantes locales (coherentes con BioBuzzConfig) ----
    private static final double DEADBAND      = BioBuzzConfig.DEADBAND;
    private static final double INTAKE_POWER  = BioBuzzConfig.INTAKE_POWER;
    private static final double TRIGGER_UMBRAL = 0.05;

    @Override
    public void init() {
        // ---- Obtener hardware con los nombres reales del Driver Station ----
        frontLeft  = hardwareMap.get(DcMotor.class, "frontLeft");
        frontRight = hardwareMap.get(DcMotor.class, "frontRight");
        backLeft   = hardwareMap.get(DcMotor.class, "backLeft");
        backRight  = hardwareMap.get(DcMotor.class, "backRight");
        intake     = hardwareMap.get(DcMotor.class, "intake");
        shooter    = hardwareMap.get(DcMotorEx.class, "shooter");

        // ---- Direcciones del chasis (misma convencion que TeleopMecanumV2) ----
        frontLeft.setDirection(DcMotor.Direction.REVERSE);
        backLeft.setDirection(DcMotor.Direction.REVERSE);
        frontRight.setDirection(DcMotor.Direction.FORWARD);
        backRight.setDirection(DcMotor.Direction.FORWARD);

        // ---- Freno en el chasis (coherente con el resto de OpModes) ----
        for (DcMotor m : new DcMotor[]{frontLeft, frontRight, backLeft, backRight}) {
            m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        }

        // ---- Intake y shooter ----
        intake.setDirection(DcMotor.Direction.FORWARD);
        intake.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        shooter.setDirection(DcMotor.Direction.FORWARD);
        shooter.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        shooter.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);

        telemetry.addLine("TeleOp Shooter Intake listo");
        telemetry.update();
    }

    @Override
    public void loop() {
        // ---- Chasis mecanum (4 motores) ----
        double y  = -gamepad1.left_stick_y;
        double x  =  gamepad1.left_stick_x;
        double rx =  gamepad1.right_stick_x;

        // Deadband para evitar giros con sticks sueltos
        if (Math.abs(y)  < DEADBAND) y  = 0.0;
        if (Math.abs(x)  < DEADBAND) x  = 0.0;
        if (Math.abs(rx) < DEADBAND) rx = 0.0;

        frontLeft.setPower(y + x + rx);
        frontRight.setPower(y - x - rx);
        backLeft.setPower(y - x + rx);
        backRight.setPower(y + x - rx);

        // ---- Intake: A adelante, B reversa ----
        if (gamepad1.a) {
            intake.setPower(INTAKE_POWER);
        } else if (gamepad1.b) {
            intake.setPower(-INTAKE_POWER);
        } else {
            intake.setPower(0.0);
        }

        // ---- Shooter: gatillo derecho acelera, izquierdo para ----
        if (gamepad1.right_trigger > TRIGGER_UMBRAL) {
            shooter.setVelocity(BioBuzzConfig.BASE_SHOOTER_VELOCITY);
        } else if (gamepad1.left_trigger > TRIGGER_UMBRAL) {
            // setPower(0) detiene de verdad; setVelocity(0) deja el lazo cerrado
            shooter.setPower(0.0);
        }

        // ---- Telemetria ----
        telemetry.addData("Shooter vel", shooter.getVelocity());
        telemetry.addData("Objetivo", BioBuzzConfig.BASE_SHOOTER_VELOCITY);
        telemetry.addData("Intake", intake.getPower());
        telemetry.update();
    }
}