package org.firstinspires.ftc.teamcode;

import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;

@Autonomous(
        name = "BIOBUZZ AI AUTO V1",
        group = "BIOBUZZ 24096"
)
public class BioBuzzAIAuto extends LinearOpMode {

    private DcMotor frontLeft;
    private DcMotor frontRight;
    private DcMotor backLeft;
    private DcMotor backRight;

    private DcMotor intakeMotor;
    private DcMotor shooterMotor;

    private BioBuzzAI ai;

    private static final double SEARCH_SPEED = 0.20;

    private static final double ALIGN_SPEED = 0.18;

    private static final double DRIVE_SPEED = 0.25;

    private static final double CENTER_X = 320;

    private static final double TOLERANCE = 25;

    private static final double MIN_CONFIDENCE = 75;

    @Override
    public void runOpMode() {

        // =====================================
        // HARDWARE
        // =====================================

        frontLeft =
                hardwareMap.get(
                        DcMotor.class,
                        "frontLeft"
                );

        frontRight =
                hardwareMap.get(
                        DcMotor.class,
                        "frontRight"
                );

        backLeft =
                hardwareMap.get(
                        DcMotor.class,
                        "backLeft"
                );

        backRight =
                hardwareMap.get(
                        DcMotor.class,
                        "backRight"
                );

        intakeMotor =
                hardwareMap.get(
                        DcMotor.class,
                        "intakeMotor"
                );

        shooterMotor =
                hardwareMap.get(
                        DcMotor.class,
                        "shooterMotor"
                );

        // =====================================
        // DIRECTIONS
        // =====================================

        frontLeft.setDirection(
                DcMotor.Direction.REVERSE
        );

        backLeft.setDirection(
                DcMotor.Direction.REVERSE
        );

        // =====================================
        // BRAKE
        // =====================================

        brake(frontLeft);
        brake(frontRight);
        brake(backLeft);
        brake(backRight);

        brake(intakeMotor);
        brake(shooterMotor);

        // =====================================
        // AI
        // =====================================

        ai = new BioBuzzAI(hardwareMap);

        telemetry.addLine(
                "=========================="
        );

        telemetry.addLine(
                "BIOBUZZ AI AUTO V1"
        );

        telemetry.addLine(
                "TEAM 24096"
        );

        telemetry.addLine(
                "=========================="
        );

        telemetry.addLine(
                "READY"
        );

        telemetry.update();

        waitForStart();

        if (isStopRequested()) {
            return;
        }

        ai.start();

        // =====================================
        // SEARCH
        // =====================================

        boolean found =
                searchPollen(6000);

        if (!found) {

            telemetry.addLine(
                    "POLLEN NOT FOUND"
            );

            telemetry.update();

            shutdown();

            return;
        }

        // =====================================
        // ALIGN
        // =====================================

        boolean aligned =
                alignPollen(3000);

        if (!aligned) {

            shutdown();

            return;
        }

        // =====================================
        // COLLECT
        // =====================================

        collectPollen(2500);

        // =====================================
        // FINISH
        // =====================================

        stopDrive();

        intakeMotor.setPower(0);

        telemetry.addLine(
                "AUTO COMPLETE"
        );

        telemetry.update();

        sleep(500);

        shutdown();
    }

    // =========================================
    // SEARCH POLLEN
    // =========================================

    private boolean searchPollen(
            long timeout) {

        long start =
                System.currentTimeMillis();

        while (
                opModeIsActive()
                        &&
                System.currentTimeMillis()
                        - start < timeout
        ) {

            ai.update();

            if (
                    ai.seesPollen()
                            &&
                    ai.getConfidence()
                            >= MIN_CONFIDENCE
            ) {

                stopDrive();

                return true;
            }

            mecanum(
                    0,
                    0,
                    SEARCH_SPEED
            );

            telemetry.addLine(
                    "SEARCHING POLLEN..."
            );

            telemetry.update();
        }

        stopDrive();

        return false;
    }

    // =========================================
    // ALIGN POLLEN
    // =========================================

    private boolean alignPollen(
            long timeout) {

        long start =
                System.currentTimeMillis();

        while (
                opModeIsActive()
                        &&
                System.currentTimeMillis()
                        - start < timeout
        ) {

            ai.update();

            if (!ai.seesPollen()) {
                return false;
            }

            double error =
                    ai.getX() - CENTER_X;

            if (
                    Math.abs(error)
                            <= TOLERANCE
            ) {

                stopDrive();

                return true;
            }

            if (error > 0) {

                mecanum(
                        0,
                        0,
                        ALIGN_SPEED
                );

            } else {

                mecanum(
                        0,
                        0,
                        -ALIGN_SPEED
                );
            }

            telemetry.addData(
                    "TARGET",
                    "POLLEN"
            );

            telemetry.addData(
                    "CONFIDENCE",
                    "%.1f%%",
                    ai.getConfidence()
            );

            telemetry.addData(
                    "X",
                    "%.1f",
                    ai.getX()
            );

            telemetry.update();
        }

        stopDrive();

        return false;
    }

    // =========================================
    // COLLECT
    // =========================================

    private void collectPollen(
            long timeout) {

        long start =
                System.currentTimeMillis();

        intakeMotor.setPower(1.0);

        while (
                opModeIsActive()
                        &&
                System.currentTimeMillis()
                        - start < timeout
        ) {

            ai.update();

            if (!ai.seesPollen()) {

                stopDrive();

                break;
            }

            double area =
                    ai.getArea();

            double speed;

            if (area > 1500) {

                speed = 0.12;

            } else {

                speed = DRIVE_SPEED;
            }

            mecanum(
                    speed,
                    0,
                    0
            );

            telemetry.addData(
                    "POLLEN AREA",
                    "%.1f",
                    area
            );

            telemetry.addData(
                    "CONFIDENCE",
                    "%.1f%%",
                    ai.getConfidence()
            );

            telemetry.update();
        }

        stopDrive();

        intakeMotor.setPower(0);
    }

    // =========================================
    // MECANUM
    // =========================================

    private void mecanum(
            double forward,
            double strafe,
            double turn) {

        double fl =
                forward + strafe + turn;

        double fr =
                forward - strafe - turn;

        double bl =
                forward - strafe + turn;

        double br =
                forward + strafe - turn;

        double max = Math.max(
                1.0,
                Math.max(
                        Math.abs(fl),
                        Math.max(
                                Math.abs(fr),
                                Math.max(
                                        Math.abs(bl),
                                        Math.abs(br)
                                )
                        )
                )
        );

        frontLeft.setPower(fl / max);
        frontRight.setPower(fr / max);
        backLeft.setPower(bl / max);
        backRight.setPower(br / max);
    }

    // =========================================
    // BRAKE
    // =========================================

    private void brake(
            DcMotor motor) {

        motor.setZeroPowerBehavior(
                DcMotor.ZeroPowerBehavior.BRAKE
        );
    }

    // =========================================
    // STOP
    // =========================================

    private void stopDrive() {

        frontLeft.setPower(0);
        frontRight.setPower(0);
        backLeft.setPower(0);
        backRight.setPower(0);
    }

    // =========================================
    // SHUTDOWN
    // =========================================

    private void shutdown() {

        stopDrive();

        intakeMotor.setPower(0);

        shooterMotor.setPower(0);

        if (ai != null) {
            ai.stop();
        }
    }
}