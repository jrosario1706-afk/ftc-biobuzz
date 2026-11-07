
package org.jhoandri.pathing.Vision;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.hardware.HardwareMap;

import org.firstinspires.ftc.teamcode.hardware.RobotConfig;
import org.firstinspires.ftc.teamcode.hardware.RobotHardware;
import org.firstinspires.ftc.teamcode.hardware.VisionTarget;
import org.jhoandri.pathing.Pose;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

public class JhoandriVision {

    private Limelight3A limelight;
    private Pose robotPose;

    private String targetClassName = "pollen";
    // Escala 0..100. CORRECCION: antes era 70 fijo pero la Limelight entrega la
    // confianza en 0..1, asi que NINGUNA deteccion pasaba el filtro.
    private double minimumConfidence = RobotConfig.MIN_DETECTION_CONFIDENCE * 100.0;

    private boolean cameraAvailable;
    private Detection bestPollen;
    private final List<Detection> detections = new ArrayList<>();

    public JhoandriVision(HardwareMap hardwareMap) {
        try {
            limelight = hardwareMap.get(
                    Limelight3A.class, "limelight"
            );

            limelight.pipelineSwitch(0);
            limelight.start();
            cameraAvailable = true;

        } catch (Exception e) {
            limelight = null;
            cameraAvailable = false;
        }
    }

    public void updateDetections(Pose currentPose) {
        robotPose = currentPose == null
                ? null : currentPose.copy();

        detections.clear();
        bestPollen = null;

        if (limelight == null) {
            cameraAvailable = false;
            return;
        }

        try {
            LLResult result = limelight.getLatestResult();

            if (result == null || !result.isValid()) {
                return;
            }

            List<LLResultTypes.DetectorResult> results =
                    result.getDetectorResults();

            if (results == null) {
                return;
            }

            for (LLResultTypes.DetectorResult item : results) {
                if (item == null) {
                    continue;
                }

                String className = item.getClassName();

                if (className == null || !matchesTarget(className)) {
                    continue;
                }

                double confidence =
                        RobotHardware.normalizeConfidence(item.getConfidence()) * 100.0;

                if (!Double.isFinite(confidence)
                        || confidence < minimumConfidence) {
                    continue;
                }

                Detection detection = new Detection(
                        className,
                        item.getClassId(),
                        confidence,
                        item.getTargetArea(),
                        item.getTargetXPixels(),
                        item.getTargetYPixels(),
                        item.getTargetXDegrees(),
                        item.getTargetYDegrees(),
                        item.getTargetCorners()
                );

                detections.add(detection);

                if (bestPollen == null ||
                        detection.getConfidence() >
                                bestPollen.getConfidence()) {
                    bestPollen = detection;
                }
            }

        } catch (Exception e) {
            detections.clear();
            bestPollen = null;
        }
    }

    /** Acepta variantes del nombre de clase: pollen, Yellow Pollen, polen... */
    private boolean matchesTarget(String className) {
        String name = className.trim().toLowerCase(Locale.ROOT);
        String target = targetClassName.toLowerCase(Locale.ROOT);
        if (name.contains(target)) {
            return true;
        }
        return (target.equals("pollen") || target.equals("polen"))
                && VisionTarget.isPollen(name);
    }

    public Detection findYellowWithBox() {
        return bestPollen;
    }

    public Detection getBestPollen() {
        return bestPollen;
    }

    public List<Detection> getDetections() {
        return Collections.unmodifiableList(
                new ArrayList<>(detections)
        );
    }

    public boolean isTargetVisible() {
        return bestPollen != null;
    }

    public boolean isCameraAvailable() {
        return cameraAvailable;
    }

    public Pose getRobotPose() {
        return robotPose == null ? null : robotPose.copy();
    }

    public LLResult getLatestResult() {
        if (limelight == null) {
            return null;
        }

        try {
            return limelight.getLatestResult();
        } catch (Exception e) {
            return null;
        }
    }

