package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

/**
 * TeleOp Mecanum funcional para el chasis del equipo.
 *
 * Usa EXCLUSIVAMENTE los nombres de hardwareMap reales del proyecto:
 *   frontLeft, frontRight, backLeft, backRight  (DcMotor, chasis mecanum)
 *   intake  (DcMotor)
 *   shooter (DcMotorEx, con encoder)
 *
 * No depende de ninguna clase externa: todo esta aqui y compila con el FTC SDK.
 */
@TeleOp(name = "Teleop Mecanum", group = "Drive")
public class TeleopMecanum extends LinearOpMode {

    private DcMotor frontLeft;
    private DcMotor frontRight;
    private DcMotor backLeft;
    private DcMotor backRight;
    private DcMotor intake;
    private DcMotorEx shooter;

    @Override
    public void runOpMode() {
        // ---- Obtener el hardware real (nombres del Driver Station) ----
        frontLeft  = hardwareMap.get(DcMotor.class, "frontLeft");
        frontRight = hardwareMap.get(DcMotor.class, "frontRight");
        backLeft   = hardwareMap.get(DcMotor.class, "backLeft");
        backRight  = hardwareMap.get(DcMotor.class, "backRight");
        intake     = hardwareMap.get(DcMotor.class, "intake");
        shooter    = hardwareMap.get(DcMotorEx.class, "shooter");

        // ---- Direcciones del chasis (igual que el resto del proyecto) ----
        frontLeft.setDirection(DcMotorSimple.Direction.REVERSE);
        backLeft.setDirection(DcMotorSimple.Direction.REVERSE);
        frontRight.setDirection(DcMotorSimple.Direction.FORWARD);
        backRight.setDirection(DcMotorSimple.Direction.FORWARD);

        // ---- Frenado en seco y modo sin encoder para el chasis ----
        for (DcMotor m : new DcMotor[]{frontLeft, frontRight, backLeft, backRight}) {
            m.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            m.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }

        // ---- Intake ----
        intake.setDirection(DcMotorSimple.Direction.FORWARD);
        intake.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        intake.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);

        // ---- Shooter (DcMotorEx con encoder, control por velocidad) ----
        shooter.setDirection(DcMotorSimple.Direction.FORWARD);
        shooter.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        shooter.setMode(DcMotor.RunMode.RUN_USING_ENCODER);

        telemetry.addLine("Listo. Presiona START.");
        telemetry.update();
        waitForStart();

        while (opModeIsActive()) {
            // ---- CHASIS MECANUM (cinematica inversa) ----
            double y  = -gamepad1.left_stick_y;   // adelante / atras
            double x  =  gamepad1.left_stick_x;   // strafe lateral
            double rx =  gamepad1.right_stick_x;  // rotacion

            double fl = y + x + rx;
            double bl = y - x + rx;
            double fr = y - x - rx;
            double br = y + x - rx;

            // Normaliza para no pasar de 1.0 y que no se pierda la direccion
            double max = Math.max(Math.abs(fl),
                        Math.max(Math.abs(bl),
                        Math.max(Math.abs(fr), Math.abs(br))));
            if (max > 1.0) {
                fl /= max; bl /= max; fr /= max; br /= max;
            }

            frontLeft.setPower(fl);
            backLeft.setPower(bl);
            frontRight.setPower(fr);
            backRight.setPower(br);

            // ---- INTAKE ----
            if (gamepad1.right_bumper) {
                intake.setPower(1.0);
            } else if (gamepad1.left_bumper) {
                intake.setPower(-1.0);
            } else {
                intake.setPower(0.0);
            }

            // ---- SHOOTER (velocidad proporcional al gatillo) ----
            if (gamepad1.right_trigger > 0.1) {
                shooter.setVelocity(gamepad1.right_trigger * 3000.0);
            } else {
                shooter.setVelocity(0.0);
            }

            telemetry.addData("Chasis", "y=%.2f x=%.2f giro=%.2f", y, x, rx);
            telemetry.addData("Shooter", "%.0f ticks/s", shooter.getVelocity());
            telemetry.update();
        }

        // ---- Parada segura ----
        frontLeft.setPower(0.0);
        frontRight.setPower(0.0);
        backLeft.setPower(0.0);
        backRight.setPower(0.0);
        intake.setPower(0.0);
        shooter.setVelocity(0.0);
    }
}
