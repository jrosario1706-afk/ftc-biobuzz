package frc.robot.subsystems;

import com.revrobotics.spark.SparkLowLevel.MotorType;
import com.revrobotics.spark.SparkMax;
import edu.wpi.first.math.MathUtil;
import edu.wpi.first.math.geometry.Rotation2d;
import edu.wpi.first.math.kinematics.SwerveModuleState;
import edu.wpi.first.wpilibj.DutyCycleEncoder;
import frc.robot.Constants;

public final class SwerveModule {
    private final SparkMax driveMotor;
    private final SparkMax turnMotor;
    private final DutyCycleEncoder absoluteEncoder;
    private final double encoderOffsetDegrees;
    private final double maximumSpeed;

    public SwerveModule(Constants.Module config, double maximumSpeed) {
        driveMotor = new SparkMax(config.driveCanId, MotorType.kBrushless);
        turnMotor = new SparkMax(config.turnCanId, MotorType.kBrushless);
        absoluteEncoder = new DutyCycleEncoder(config.encoderDioChannel);
        absoluteEncoder.setInverted(false);
        encoderOffsetDegrees = config.encoderOffsetDegrees;
        this.maximumSpeed = maximumSpeed;

        driveMotor.setInverted(config.driveInverted);
        turnMotor.setInverted(config.turnInverted);
    }

    public Rotation2d getAngle() {
        return Rotation2d.fromRotations(absoluteEncoder.get())
                .minus(Rotation2d.fromDegrees(encoderOffsetDegrees));
    }

    public boolean isEncoderConnected() {
        return absoluteEncoder.isConnected();
    }

    public void setDesiredState(SwerveModuleState desiredState) {
        if (!absoluteEncoder.isConnected()) {
            stop();
            return;
        }

        SwerveModuleState optimized =
                SwerveModuleState.optimize(desiredState, getAngle());
        double angleError = optimized.angle.minus(getAngle()).getRadians();
        double turnOutput = MathUtil.clamp(
                angleError * Constants.STEERING_KP,
                -Constants.MAX_STEERING_OUTPUT,
                Constants.MAX_STEERING_OUTPUT);
        double driveOutput = MathUtil.clamp(
                optimized.speedMetersPerSecond / maximumSpeed, -1.0, 1.0);

        turnMotor.set(turnOutput);
        driveMotor.set(driveOutput);
    }

    public void stop() {
        driveMotor.stopMotor();
        turnMotor.stopMotor();
    }
}