    public void setTargetClassName(String className) {
        if (className != null && !className.trim().isEmpty()) {
            targetClassName = className.trim();
        }
    }

    public void setMinimumConfidence(double confidence) {
        if (Double.isFinite(confidence)) {
            minimumConfidence = Math.max(
                    0.0, Math.min(100.0, confidence)
            );
        }
    }

    public void setPipeline(int pipelineIndex) {
        if (limelight != null && pipelineIndex >= 0) {
            limelight.pipelineSwitch(pipelineIndex);
        }
    }

    public void stop() {
        if (limelight != null) {
            try {
                limelight.stop();
            } catch (Exception ignored) {
                // No impedir la parada del robot por un error de cámara.
            }
        }

        cameraAvailable = false;
        bestPollen = null;
        detections.clear();
    }

    public static class Detection {

        private final String className;
        private final int classId;
        private final double confidence;
        private final double area;
        private final double targetXPixels;
        private final double targetYPixels;
        private final double targetXDegrees;
        private final double targetYDegrees;

        private final double left;
        private final double top;
        private final double right;
        private final double bottom;
        private final boolean hasBoundingBox;

        Detection(
                String className,
                int classId,
                double confidence,
                double area,
                double targetXPixels,
                double targetYPixels,
                double targetXDegrees,
                double targetYDegrees,
                List<List<Double>> corners) {

            this.className = className;
            this.classId = classId;
            this.confidence = confidence;
            this.area = area;
            this.targetXPixels = targetXPixels;
            this.targetYPixels = targetYPixels;
            this.targetXDegrees = targetXDegrees;
            this.targetYDegrees = targetYDegrees;

            double minX = Double.POSITIVE_INFINITY;
            double minY = Double.POSITIVE_INFINITY;
            double maxX = Double.NEGATIVE_INFINITY;
            double maxY = Double.NEGATIVE_INFINITY;
            int validCorners = 0;

            if (corners != null) {
                for (List<Double> point : corners) {
                    if (point == null || point.size() < 2 ||
                            point.get(0) == null ||
                            point.get(1) == null) {
                        continue;
                    }

                    double x = point.get(0);
                    double y = point.get(1);

                    if (!Double.isFinite(x) ||
                            !Double.isFinite(y)) {
                        continue;
                    }

                    minX = Math.min(minX, x);
                    minY = Math.min(minY, y);
                    maxX = Math.max(maxX, x);
                    maxY = Math.max(maxY, y);
                    validCorners++;
                }
            }

            hasBoundingBox = validCorners >= 2;

            left = hasBoundingBox ? minX : Double.NaN;
            top = hasBoundingBox ? minY : Double.NaN;
            right = hasBoundingBox ? maxX : Double.NaN;
            bottom = hasBoundingBox ? maxY : Double.NaN;
        }

        public String getClassName() {
            return className;
        }

        public int getClassId() {
            return classId;
        }

        public double getConfidence() {
            return confidence;
        }

        public double getArea() {
            return area;
        }

        public double getTargetXPixels() {
            return targetXPixels;
        }

        public double getTargetYPixels() {
            return targetYPixels;
        }

        public double getTargetXDegrees() {
            return targetXDegrees;
        }

        public double getTargetYDegrees() {
            return targetYDegrees;
        }

        public boolean hasBoundingBox() {
            return hasBoundingBox;
        }

        public double getLeft() {
            return left;
        }

        public double getTop() {
            return top;
        }

        public double getRight() {
            return right;
        }

        public double getBottom() {
            return bottom;
        }

        public double getBoxWidth() {
            return hasBoundingBox ? right - left : Double.NaN;
        }

        public double getBoxHeight() {
            return hasBoundingBox ? bottom - top : Double.NaN;
        }

        public double getCenterX() {
            return hasBoundingBox ? (left + right) / 2.0 : Double.NaN;
        }

        public double getCenterY() {
            return hasBoundingBox ? (top + bottom) / 2.0 : Double.NaN;
        }
    }
}
