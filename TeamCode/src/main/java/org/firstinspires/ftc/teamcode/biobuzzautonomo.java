package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorSimple;

@Autonomous(name = "BioBuzz Autonomo", group = "BioBuzz")
public class biobuzzautonomo extends LinearOpMode {

    private DcMotor frontLeft;
    private DcMotor frontRight;
    private DcMotor backLeft;
    private DcMotor backRight;

    @Override
    public void runOpMode() {
        // Inicializar motores del chasis
        frontLeft = hardwareMap.get(DcMotor.class, "frontLeft");
        frontRight = hardwareMap.get(DcMotor.class, "frontRight");
        backLeft = hardwareMap.get(DcMotor.class, "backLeft");
        backRight = hardwareMap.get(DcMotor.class, "backRight");

        // Configurar direcciones de los motores (coherente con TeleOp)
        frontLeft.setDirection(DcMotorSimple.Direction.REVERSE);
        backLeft.setDirection(DcMotorSimple.Direction.REVERSE);
        frontRight.setDirection(DcMotorSimple.Direction.FORWARD);
        backRight.setDirection(DcMotorSimple.Direction.FORWARD);

        // Configurar comportamiento en freno
        frontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        frontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);

        telemetry.addData("Estado", "Listo para iniciar autónomo");
        telemetry.update();

        waitForStart();

        if (isStopRequested()) return;

        // 1. Moverse hacia atrás durante 1 segundo (potencia -0.5)
        telemetry.addData("Autónomo", "Moviéndose hacia atrás (1s)...");
        telemetry.update();
        setMotorPowers(-0.5);
        sleep(900);

        // 2. Moverse hacia adelante durante 2 segundos (potencia 0.5)
        telemetry.addData("Autónomo", "Moviéndose hacia adelante (2s)...");
        telemetry.update();
        setMotorPowers(0.5);
        sleep(500);

        // 3. Detener todos los motores
        setMotorPowers(0.0);
        telemetry.addData("Autónomo", "Rutina finalizada con éxito");
        telemetry.update();
    }

    private void setMotorPowers(double power) {
        frontLeft.setPower(power);
        backLeft.setPower(power);
        frontRight.setPower(power);
        backRight.setPower(power);
    }
}
