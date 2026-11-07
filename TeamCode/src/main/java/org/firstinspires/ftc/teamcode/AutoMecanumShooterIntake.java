package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.rev.RevHubOrientationOnRobot;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;
import com.qualcomm.robotcore.eventloop.opmode.LinearOpMode;
import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DigitalChannel;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.hardware.PIDFCoefficients;
import com.qualcomm.robotcore.hardware.Servo;
import com.qualcomm.robotcore.hardware.VoltageSensor;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

/**
 * Calibratable autonomous base for a mecanum FTC robot.
 *
 * This program deliberately has NO invented physical values. It will stop after START
 * until the calibration section below is filled out and AUTO_CALIBRATION_COMPLETE is true.
 * Test every mechanism safely before enabling autonomous movement.
 *
 * Required Robot Configuration names:
 * frontLeft, frontRight, backLeft, backRight, intakeMotor, shooterMotor
 *
 * Added hardware names:
 * elevatorMotor      - optional DcMotorEx elevator
 * imu                - optional Control Hub/Expansion Hub IMU
 * feederServo        - optional servo that pushes a game piece into the shooter
 * elevatorHomeSwitch - optional DigitalChannel limit switch for elevator homing
 * pollenSensor       - optional DigitalChannel sensor for future intake detection
 */
@Autonomous(name = "Auto Mecanum Shooter Intake", group = "Competition")
public class AutoMecanumShooterIntake extends LinearOpMode {

    // -------------------------------------------------------------------------
    // CALIBRATION: Set every value from tests on YOUR robot. Do not guess.
    // -------------------------------------------------------------------------

    /** Set true only after all required constants below have been measured. */
    private static final boolean AUTO_CALIBRATION_COMPLETE = false;

    // Drivetrain encoders and path. Counts are encoder ticks, not millimeters.
    private static final double DRIVE_POWER = 0.0; // CALIBRATE: safe drive power (0 to 1).
    private static final int DRIVE_TO_POLLEN_COUNTS = 0; // CALIBRATE.
    private static final int RETURN_COUNTS = 0; // CALIBRATE: positive distance; auto drives backward.
    private static final int PARK_COUNTS = 0; // CALIBRATE: signed encoder distance for parking.
    private static final double DRIVE_TIMEOUT_SECONDS = 0.0; // CALIBRATE.

    // IMU. Set these to the actual physical orientation of the Control/Expansion Hub.
    // Leave null until you have checked the Hub mounting. The auto will refuse to run.
    private static final RevHubOrientationOnRobot.LogoFacingDirection HUB_LOGO_DIRECTION = null;
    private static final RevHubOrientationOnRobot.UsbFacingDirection HUB_USB_DIRECTION = null;
    private static final double AIM_HEADING_DEGREES = Double.NaN; // CALIBRATE from field setup.
    private static final double HEADING_TOLERANCE_DEGREES = 0.0; // CALIBRATE.
    private static final double TURN_KP = 0.0; // CALIBRATE.
    private static final double MAX_TURN_POWER = 0.0; // CALIBRATE: 0 to 1.
    private static final double TURN_TIMEOUT_SECONDS = 0.0; // CALIBRATE.

    // Shooter velocity units are encoder ticks per second. Determine them from telemetry.
    private static final double SHOOTER_TARGET_VELOCITY_TPS = 0.0; // CALIBRATE.
    private static final double SHOOTER_READY_MIN_VELOCITY_TPS = 0.0; // CALIBRATE.
    private static final double SHOOTER_PIDF_P = 0.0; // CALIBRATE.
    private static final double SHOOTER_PIDF_I = 0.0; // CALIBRATE.
    private static final double SHOOTER_PIDF_D = 0.0; // CALIBRATE.
    private static final double SHOOTER_PIDF_F = 0.0; // CALIBRATE.
    private static final boolean SHOOTER_PIDF_CALIBRATED = false;
    private static final double SHOOTER_SPINUP_TIMEOUT_SECONDS = 0.0; // CALIBRATE.

    // Intake and feeder. Feed time is needed because a normal servo has no position feedback.
    private static final double INTAKE_POWER = 0.0; // CALIBRATE: -1 to 1.
    private static final double INTAKE_MAX_SECONDS = 0.0; // CALIBRATE fallback if no pollenSensor.
    private static final double FEEDER_HOME_POSITION = Double.NaN; // CALIBRATE: 0 to 1.
    private static final double FEEDER_FIRE_POSITION = Double.NaN; // CALIBRATE: 0 to 1.
    private static final double FEED_DURATION_SECONDS = 0.0; // CALIBRATE.

