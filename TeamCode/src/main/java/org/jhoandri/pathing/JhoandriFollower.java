
package org.jhoandri.pathing;

import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.IMU;
import com.qualcomm.robotcore.util.Range;
import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

/**
 * Seguidor de rutas por odometria de ruedas + IMU.
 *
 * Sistema de coordenadas estandar (el mismo de Pedro Pathing):
 *   X = adelante, Y = IZQUIERDA, heading antihorario positivo (igual que el IMU).
 */
public class JhoandriFollower {

    private final DcMotorEx leftFront;
    private final DcMotorEx leftRear;
    private final DcMotorEx rightFront;
    private final DcMotorEx rightRear;
    private final IMU imu;

    private Pose currentPose = new Pose(0, 0, 0);

    private PathChain activeChain;
    private int activePathIndex;
    private boolean busy;

    private int lastLeftFrontTicks;
    private int lastLeftRearTicks;
    private int lastRightFrontTicks;
    private int lastRightRearTicks;

    private double headingOffset;

    public JhoandriFollower(
            DcMotorEx lf,
            DcMotorEx lr,
            DcMotorEx rf,
            DcMotorEx rr,
            IMU imu) {

        leftFront = lf;
        leftRear = lr;
        rightFront = rf;
        rightRear = rr;
        this.imu = imu;

        captureEncoderBaseline();
        headingOffset = -getRawHeadingRadians();
    }

    public void setCurrentPose(Pose pose) {
        currentPose = pose.copy();

        headingOffset = AngleUnit.normalizeRadians(
                currentPose.heading - getRawHeadingRadians()
        );

        captureEncoderBaseline();
    }

    public void followPath(PathChain chain, boolean holdEnd) {
        if (chain == null || chain.size() == 0) {
            activeChain = null;
            busy = false;
            stop();
            return;
        }

        activeChain = chain;
        activePathIndex = 0;
        busy = true;
    }

    public Pose update() {
        updatePoseFromEncoders();

        if (!busy || activeChain == null) {
            stop();
            return currentPose.copy();
        }

        BezierLine activePath = activeChain.getPath(activePathIndex);
        Pose goal = activePath.getEnd();

        driveToward(goal);

        if (atPose(goal)) {
            activePathIndex++;

            if (activePathIndex >= activeChain.size()) {
                busy = false;
                activeChain = null;
                stop();
            }
        }

        return currentPose.copy();
    }

    public boolean isBusy() {
        return busy;
    }

    public boolean atPose(Pose pose) {
        double positionError = Math.hypot(
                pose.x - currentPose.x,
                pose.y - currentPose.y
        );

        double headingError = Math.abs(
                AngleUnit.normalizeRadians(
                        pose.heading - currentPose.heading
                )
        );

        return positionError <=
                JhoandriPathingConstants.POSITION_TOLERANCE_IN
                && headingError <=
                JhoandriPathingConstants.HEADING_TOLERANCE_RAD;
    }

    public Pose getCurrentPose() {
        return currentPose.copy();
    }

    public int[] getEncoderTicks() {
        return new int[]{
                leftFront.getCurrentPosition(),
                leftRear.getCurrentPosition(),
                rightFront.getCurrentPosition(),
                rightRear.getCurrentPosition()
        };
    }

    public void stop() {
        leftFront.setPower(0);
        leftRear.setPower(0);
        rightFront.setPower(0);
        rightRear.setPower(0);
    }

    private void updatePoseFromEncoders() {
        int lfTicks = leftFront.getCurrentPosition();
        int lrTicks = leftRear.getCurrentPosition();
        int rfTicks = rightFront.getCurrentPosition();
        int rrTicks = rightRear.getCurrentPosition();

        double lf = (lfTicks - lastLeftFrontTicks)
                * JhoandriPathingConstants.LEFT_FRONT_ENCODER_SIGN;

        double lr = (lrTicks - lastLeftRearTicks)
                * JhoandriPathingConstants.LEFT_REAR_ENCODER_SIGN;

        double rf = (rfTicks - lastRightFrontTicks)
                * JhoandriPathingConstants.RIGHT_FRONT_ENCODER_SIGN;

        double rr = (rrTicks - lastRightRearTicks)
                * JhoandriPathingConstants.RIGHT_REAR_ENCODER_SIGN;

        lastLeftFrontTicks = lfTicks;
        lastLeftRearTicks = lrTicks;
        lastRightFrontTicks = rfTicks;
        lastRightRearTicks = rrTicks;

        double inchesPerTick =
                (2.0 * Math.PI
                        * JhoandriPathingConstants.WHEEL_RADIUS_IN
                        * JhoandriPathingConstants.WHEEL_ROTATIONS_PER_MOTOR_REV)
                        / JhoandriPathingConstants.TICKS_PER_REV;

        double forwardRobot =
                (lf + lr + rf + rr) * 0.25 * inchesPerTick
                        * JhoandriPathingConstants.FORWARD_MULTIPLIER;

        // La combinacion (lf - lr - rf + rr) mide strafe a la DERECHA; con el
        // signo menos queda "izquierda positivo" como pide el sistema estandar.
        double strafeLeftRobot =
                -(lf - lr - rf + rr) * 0.25 * inchesPerTick
                        * JhoandriPathingConstants.STRAFE_MULTIPLIER
                        * JhoandriPathingConstants.STRAFE_SIGN;

        double newHeading = AngleUnit.normalizeRadians(
                getRawHeadingRadians() + headingOffset
        );

        double headingDelta = AngleUnit.normalizeRadians(
                newHeading - currentPose.heading
        );

        double midpoint = currentPose.heading + headingDelta * 0.5;

        currentPose.x += forwardRobot * Math.cos(midpoint)
                - strafeLeftRobot * Math.sin(midpoint);

        currentPose.y += forwardRobot * Math.sin(midpoint)
                + strafeLeftRobot * Math.cos(midpoint);

        currentPose.heading = newHeading;
    }

