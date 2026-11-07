package org.jhoandri.pathing;

import org.firstinspires.ftc.robotcore.external.navigation.AngleUnit;

public class BezierLine {
    private final Pose start;
    private final Pose end;

    public BezierLine(Pose start, Pose end) {
        this.start = start.copy();
        this.end = end.copy();
    }

    public Pose getStart() {
        return start.copy();
    }

    public Pose getEnd() {
        return end.copy();
    }

    /** Returns a point on the line and linearly interpolates the robot heading. */
    public Pose getPoint(double t) {
        double clampedT = Math.max(0.0, Math.min(1.0, t));
        double headingDelta = AngleUnit.normalizeRadians(end.heading - start.heading);
        return new Pose(
                start.x + (end.x - start.x) * clampedT,
                start.y + (end.y - start.y) * clampedT,
                AngleUnit.normalizeRadians(start.heading + headingDelta * clampedT)
        );
    }
}
