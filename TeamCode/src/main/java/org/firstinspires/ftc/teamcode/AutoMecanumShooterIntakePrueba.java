package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;

@Autonomous(name = "Auto Mecanum Shooter Intake Prueba", group = "Competition")
public class AutoMecanumShooterIntakePrueba extends LinearOpMode {

    // =====================================================
    // MECANUM
    // =====================================================

    private DcMotor frontLeft;
    private DcMotor frontRight;
    private DcMotor backLeft;
    private DcMotor backRight;

    // =====================================================
    // SHOOTER
    // =====================================================

    private DcMotor shooterMotor;

    // =====================================================
    // INTAKE
    // =====================================================

    private DcMotor intakeMotor;

    // =====================================================
    // TRANSFERENCIA DE POLEN (FEEDER)
    // =====================================================

    private Servo feederServo;

    // =====================================================
    // CONFIGURACIÓN
    // =====================================================

    private static final double DRIVE_POWER = 0.5;
    private static final double SHOOTER_POWER = 1.0;
    private static final double INTAKE_POWER = 1.0;
    private static final double FEEDER_HOME_POSITION = 0.10;
    private static final double FEEDER_FIRE_POSITION = 0.75;
    private static final int FEED_TRANSFER_MS = 800;


    @Override
    public void runOpMode() throws InterruptedException {

        // =================================================
        // 1. HARDWARE MAP
        // =================================================

        frontLeft = hardwareMap.get(
                DcMotor.class, "frontLeft");

        frontRight = hardwareMap.get(
                DcMotor.class, "frontRight");

        backLeft = hardwareMap.get(
                DcMotor.class, "backLeft");

        backRight = hardwareMap.get(
                DcMotor.class, "backRight");

        shooterMotor = hardwareMap.get(
                DcMotor.class, "shooterMotor");

        intakeMotor = hardwareMap.get(
                DcMotor.class, "intakeMotor");

        // El feeder (servo de transferencia) es opcional pero recomendado.
        feederServo = hardwareMap.tryGet(
                Servo.class, "feederServo");


        // =================================================
        // 2. DIRECCIONES
        // =================================================

        frontLeft.setDirection(
                DcMotor.Direction.REVERSE);

        backLeft.setDirection(
                DcMotor.Direction.REVERSE);

        // Si el intake gira al revés durante la prueba,
        // cambia su dirección a REVERSE.
        intakeMotor.setDirection(
                DcMotor.Direction.FORWARD);


        // =================================================
        // 3. BRAKE
        // =================================================

        frontLeft.setZeroPowerBehavior(
                DcMotor.ZeroPowerBehavior.BRAKE);

        frontRight.setZeroPowerBehavior(
                DcMotor.ZeroPowerBehavior.BRAKE);

        backLeft.setZeroPowerBehavior(
                DcMotor.ZeroPowerBehavior.BRAKE);

        backRight.setZeroPowerBehavior(
                DcMotor.ZeroPowerBehavior.BRAKE);

        // =================================================
        // 4. SHOOTER
        // =================================================

        shooterMotor.setZeroPowerBehavior(
                DcMotor.ZeroPowerBehavior.FLOAT);

        shooterMotor.setPower(0);


        // =================================================
        // 5. INTAKE
        // =================================================

        intakeMotor.setZeroPowerBehavior(
                DcMotor.ZeroPowerBehavior.BRAKE);

        intakeMotor.setPower(0);

        // Poner el feeder en posición de reposo (no bloquea el intake).
        if (feederServo != null) {
            feederServo.setPosition(FEEDER_HOME_POSITION);
        }


        // =================================================
        // ROBOT LISTO
        // =================================================

        telemetry.addLine("==============================");
        telemetry.addLine(" MECANUM + SHOOTER + INTAKE");
        telemetry.addLine("==============================");
        telemetry.addLine("");
        telemetry.addLine("Sin garra ni brazos");
        telemetry.addLine("Intake preparado");
        telemetry.addLine("Shooter preparado");
        telemetry.addData("Transferencia de polen",
                feederServo == null ? "no instalada" : "feeder listo");
        telemetry.addLine("");
        telemetry.addLine("Presiona PLAY");
        telemetry.update();


        // =================================================
        // 6. START
        // =================================================

        waitForStart();


        // =================================================
        // 7. AUTONOMOUS
        // =================================================

        if (opModeIsActive()) {

            // ---------------------------------------------
            // AVANZAR HACIA LA PIEZA
            // ---------------------------------------------

            telemetry.addLine("Avanzando hacia la pieza...");
            telemetry.update();

            driveForward(DRIVE_POWER);

            sleep(1500);

            stopDrive();


            // ---------------------------------------------
            // ACTIVAR INTAKE
            // ---------------------------------------------

            telemetry.addLine("INTAKE ACTIVADO");
            telemetry.update();

            intakeMotor.setPower(INTAKE_POWER);

            sleep(1500);


            // ---------------------------------------------
            // DETENER INTAKE
            // ---------------------------------------------

            intakeMotor.setPower(0);

            sleep(500);


            // ---------------------------------------------
            // PREPARAR SHOOTER
            // ---------------------------------------------

            telemetry.addLine("Preparando shooter...");
            telemetry.update();

            shooterMotor.setPower(SHOOTER_POWER);

            sleep(2000);


            // ---------------------------------------------
            // TRANSFERIR POLEN AL SHOOTER (FEEDER)
            // ---------------------------------------------

            if (feederServo != null) {
                telemetry.addLine("TRANSFIRIENDO POLEN AL SHOOTER...");
                telemetry.update();

                feederServo.setPosition(FEEDER_FIRE_POSITION);
                sleep(FEED_TRANSFER_MS);
                feederServo.setPosition(FEEDER_HOME_POSITION);
                sleep(300);
            }


            // ---------------------------------------------
            // DISPARAR
            // ---------------------------------------------

            telemetry.addLine("DISPARANDO");
            telemetry.update();

            sleep(1500);


            // ---------------------------------------------
            // DETENER SHOOTER
            // ---------------------------------------------

            shooterMotor.setPower(0);

            sleep(500);


            // ---------------------------------------------
            // RETROCEDER
            // ---------------------------------------------

            telemetry.addLine("Retrocediendo...");
            telemetry.update();

            driveBackward(DRIVE_POWER);

            sleep(1000);

            stopDrive();


            // ---------------------------------------------
            // FIN
            // ---------------------------------------------

            telemetry.addLine("AUTO TERMINADO");
            telemetry.update();
        }


        // =================================================
        // SEGURIDAD
        // =================================================

        stopDrive();

        shooterMotor.setPower(0);

        intakeMotor.setPower(0);

        if (feederServo != null) {
            feederServo.setPosition(FEEDER_HOME_POSITION);
        }
    }


    // =====================================================
    // AVANZAR
    // =====================================================

    private void driveForward(double power) {

        frontLeft.setPower(power);
        frontRight.setPower(power);
        backLeft.setPower(power);
        backRight.setPower(power);
    }


    // =====================================================
    // RETROCEDER
    // =====================================================

    private void driveBackward(double power) {

        frontLeft.setPower(-power);
        frontRight.setPower(-power);
        backLeft.setPower(-power);
        backRight.setPower(-power);
    }


    // =====================================================
    // DETENER DRIVETRAIN
    // =====================================================

    private void stopDrive() {

        frontLeft.setPower(0);
        frontRight.setPower(0);
        backLeft.setPower(0);
        backRight.setPower(0);
    }
}