    // Optional digital sensors. Leave null until polarity is tested.
    private static final Boolean POLLEN_SENSOR_ACTIVE_LOW = null;
    private static final Boolean ELEVATOR_HOME_SWITCH_ACTIVE_LOW = null;

    // Elevator encoder positions. These must be measured on the real elevator.
    private static final int ELEVATOR_HOME_TICKS = 0; // Encoder zero only after homing.
    private static final int ELEVATOR_LOW_TICKS = 0; // CALIBRATE.
    private static final int ELEVATOR_MID_TICKS = 0; // CALIBRATE.
    private static final int ELEVATOR_HIGH_TICKS = 0; // CALIBRATE.
    private static final int ELEVATOR_MIN_TICKS = 0; // CALIBRATE.
    private static final int ELEVATOR_MAX_TICKS = 0; // CALIBRATE.
    private static final double ELEVATOR_POWER = 0.0; // CALIBRATE: 0 to 1.
    private static final double ELEVATOR_HOMING_POWER = 0.0; // CALIBRATE: safe direction and power.

    private enum AutoState {
        START, DRIVE_TO_POLLEN, INTAKE, AIM, SPINUP, SHOOT, RETURN, PARK, DONE
    }

    private enum ElevatorPosition {
        HOME, LOW, MID, HIGH
    }

    private DcMotor frontLeft;
    private DcMotor frontRight;
    private DcMotor backLeft;
    private DcMotor backRight;
    private DcMotor intakeMotor;
    private DcMotorEx shooterMotor;
    private DcMotorEx elevatorMotor;
    private Servo feederServo;
    private DigitalChannel elevatorHomeSwitch;
    private DigitalChannel pollenSensor;
    private IMU imu;

    private final ElapsedTime stateTimer = new ElapsedTime();
    private AutoState autoState = AutoState.START;
    private boolean imuConfigured;
    private int elevatorTargetTicks = ELEVATOR_HOME_TICKS;

    @Override
    public void runOpMode() {
        initializeHardware();
        initializeImu();
        initializeVisionPlaceholder();

        telemetry.addLine("BIOBUZZ autonomous base initialized");
        telemetry.addLine("This auto will not run until calibration is completed.");
        telemetry.addData("Feeder", feederServo == null ? "not installed" : "installed");
        telemetry.addData("Elevator", elevatorMotor == null ? "not installed" : "installed");
        telemetry.addData("IMU", imuConfigured ? "ready" : "needs orientation calibration");
        telemetry.update();

        waitForStart();

        if (isStopRequested()) {
            stopAllMotors();
            return;
        }

        if (!autoCalibrationIsValid()) {
            telemetry.addLine("AUTO CANCELLED: finish calibration constants first.");
            telemetry.update();
            stopAllMotors();
            return;
        }

        setAutoState(AutoState.START);
        while (opModeIsActive() && autoState != AutoState.DONE) {
            updateAutonomousState();
            updateTelemetry();
            idle();
        }

        stopAllMotors();
    }

    private void initializeHardware() {
        frontLeft = hardwareMap.get(DcMotor.class, "frontLeft");
        frontRight = hardwareMap.get(DcMotor.class, "frontRight");
        backLeft = hardwareMap.get(DcMotor.class, "backLeft");
        backRight = hardwareMap.get(DcMotor.class, "backRight");
        intakeMotor = hardwareMap.get(DcMotor.class, "intakeMotor");
        shooterMotor = hardwareMap.get(DcMotorEx.class, "shooterMotor");

        // New devices are optional so older robot configurations do not crash.
        elevatorMotor = hardwareMap.tryGet(DcMotorEx.class, "elevatorMotor");
        feederServo = hardwareMap.tryGet(Servo.class, "feederServo");
        elevatorHomeSwitch = hardwareMap.tryGet(DigitalChannel.class, "elevatorHomeSwitch");
        pollenSensor = hardwareMap.tryGet(DigitalChannel.class, "pollenSensor");
        imu = hardwareMap.tryGet(IMU.class, "imu");

        // Verify these directions one motor at a time before the first drive test.
        frontLeft.setDirection(DcMotor.Direction.REVERSE);
        backLeft.setDirection(DcMotor.Direction.REVERSE);

        setDriveBrake();
        intakeMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        shooterMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
        shooterMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        configureShooterPidf();

        if (elevatorMotor != null) {
            elevatorMotor.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            elevatorMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        }
        if (elevatorHomeSwitch != null) elevatorHomeSwitch.setMode(DigitalChannel.Mode.INPUT);
        if (pollenSensor != null) pollenSensor.setMode(DigitalChannel.Mode.INPUT);

        resetFeeder();
        resetDriveEncoders();
        stopAllMotors();
    }

