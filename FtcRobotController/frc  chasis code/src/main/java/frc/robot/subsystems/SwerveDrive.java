package frc.robot.subsystems;

import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Translation2d;
import edu.wpi.first.math.kinematics.ChassisSpeeds;
import edu.wpi.first.math.kinematics.SwerveDriveKinematics;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import frc.robot.Constants;

public final class SwerveDrive {
    private final SwerveModule[] modules;
    private final SwerveDriveKinematics kinematics;

    public SwerveDrive() {
        if (!Constants.Drive.isConfigured()) {
            throw new IllegalStateException(
                    "Swerve hardware is not configured. Set module CAN IDs, DIO "
                            + "channels, encoder offsets, wheelbase, trackwidth, "
                            + "and maximum speed in Constants.java.");
        }

        double halfWheelbase = Constants.Drive.WHEELBASE_METERS / 2.0;
        double halfTrackwidth = Constants.Drive.TRACKWIDTH_METERS / 2.0;
        kinematics = new SwerveDriveKinematics(
                new Translation2d(halfWheelbase, halfTrackwidth),
                new Translation2d(halfWheelbase, -halfTrackwidth),
                new Translation2d(-halfWheelbase, halfTrackwidth),
                new Translation2d(-halfWheelbase, -halfTrackwidth));

        modules = new SwerveModule[] {
                new SwerveModule(Constants.Drive.FRONT_LEFT,
                        Constants.Drive.MAX_SPEED_METERS_PER_SECOND),
                new SwerveModule(Constants.Drive.FRONT_RIGHT,
                        Constants.Drive.MAX_SPEED_METERS_PER_SECOND),
                new SwerveModule(Constants.Drive.BACK_LEFT,
                        Constants.Drive.MAX_SPEED_METERS_PER_SECOND),
                new SwerveModule(Constants.Drive.BACK_RIGHT,
                        Constants.Drive.MAX_SPEED_METERS_PER_SECOND)
        };
    }

    public void drive(double forward, double left, double turn, boolean slowMode) {
        if (Math.abs(forward) < Constants.JOYSTICK_DEADBAND
                && Math.abs(left) < Constants.JOYSTICK_DEADBAND
                && Math.abs(turn) < Constants.JOYSTICK_DEADBAND) {
            stop();
            return;
        }
        for (SwerveModule module : modules) {
            if (!module.isEncoderConnected()) {
                stop();
                return;
            }
        }

        double scale = slowMode ? Constants.SLOW_MODE_SCALE : 1.0;
        double maxSpeed = Constants.Drive.MAX_SPEED_METERS_PER_SECOND;
        ChassisSpeeds speeds = new ChassisSpeeds(
                MathUtil.clamp(forward, -1.0, 1.0) * maxSpeed * scale,
                MathUtil.clamp(left, -1.0, 1.0) * maxSpeed * scale,
                MathUtil.clamp(turn, -1.0, 1.0)
                        * Constants.MAX_ANGULAR_SPEED_RADIANS_PER_SECOND * scale);

        SwerveModuleState[] states = kinematics.toSwerveModuleStates(speeds);
        SwerveDriveKinematics.desaturateWheelSpeeds(states, maxSpeed);
        for (int i = 0; i < modules.length; i++) {
            modules[i].setDesiredState(states[i]);
        }
    }

    public void stop() {
        for (SwerveModule module : modules) {
            module.stop();
        }
    }
}
