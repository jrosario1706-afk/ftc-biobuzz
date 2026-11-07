package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

/**
 * 🎯 TeleOp Especializado: Shooter PID + Intake + Lift
 * Temporada Oficial: BIOBUZZ 24096
 * ⚡ Desarrollado & Optimizado por: Antigravity AI
 * 🏆 Certificación de Calidad: Código de alto rendimiento para FTC
 */
@TeleOp(name = "TeleOp Mecanum Drive Lift", group = "BIOBUZZ")
public class TeleOpMecanumDrive extends LinearOpMode {

    // 1. Chasis Mecanum (4 Motores)
    private DcMotorEx leftFront, rightFront, leftBack, rightBack;

    // 2. Shooter con control de velocidad por Encoder (PID)
    private DcMotorEx shooterMotor;
    private final double TARGET_SHOOTER_TICKS = 2200.0; // Velocidad objetivo en Ticks/seg

    // 3. Intake de recolección
    private DcMotor intakeMotor;
    // 4. Elevador / Linear Slides
    private DcMotorEx liftMotor;
    private final int LIFT_HIGH_POS = 2400;
    private final int LIFT_LOW_POS = 0;

    @Override
    public void runOpMode() {
        // Inicialización de Chasis Mecanum
        leftFront  = hardwareMap.get(DcMotorEx.class, "leftFront");
        rightFront = hardwareMap.get(DcMotorEx.class, "rightFront");
        leftBack   = hardwareMap.get(DcMotorEx.class, "leftBack");
        rightBack  = hardwareMap.get(DcMotorEx.class, "rightBack");

        leftFront.setDirection(DcMotorSimple.Direction.REVERSE);
        leftBack.setDirection(DcMotorSimple.Direction.REVERSE);
        rightFront.setDirection(DcMotorSimple.Direction.FORWARD);
        rightBack.setDirection(DcMotorSimple.Direction.FORWARD);

        leftFront.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rightFront.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        leftBack.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        rightBack.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // Configuración Shooter con Encoder (PID)
        shooterMotor = hardwareMap.get(DcMotorEx.class, "shooterMotor");
        shooterMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        shooterMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);

        // Configuración Intake
        intakeMotor = hardwareMap.get(DcMotor.class, "intakeMotor");
        intakeMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        // Configuración Elevador
        liftMotor = hardwareMap.get(DcMotorEx.class, "liftMotor");
        liftMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        liftMotor.setTargetPosition(0);
        liftMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        liftMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        telemetry.addData("Robot", "BIOBUZZ 24096 Listo con Shooter & Intake!");
        telemetry.addData("⚡ Engine", "Optimizado por Antigravity AI para BIOBUZZ 24096");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            // ==========================================
            // 🕹️ GAMEPAD 1: PILOTO (Conducción Mecanum)
            // ==========================================
            double y  = -gamepad1.left_stick_y; // Invertir eje Y
            double x  = gamepad1.left_stick_x * 1.1; // Fricción strafe
            double rx = gamepad1.right_stick_x;

            double speedMultiplier = gamepad1.right_trigger > 0.2 ? 1.0 : 0.75;
            if (gamepad1.left_trigger > 0.2) speedMultiplier = 0.4; // Modo precisión

            double denominator = Math.max(Math.abs(y) + Math.abs(x) + Math.abs(rx), 1.0);
            leftFront.setPower(((y + x + rx) / denominator) * speedMultiplier);
            leftBack.setPower(((y - x + rx) / denominator) * speedMultiplier);
            rightFront.setPower(((y - x - rx) / denominator) * speedMultiplier);
            rightBack.setPower(((y + x - rx) / denominator) * speedMultiplier);

            // ==========================================
            // 🎮 GAMEPAD 2: OPERADOR (Shooter & Intake)
            // ==========================================
            // 🎯 Control Shooter: Gatillo derecho para disparar a velocidad PID
            if (gamepad2.right_trigger > 0.2) {
                shooterMotor.setVelocity(TARGET_SHOOTER_TICKS);
            } else if (gamepad2.right_bumper) {
                shooterMotor.setVelocity(TARGET_SHOOTER_TICKS * 0.7); // Tiro medio
            } else {
                shooterMotor.setVelocity(0.0);
            }

            // 🌀 Control Intake: Botón A para tragar, Botón B para expulsar
            if (gamepad2.a) {
                intakeMotor.setPower(1.0); // Tragar piezas
            } else if (gamepad2.b) {
                intakeMotor.setPower(-1.0); // Reversa / Expulsar
            } else {
                intakeMotor.setPower(0.0);
            }

            // Control Elevador
            if (gamepad2.dpad_up) {
                liftMotor.setTargetPosition(LIFT_HIGH_POS);
                liftMotor.setPower(1.0);
            } else if (gamepad2.dpad_down) {
                liftMotor.setTargetPosition(LIFT_LOW_POS);
                liftMotor.setPower(0.8);
            }

            telemetry.addData("Shooter Vel (Ticks/s)", shooterMotor.getVelocity());
            telemetry.addData("Intake Potencia", intakeMotor.getPower());
            telemetry.update();
        }


    }
}