    /** Initializes yaw only after the real Hub mounting orientation has been entered above. */
    private void initializeImu() {
        imuConfigured = false;
        if (imu == null || HUB_LOGO_DIRECTION == null || HUB_USB_DIRECTION == null) return;

        IMU.Parameters parameters = new IMU.Parameters(new RevHubOrientationOnRobot(
                HUB_LOGO_DIRECTION, HUB_USB_DIRECTION));
        imu.initialize(parameters);
        resetHeading();
        imuConfigured = true;
    }

    /** Reserved for AprilTag setup; camera name, tag IDs, and field poses are intentionally unspecified. */
    private void initializeVisionPlaceholder() {
        // Add VisionPortal and AprilTagProcessor only after the camera and legal tag data are known.
    }

    private void updateAutonomousState() {
        switch (autoState) {
            case START:
                driveForward(DRIVE_TO_POLLEN_COUNTS);
                setAutoState(AutoState.DRIVE_TO_POLLEN);
                break;

            case DRIVE_TO_POLLEN:
                if (driveMotionComplete()) {
                    stopDrive();
                    intakeIn();
                    setAutoState(AutoState.INTAKE);
                } else if (stateTimer.seconds() >= DRIVE_TIMEOUT_SECONDS) {
                    failSafeStop("Drive to pollen timed out");
                }
                break;

            case INTAKE:
                // A real sensor takes priority. The timed fallback is for robots without pollenSensor.
                if (isPollenDetected() || stateTimer.seconds() >= INTAKE_MAX_SECONDS) {
                    intakeStop();
                    setAutoState(AutoState.AIM);
                }
                break;

            case AIM:
                if (turnToHeading(AIM_HEADING_DEGREES)) {
                    startShooter();
                    setAutoState(AutoState.SPINUP);
                } else if (stateTimer.seconds() >= TURN_TIMEOUT_SECONDS) {
                    failSafeStop("Aim timed out");
                }
                break;

            case SPINUP:
                if (shooterIsReady()) {
                    feed();
                    intakeIn();
                    setAutoState(AutoState.SHOOT);
                } else if (stateTimer.seconds() >= SHOOTER_SPINUP_TIMEOUT_SECONDS) {
                    failSafeStop("Shooter did not reach target velocity");
                }
                break;

            case SHOOT:
                // Standard FTC servos have no feedback; use only a field-tested feed duration.
                if (stateTimer.seconds() >= FEED_DURATION_SECONDS) {
                    resetFeeder();
                    intakeStop();
                    stopShooter();
                    driveBackward(RETURN_COUNTS);
                    setAutoState(AutoState.RETURN);
                }
                break;

            case RETURN:
                if (driveMotionComplete()) {
                    stopDrive();
                    driveByEncoderCounts(PARK_COUNTS, 0, DRIVE_POWER);
                    setAutoState(AutoState.PARK);
                } else if (stateTimer.seconds() >= DRIVE_TIMEOUT_SECONDS) {
                    failSafeStop("Return timed out");
                }
                break;

            case PARK:
                if (driveMotionComplete()) {
                    stopDrive();
                    setAutoState(AutoState.DONE);
                } else if (stateTimer.seconds() >= DRIVE_TIMEOUT_SECONDS) {
                    failSafeStop("Park timed out");
                }
                break;

            case DONE:
            default:
                stopAllMotors();
                break;
        }
    }

    private void setAutoState(AutoState nextState) {
        autoState = nextState;
        stateTimer.reset();
    }

    // -------------------------------------------------------------------------
    // Shooter subsystem
    // -------------------------------------------------------------------------

