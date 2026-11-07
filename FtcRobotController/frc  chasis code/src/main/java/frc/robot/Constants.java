package frc.robot;

public final class Constants {
    private Constants() {}

    public static final int DRIVER_CONTROLLER_PORT = 0;
    public static final double JOYSTICK_DEADBAND = 0.08;
    public static final double MAX_ANGULAR_SPEED_RADIANS_PER_SECOND = 4.0;
    public static final double STEERING_KP = 0.8;
    public static final double MAX_STEERING_OUTPUT = 0.55;
    public static final double SLOW_MODE_SCALE = 0.35;

    public static final class Drive {
        private Drive() {}

        // Replace every placeholder with measurements from the actual robot.
        public static final double WHEELBASE_METERS = 0.0;
        public static final double TRACKWIDTH_METERS = 0.0;
        public static final double MAX_SPEED_METERS_PER_SECOND = 0.0;

        public static final Module FRONT_LEFT =
                new Module(-1, -1, Double.NaN, false, false);
        public static final Module FRONT_RIGHT =
                new Module(-1, -1, Double.NaN, false, false);
        public static final Module BACK_LEFT =
                new Module(-1, -1, Double.NaN, false, false);
        public static final Module BACK_RIGHT =
                new Module(-1, -1, Double.NaN, false, false);

        public static boolean isConfigured() {
            Module[] modules = {FRONT_LEFT, FRONT_RIGHT, BACK_LEFT, BACK_RIGHT};
            if (WHEELBASE_METERS <= 0.0 || TRACKWIDTH_METERS <= 0.0
                    || MAX_SPEED_METERS_PER_SECOND <= 0.0) {
                return false;
            }

            for (int i = 0; i < modules.length; i++) {
                if (!modules[i].isConfigured()) {
                    return false;
                }
                for (int j = i + 1; j < modules.length; j++) {
                    if (modules[i].driveCanId == modules[j].driveCanId
                            || modules[i].turnCanId == modules[j].turnCanId
                            || modules[i].driveCanId == modules[j].turnCanId
                            || modules[i].turnCanId == modules[j].driveCanId) {
                        return false;
                    }
                }
            }
            return true;
        }
    }

    public static final class Module {
        public final int driveCanId;
        public final int turnCanId;
        public final int encoderDioChannel;
        public final double encoderOffsetDegrees;
        public final boolean driveInverted;
        public final boolean turnInverted;

        public Module(int driveCanId, int turnCanId, double encoderOffsetDegrees,
                      boolean driveInverted, boolean turnInverted) {
            this(driveCanId, turnCanId, -1, encoderOffsetDegrees,
                    driveInverted, turnInverted);
        }

        public Module(int driveCanId, int turnCanId, int encoderDioChannel,
                      double encoderOffsetDegrees, boolean driveInverted,
                      boolean turnInverted) {
            this.driveCanId = driveCanId;
            this.turnCanId = turnCanId;
            this.encoderDioChannel = encoderDioChannel;
            this.encoderOffsetDegrees = encoderOffsetDegrees;
            this.driveInverted = driveInverted;
            this.turnInverted = turnInverted;
        }

        public boolean isConfigured() {
            return driveCanId >= 0 && turnCanId >= 0
                    && driveCanId != turnCanId
                    && encoderDioChannel >= 0
                    && Double.isFinite(encoderOffsetDegrees);
        }
    }
}