    private void driveToward(Pose target) {
        double dx = target.x - currentPose.x;
        double dy = target.y - currentPose.y;
        double distance = Math.hypot(dx, dy);

        // Error expresado en el marco del robot.
        double forwardError =
                Math.cos(currentPose.heading) * dx
                        + Math.sin(currentPose.heading) * dy;

        double strafeLeftError =
                -Math.sin(currentPose.heading) * dx
                        + Math.cos(currentPose.heading) * dy;

        double translationPower = Range.clip(
                distance * JhoandriPathingConstants.TRANSLATION_KP,
                0.0,
                JhoandriPathingConstants.MAX_TRANSLATION_POWER
        );

        if (distance > JhoandriPathingConstants.POSITION_TOLERANCE_IN) {
            translationPower = Math.max(
                    translationPower,
                    JhoandriPathingConstants.MIN_TRANSLATION_POWER
            );
        } else {
            translationPower = 0.0;
        }

        double forwardPower = distance > 0
                ? forwardError / distance * translationPower : 0.0;

        double strafeLeftPower = distance > 0
                ? strafeLeftError / distance * translationPower : 0.0;

        // Positivo = hay que girar antihorario.
        double headingError = AngleUnit.normalizeRadians(
                target.heading - currentPose.heading
        );

        double turnCcwPower = Range.clip(
                headingError * JhoandriPathingConstants.HEADING_KP,
                -JhoandriPathingConstants.MAX_TURN_POWER,
                JhoandriPathingConstants.MAX_TURN_POWER
        );

        if (Math.abs(headingError) >
                JhoandriPathingConstants.HEADING_TOLERANCE_RAD) {

            turnCcwPower = Math.copySign(
                    Math.max(
                            Math.abs(turnCcwPower),
                            JhoandriPathingConstants.MIN_TURN_POWER
                    ),
                    headingError
            );
        } else {
            turnCcwPower = 0.0;
        }

        setMecanumPower(forwardPower, strafeLeftPower, turnCcwPower);
    }

    /**
     * @param forward    adelante positivo
     * @param strafeLeft izquierda positivo
     * @param turnCcw    antihorario positivo (igual que el IMU)
     */
    private void setMecanumPower(
            double forward,
            double strafeLeft,
            double turnCcw) {

        // CORRECCION: antes el giro se mandaba como horario mientras el error
        // del IMU es antihorario, asi que el robot giraba al lado contrario y
        // nunca alcanzaba el angulo. Igual con el strafe.
        double strafeRight = -strafeLeft;
        double turnCw = -turnCcw;

        double lf = forward + strafeRight + turnCw;
        double lr = forward - strafeRight + turnCw;
        double rf = forward - strafeRight - turnCw;
        double rr = forward + strafeRight - turnCw;

        double scale = Math.max(
                1.0,
                Math.max(
                        Math.max(Math.abs(lf), Math.abs(lr)),
                        Math.max(Math.abs(rf), Math.abs(rr))
                )
        );

        leftFront.setPower(lf / scale);
        leftRear.setPower(lr / scale);
        rightFront.setPower(rf / scale);
        rightRear.setPower(rr / scale);
    }

    private double getRawHeadingRadians() {
        return imu.getRobotYawPitchRollAngles()
                .getYaw(AngleUnit.RADIANS);
    }

    private void captureEncoderBaseline() {
        lastLeftFrontTicks = leftFront.getCurrentPosition();
        lastLeftRearTicks = leftRear.getCurrentPosition();
        lastRightFrontTicks = rightFront.getCurrentPosition();
        lastRightRearTicks = rightRear.getCurrentPosition();
    }
}
