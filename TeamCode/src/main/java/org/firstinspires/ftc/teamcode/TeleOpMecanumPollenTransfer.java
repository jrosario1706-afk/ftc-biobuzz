package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.eventloop.opmode.TeleOp;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.util.Range;

/**
 * Driver-controlled mecanum robot with intake, shooter, and pollen transfer system.
 *
 * The feeder servo transfers (pushes) the pollen from the intake into the shooter.
 *
 * Robot Configuration names:
 * frontLeft, frontRight, backLeft, backRight, intakeMotor, shooterMotor
 * Optional: feederServo
 */
@TeleOp(name = "TeleOp Pollen Transfer", group = "Competition")
public class TeleOpMecanumPollenTransfer extends LinearOpMode {

    private DcMotor frontLeft;
    private DcMotor frontRight;
    private DcMotor backLeft;
    private DcMotor backRight;
    private DcMotor intakeMotor;
    private DcMotor shooterMotor;
    private Servo feederServo;

    private static final double INTAKE_POWER = 1.0;
    private static final double SHOOTER_POWER = 1.0;
    private static final double SLOW_MODE_MULTIPLIER = 0.45;
    // Tune these only if your robot has a feeder servo.
    private static final double FEEDER_HOME_POSITION = 0.10;
    private static final double FEEDER_FIRE_POSITION = 0.75;

    @Override
    public void runOpMode() {
        frontLeft = hardwareMap.get(DcMotor.class, "frontLeft");
        frontRight = hardwareMap.get(DcMotor.class, "frontRight");
        backLeft = hardwareMap.get(DcMotor.class, "backLeft");
        backRight = hardwareMap.get(DcMotor.class, "backRight");
        intakeMotor = hardwareMap.get(DcMotor.class, "intakeMotor");
        shooterMotor = hardwareMap.get(DcMotor.class, "shooterMotor");
        // The feeder is optional: the robot still works without it.
        feederServo = hardwareMap.tryGet(Servo.class, "feederServo");

        // Reverse the left motors if forward joystick input drives the robot backward.
        frontLeft.setDirection(DcMotor.Direction.REVERSE);
        backLeft.setDirection(DcMotor.Direction.REVERSE);

        frontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        frontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        intakeMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        shooterMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        if (feederServo != null) {
            feederServo.setPosition(FEEDER_HOME_POSITION);
        }

        telemetry.addLine("Ready");
        telemetry.addLine("Driver 1: sticks = drive; left bumper = slow mode");
        telemetry.addLine("Driver 2: right trigger = intake; left trigger = reverse intake");
        telemetry.addLine("Driver 2: right bumper = shooter; A = transfer pollen to shooter");
        telemetry.addData("Feeder servo", feederServo == null ? "not installed" : "ready");
        telemetry.update();

        waitForStart();

        while (opModeIsActive()) {
            // Mecanum drive: forward/back, strafe, and turn.
            double drive = -gamepad1.left_stick_y;
            double strafe = gamepad1.left_stick_x;
            double turn = gamepad1.right_stick_x;

            double frontLeftPower = drive + strafe + turn;
            double frontRightPower = drive - strafe - turn;
            double backLeftPower = drive - strafe + turn;
            double backRightPower = drive + strafe - turn;

            // Preserve direction while keeping all motor powers in range.
            double max = Math.max(1.0, Math.max(Math.abs(frontLeftPower),
                    Math.max(Math.abs(frontRightPower), Math.max(Math.abs(backLeftPower),
                            Math.abs(backRightPower)))));
            frontLeftPower /= max;
            frontRightPower /= max;
            backLeftPower /= max;
            backRightPower /= max;

            if (gamepad1.left_bumper) {
                frontLeftPower *= SLOW_MODE_MULTIPLIER;
                frontRightPower *= SLOW_MODE_MULTIPLIER;
                backLeftPower *= SLOW_MODE_MULTIPLIER;
                backRightPower *= SLOW_MODE_MULTIPLIER;
            }

            frontLeft.setPower(frontLeftPower);
            frontRight.setPower(frontRightPower);
            backLeft.setPower(backLeftPower);
            backRight.setPower(backRightPower);

            // Intake: right trigger brings pollen in; left trigger ejects it.
            double intakePower = gamepad2.right_trigger - gamepad2.left_trigger;
            intakeMotor.setPower(Range.clip(intakePower, -1.0, 1.0) * INTAKE_POWER);

            // Hold the right bumper to run the shooter.
            shooterMotor.setPower(gamepad2.right_bumper ? SHOOTER_POWER : 0.0);

            // Optional feeder transfer: hold A while the shooter is on to push pollen into the shooter.
            if (feederServo != null) {
                boolean transfer = gamepad2.right_bumper && gamepad2.a;
                feederServo.setPosition(transfer ? FEEDER_FIRE_POSITION : FEEDER_HOME_POSITION);
            }

            telemetry.addData("Intake", "%.0f%%", intakeMotor.getPower() * 100);
            telemetry.addData("Shooter", gamepad2.right_bumper ? "ON" : "OFF");
            telemetry.addData("Transfer", (gamepad2.right_bumper && gamepad2.a) ? "FEEDING" : "IDLE");
            telemetry.addData("Slow mode", gamepad1.left_bumper ? "ON" : "OFF");
            telemetry.update();
        }

        stopAllMotors();
    }


    private void stopAllMotors() {
        if (frontLeft != null) frontLeft.setPower(0);
        if (frontRight != null) frontRight.setPower(0);
        if (backLeft != null) backLeft.setPower(0);
        if (backRight != null) backRight.setPower(0);
        if (intakeMotor != null) intakeMotor.setPower(0);
        if (shooterMotor != null) shooterMotor.setPower(0);
        if (feederServo != null) feederServo.setPosition(FEEDER_HOME_POSITION);
    }
}