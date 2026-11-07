
package org.jhoandri.pathing;

import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.hardware.RobotHardware;
import org.jhoandri.pathing.Vision.JhoandriVision;

/**
 * Fachada de navegacion: odometria + seguidor + vision + mecanismos.
 *
 * Coordenadas estandar (Pedro Pathing): X adelante, Y izquierda,
 * heading antihorario positivo, en pulgadas y radianes.
 */
public class JhoandriPathing {

    private final RobotHardware robot;
    private final JhoandriFollower follower;
    private final JhoandriVision vision;

    private Pose currentPose = new Pose(0, 0, 0);
    private Pose targetPose = new Pose(0, 0, 0);

    public JhoandriPathing(HardwareMap hardwareMap) {
        this(hardwareMap, true);
    }

    public JhoandriPathing(
            HardwareMap hardwareMap,
            boolean enableVision) {

        // La Limelight la administra JhoandriVision, no RobotHardware.
        robot = new RobotHardware(hardwareMap, false);

        if (!robot.hasImu()) {
            throw new IllegalStateException(
                    "JhoandriPathing necesita el IMU llamado 'imu' en la configuracion.");
        }

        follower = new JhoandriFollower(
                robot.frontLeft, robot.backLeft,
                robot.frontRight, robot.backRight,
                robot.imu
        );

        vision = enableVision
                ? new JhoandriVision(hardwareMap)
                : null;

        robot.resetHeading();
        setPose(0, 0, 0);
    }

    /** Acceso al hardware compartido (shooter, intake, transfer...). */
    public RobotHardware getRobot() {
        return robot;
    }

    public void setPose(double x, double y, double headingRad) {
        currentPose = new Pose(x, y, headingRad);
        follower.setCurrentPose(currentPose);
    }

    public void resetPoseAndHeading(
            double x, double y, double headingRad) {

        robot.resetHeading();
        setPose(x, y, headingRad);
    }

    public void setTarget(double x, double y, double headingRad) {
        targetPose = new Pose(x, y, headingRad);

        PathChain chain = new PathChain();
        chain.addPath(new BezierLine(currentPose, targetPose));

        follower.followPath(chain, false);
    }

    /**
     * Llamar continuamente desde el loop del OpMode.
     * Devuelve true en el ciclo en que termina el recorrido.
     */
    public boolean update() {
        boolean wasBusy = follower.isBusy();

        currentPose = follower.update();

        if (vision != null) {
            vision.updateDetections(currentPose);
        }

        return wasBusy && !follower.isBusy();
    }

    public double getX() {
        return currentPose.x;
    }

    public double getY() {
        return currentPose.y;
    }

    public double getHeading() {
        return currentPose.heading;
    }

    public Pose getPose() {
        return currentPose.copy();
    }

    public boolean isBusy() {
        return follower.isBusy();
    }

    public boolean atTarget() {
        return follower.atPose(targetPose);
    }

    public int[] getDriveEncoderTicks() {
        return follower.getEncoderTicks();
    }

    public JhoandriVision getVision() {
        return vision;
    }

    public void setShooterPower(double power) {
        if (robot.shooter != null) {
            robot.shooter.setPower(Math.max(-1.0, Math.min(1.0, power)));
        }
    }

    public void setIntakePower(double power) {
        robot.setIntakePower(power);
    }

    public void setTransferServo(double position) {
        if (robot.feederServo != null) {
            robot.feederServo.setPosition(Math.max(0.0, Math.min(1.0, position)));
        }
    }

    public void stopMechanisms() {
        robot.stopMechanisms();
    }

    public void stop() {
        follower.stop();
        robot.stopAll();

        if (vision != null) {
            vision.stop();
        }
    }
}
