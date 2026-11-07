package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

@Autonomous(name = "BioBuzz Autonomo", group = "BioBuzz")
public class biobuzzautonomo extends LinearOpMode {

    private DcMotor frontLeft;
    private DcMotor frontRight;
    private DcMotor backLeft;
    private DcMotor backRight;
    private DcMotor intake;
    private DcMotor transfer;
    private DcMotorEx shooter;

    @Override
    public void runOpMode() {
        frontLeft  = hardwareMap.get(DcMotor.class, "frontLeft");
        frontRight = hardwareMap.get(DcMotor.class, "frontRight");
        backLeft   = hardwareMap.get(DcMotor.class, "backLeft");
        backRight  = hardwareMap.get(DcMotor.class, "backRight");

        try {
            intake = hardwareMap.get(DcMotor.class, "intake");
            intake.setDirection(DcMotorSimple.Direction.FORWARD);
            intake.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        } catch (Exception e) {
            intake = null;
        }

        try {
            transfer = hardwareMap.get(DcMotor.class, "transferMotor");
            transfer.setDirection(DcMotorSimple.Direction.FORWARD);
            transfer.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        } catch (Exception e) {
            transfer = null;
        }

        try {
            shooter = hardwareMap.get(DcMotorEx.class, "shooter");
            shooter.setDirection(DcMotorSimple.Direction.FORWARD);
            shooter.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
            shooter.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        } catch (Exception e) {
            shooter = null;
        }

        frontLeft.setDirection(DcMotorSimple.Direction.REVERSE);
        backLeft.setDirection(DcMotorSimple.Direction.REVERSE);
        frontRight.setDirection(DcMotorSimple.Direction.FORWARD);
        backRight.setDirection(DcMotorSimple.Direction.FORWARD);

        frontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        frontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        telemetry.addData("Estado", "Listo para iniciar autonomo (Tirar + Parquin)");
        telemetry.update();

        waitForStart();
        if (isStopRequested()) return;

        // 1. Posicionarse hacia la zona de tiro
        telemetry.addData("Autonomo", "Paso 1: Moviendose a zona de tiro...");
        telemetry.update();
        setMotorPowers(-0.45);
        sleep(900);
        setMotorPowers(0.0);

        // 2. Encender Shooter, esperar rampa y alimentar la pieza
        telemetry.addData("Autonomo", "Paso 2: Disparando pre-carga...");
        telemetry.update();
        if (shooter != null) {
            shooter.setVelocity(BioBuzzConfig.BASE_SHOOTER_VELOCITY);
            sleep(800); // Esperar rampa de velocidad
            if (intake != null) intake.setPower(BioBuzzConfig.INTAKE_POWER);
            if (transfer != null) transfer.setPower(1.0);
            sleep(900); // Alimentar y lanzar
            if (intake != null) intake.setPower(0.0);
            if (transfer != null) transfer.setPower(0.0);
            shooter.setVelocity(0.0);
        } else {
            sleep(1000);
        }

        // 3. Moverse hacia la zona de parquin (parking)
        telemetry.addData("Autonomo", "Paso 3: Moviendose hacia el parquin...");
        telemetry.update();
        setMotorPowers(0.5);
        sleep(1200);

        // 4. Detener todos los motores
        setMotorPowers(0.0);
        telemetry.addData("Autonomo", "Rutina completada: Tirar + Parquin");
        telemetry.update();
    }

    private void setMotorPowers(double power) {
        frontLeft.setPower(power);
        backLeft.setPower(power);
        frontRight.setPower(power);
        backRight.setPower(power);
    }
}