    private void configureShooterPidf() {
        if (SHOOTER_PIDF_CALIBRATED) {
            shooterMotor.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER,
                    new PIDFCoefficients(SHOOTER_PIDF_P, SHOOTER_PIDF_I,
                            SHOOTER_PIDF_D, SHOOTER_PIDF_F));
        }
    }

    private void startShooter() {
        shooterMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        shooterMotor.setVelocity(SHOOTER_TARGET_VELOCITY_TPS);
    }

    private void stopShooter() {
        shooterMotor.setVelocity(0);
        shooterMotor.setPower(0);
    }

    private boolean shooterIsReady() {
        return shooterMotor.getVelocity() >= SHOOTER_READY_MIN_VELOCITY_TPS;
    }

    // -------------------------------------------------------------------------
    // Intake and feeder subsystem
    // -------------------------------------------------------------------------

    private void intakeIn() {
        intakeMotor.setPower(INTAKE_POWER);
    }

    private void intakeOut() {
        intakeMotor.setPower(-INTAKE_POWER);
    }

    private void intakeStop() {
        intakeMotor.setPower(0);
    }

    /** Placeholder for future current-based jam detection. */
    private boolean intakeIsJammed() {
        return false;
    }

    private void feed() {
        if (feederServo != null && feederServoIsConfigured()) {
            feederServo.setPosition(FEEDER_FIRE_POSITION);
        }
    }

    private void resetFeeder() {
        if (feederServo != null && feederServoIsConfigured()) {
            feederServo.setPosition(FEEDER_HOME_POSITION);
        }
    }

    private boolean feederServoIsConfigured() {
        return isServoPosition(FEEDER_HOME_POSITION) && isServoPosition(FEEDER_FIRE_POSITION);
    }

    private boolean isPollenDetected() {
        if (pollenSensor == null || POLLEN_SENSOR_ACTIVE_LOW == null) return false;
        boolean rawState = pollenSensor.getState();
        return POLLEN_SENSOR_ACTIVE_LOW ? !rawState : rawState;
    }

    // -------------------------------------------------------------------------
    // Elevator subsystem
    // -------------------------------------------------------------------------

    private void moveElevatorTo(ElevatorPosition position) {
        if (elevatorMotor == null || !elevatorCalibrationIsValid()) return;

        int requestedTicks;
        switch (position) {
            case LOW:
                requestedTicks = ELEVATOR_LOW_TICKS;
                break;
            case MID:
                requestedTicks = ELEVATOR_MID_TICKS;
                break;
            case HIGH:
                requestedTicks = ELEVATOR_HIGH_TICKS;
                break;
            case HOME:
            default:
                requestedTicks = ELEVATOR_HOME_TICKS;
                break;
        }

        elevatorTargetTicks = Range.clip(requestedTicks, ELEVATOR_MIN_TICKS, ELEVATOR_MAX_TICKS);
        elevatorMotor.setTargetPosition(elevatorTargetTicks);
        elevatorMotor.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        elevatorMotor.setPower(Math.abs(ELEVATOR_POWER));
    }

    /**
     * Call this in a controlled test after setting homing direction and switch polarity.
     * It returns true only after it safely resets the encoder at HOME.
     */
    private boolean homeElevator() {
        if (elevatorMotor == null || elevatorHomeSwitch == null
                || ELEVATOR_HOME_SWITCH_ACTIVE_LOW == null || ELEVATOR_HOMING_POWER == 0) {
            return false;
        }

        if (!elevatorHomeSwitchPressed()) {
            elevatorMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            elevatorMotor.setPower(ELEVATOR_HOMING_POWER);
            return false;
        }

        elevatorMotor.setPower(0);
        elevatorMotor.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        elevatorMotor.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        elevatorTargetTicks = ELEVATOR_HOME_TICKS;
        return true;
    }

    private boolean elevatorHomeSwitchPressed() {
        if (elevatorHomeSwitch == null || ELEVATOR_HOME_SWITCH_ACTIVE_LOW == null) return false;
        boolean rawState = elevatorHomeSwitch.getState();
        return ELEVATOR_HOME_SWITCH_ACTIVE_LOW ? !rawState : rawState;
    }

    private boolean elevatorCalibrationIsValid() {
        return ELEVATOR_MIN_TICKS <= ELEVATOR_HOME_TICKS
                && ELEVATOR_HOME_TICKS <= ELEVATOR_LOW_TICKS
                && ELEVATOR_LOW_TICKS <= ELEVATOR_MID_TICKS
                && ELEVATOR_MID_TICKS <= ELEVATOR_HIGH_TICKS
                && ELEVATOR_HIGH_TICKS <= ELEVATOR_MAX_TICKS
                && ELEVATOR_POWER > 0 && ELEVATOR_POWER <= 1;
    }

    // -------------------------------------------------------------------------
    // Mecanum drive and IMU subsystem
    // -------------------------------------------------------------------------

    private void setDriveBrake() {
        frontLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        frontRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backLeft.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
        backRight.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
    }

    private void resetDriveEncoders() {
        frontLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        frontRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        backLeft.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        backRight.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
        setDriveRunUsingEncoder();
    }

    private void setDriveRunUsingEncoder() {
        frontLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        frontRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        backLeft.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
        backRight.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
    }

    private void driveForward(int counts) {
        driveByEncoderCounts(counts, 0, DRIVE_POWER);
    }

    private void driveBackward(int counts) {
        driveByEncoderCounts(-counts, 0, DRIVE_POWER);
    }

    private void strafeRight(int counts) {
        driveByEncoderCounts(0, counts, DRIVE_POWER);
    }

    private void strafeLeft(int counts) {
        driveByEncoderCounts(0, -counts, DRIVE_POWER);
    }

    /** Starts an encoder move; forward and strafe values are signed ticks. */
    private void driveByEncoderCounts(int forwardCounts, int strafeCounts, double power) {
        int frontLeftTarget = frontLeft.getCurrentPosition() + forwardCounts + strafeCounts;
        int frontRightTarget = frontRight.getCurrentPosition() + forwardCounts - strafeCounts;
        int backLeftTarget = backLeft.getCurrentPosition() + forwardCounts - strafeCounts;
        int backRightTarget = backRight.getCurrentPosition() + forwardCounts + strafeCounts;

        frontLeft.setTargetPosition(frontLeftTarget);
        frontRight.setTargetPosition(frontRightTarget);
        backLeft.setTargetPosition(backLeftTarget);
        backRight.setTargetPosition(backRightTarget);

        frontLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        frontRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        backLeft.setMode(DcMotor.RunMode.RUN_TO_POSITION);
        backRight.setMode(DcMotor.RunMode.RUN_TO_POSITION);

        double safePower = Range.clip(Math.abs(power), 0, 1);
        frontLeft.setPower(safePower);
        frontRight.setPower(safePower);
        backLeft.setPower(safePower);
        backRight.setPower(safePower);
    }

    private boolean driveMotionComplete() {
        return !frontLeft.isBusy() && !frontRight.isBusy()
                && !backLeft.isBusy() && !backRight.isBusy();
    }

    /** Open-loop mecanum power only for an IMU-controlled turn. Powers are normalized. */
    private void setMecanumPower(double drive, double strafe, double turn) {
        double frontLeftPower = drive + strafe + turn;
        double frontRightPower = drive - strafe - turn;
        double backLeftPower = drive - strafe + turn;
        double backRightPower = drive + strafe - turn;

        double maximum = Math.max(1.0, Math.max(Math.abs(frontLeftPower),
                Math.max(Math.abs(frontRightPower), Math.max(Math.abs(backLeftPower),
                        Math.abs(backRightPower)))));

        frontLeft.setPower(frontLeftPower / maximum);
        frontRight.setPower(frontRightPower / maximum);
        backLeft.setPower(backLeftPower / maximum);
        backRight.setPower(backRightPower / maximum);
    }

    /** Non-blocking IMU turn. Returns true only after the robot is within tolerance. */
    private boolean turnToHeading(double targetHeadingDegrees) {
        double currentHeading = getHeading();
        if (Double.isNaN(currentHeading)) return false;

        double error = normalizeAngle(targetHeadingDegrees - currentHeading);
        if (Math.abs(error) <= HEADING_TOLERANCE_DEGREES) {
            stopDrive();
            return true;
        }

        double turnPower = Range.clip(error * TURN_KP, -MAX_TURN_POWER, MAX_TURN_POWER);
        setDriveRunUsingEncoder();
        setMecanumPower(0, 0, turnPower);
        return false;
    }

    private double getHeading() {
        if (!imuConfigured || imu == null) return Double.NaN;
        return imu.getRobotYawPitchRollAngles().getYaw(AngleUnit.DEGREES);
    }

    private void resetHeading() {
        if (imu != null) imu.resetYaw();
    }

    private double normalizeAngle(double angleDegrees) {
        while (angleDegrees > 180) angleDegrees -= 360;
        while (angleDegrees <= -180) angleDegrees += 360;
        return angleDegrees;
    }

    private void stopDrive() {
        frontLeft.setPower(0);
        frontRight.setPower(0);
        backLeft.setPower(0);
        backRight.setPower(0);
        setDriveRunUsingEncoder();
    }

    // -------------------------------------------------------------------------
    // Telemetry, validation, and safety
    // -------------------------------------------------------------------------

    private void updateTelemetry() {
        telemetry.addData("STATE", autoState);
        telemetry.addData("FL encoder", frontLeft.getCurrentPosition());
        telemetry.addData("FR encoder", frontRight.getCurrentPosition());
        telemetry.addData("BL encoder", backLeft.getCurrentPosition());
        telemetry.addData("BR encoder", backRight.getCurrentPosition());
        telemetry.addData("Shooter target velocity", SHOOTER_TARGET_VELOCITY_TPS);
        telemetry.addData("Shooter actual velocity", shooterMotor.getVelocity());
        telemetry.addData("Shooter velocity error", SHOOTER_TARGET_VELOCITY_TPS - shooterMotor.getVelocity());
        telemetry.addData("Elevator target", elevatorTargetTicks);
        if (elevatorMotor == null) {
            telemetry.addData("Elevator position", "not installed");
        } else {
            telemetry.addData("Elevator position", elevatorMotor.getCurrentPosition());
        }
        if (imuConfigured) {
            telemetry.addData("IMU heading", getHeading());
        } else {
            telemetry.addData("IMU heading", "not configured");
        }
        telemetry.addData("Battery voltage", "%.2f V", getBatteryVoltage());
        telemetry.addData("Pollen detected", isPollenDetected());
        telemetry.addData("Intake jam", intakeIsJammed());
        telemetry.update();
    }

    private double getBatteryVoltage() {
        double lowestVoltage = Double.POSITIVE_INFINITY;
        for (VoltageSensor sensor : hardwareMap.voltageSensor) {
            double voltage = sensor.getVoltage();
            if (voltage > 0) lowestVoltage = Math.min(lowestVoltage, voltage);
        }
        return Double.isInfinite(lowestVoltage) ? 0 : lowestVoltage;
    }

    private boolean autoCalibrationIsValid() {
        boolean valid = AUTO_CALIBRATION_COMPLETE
                && DRIVE_POWER > 0 && DRIVE_POWER <= 1
                && DRIVE_TO_POLLEN_COUNTS != 0 && RETURN_COUNTS != 0 && PARK_COUNTS != 0
                && DRIVE_TIMEOUT_SECONDS > 0
                && imuConfigured && !Double.isNaN(AIM_HEADING_DEGREES)
                && HEADING_TOLERANCE_DEGREES > 0 && TURN_KP > 0
                && MAX_TURN_POWER > 0 && MAX_TURN_POWER <= 1 && TURN_TIMEOUT_SECONDS > 0
                && SHOOTER_PIDF_CALIBRATED
                && SHOOTER_TARGET_VELOCITY_TPS > 0
                && SHOOTER_READY_MIN_VELOCITY_TPS > 0
                && SHOOTER_READY_MIN_VELOCITY_TPS <= SHOOTER_TARGET_VELOCITY_TPS
                && SHOOTER_SPINUP_TIMEOUT_SECONDS > 0
                && INTAKE_POWER > 0 && INTAKE_POWER <= 1 && INTAKE_MAX_SECONDS > 0
                && FEED_DURATION_SECONDS > 0;

        if (feederServo != null) valid = valid && feederServoIsConfigured();
        return valid;
    }

    private boolean isServoPosition(double position) {
        return !Double.isNaN(position) && position >= 0 && position <= 1;
    }

    private void failSafeStop(String reason) {
        telemetry.addData("AUTO STOP", reason);
        telemetry.update();
        stopAllMotors();
        setAutoState(AutoState.DONE);
    }

    private void stopAllMotors() {
        if (frontLeft != null) frontLeft.setPower(0);
        if (frontRight != null) frontRight.setPower(0);
        if (backLeft != null) backLeft.setPower(0);
        if (backRight != null) backRight.setPower(0);
        if (intakeMotor != null) intakeMotor.setPower(0);
        if (shooterMotor != null) {
            shooterMotor.setVelocity(0);
            shooterMotor.setPower(0);
        }
        if (elevatorMotor != null) elevatorMotor.setPower(0);
        resetFeeder();
    }
}
