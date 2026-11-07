package frc.robot;

import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.TimedRobot;
import edu.wpi.first.wpilibj.XboxController;
import frc.robot.subsystems.SwerveDrive;

public class Robot extends TimedRobot {
    private XboxController controller;
    private SwerveDrive drive;

    @Override
    public void robotInit() {
        if (!Constants.Drive.isConfigured()) {
            DriverStation.reportError(
                    "Swerve movement disabled: configure hardware in Constants.java.",
                    false);
            return;
        }

        controller = new XboxController(Constants.DRIVER_CONTROLLER_PORT);
        drive = new SwerveDrive();
    }

    @Override
    public void teleopPeriodic() {
        if (drive == null || controller == null) {
            return;
        }

        double forward = -applyDeadband(controller.getLeftY());
        double left = -applyDeadband(controller.getLeftX());
        double turn = -applyDeadband(controller.getRightX());
        drive.drive(forward, left, turn, controller.getLeftBumper());
    }

    @Override
    public void disabledInit() {
        if (drive != null) {
            drive.stop();
        }
    }

    @Override
    public void teleopExit() {
        if (drive != null) {
            drive.stop();
        }
    }

    private double applyDeadband(double value) {
        return Math.abs(value) < Constants.JOYSTICK_DEADBAND ? 0.0 : value;
    }
}
