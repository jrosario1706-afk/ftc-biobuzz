package org.firstinspires.ftc.teamcode.autonomous;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

/**
 * Shoots the preload and parks using timed movement only (no odometry).
 */
@Config
@Autonomous(name = "BioBuzz Shoot and Park", group = "BioBuzz Simple")
public class BioBuzzTiraParking extends TimedAutoBase {

    public static double DRIVE_POWER = 0.45;
    public static double STRAFE_POWER = 0.40;
    public static double SHOOTER_VELOCITY = 1700;
    public static double FEED_TIME_SECONDS = 2.0;

    @Override
    protected String getAutoName() {
        return "AUTONOMOUS: SHOOT AND PARK";
    }

    @Override
    protected void runSequence() {
        step("1/5 Drive to the launch area");
        if (!driveForward(DRIVE_POWER, 1.0)) return;

        step("2/5 Shoot the preload");
        if (!shoot(SHOOTER_VELOCITY, 1.5, FEED_TIME_SECONDS)) return;

        step("3/5 Strafe to adjust position");
        if (!strafeRight(STRAFE_POWER, 0.8)) return;

        step("4/5 Drive to the parking area");
        if (!driveForward(DRIVE_POWER, 1.2)) return;

        step("5/5 Final parking adjustment");
        strafeLeft(STRAFE_POWER, 0.7);
    }
}
