package org.firstinspires.ftc.teamcode;

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.hardware.HardwareMap;

import java.util.List;
import java.util.Locale;

/**
 * BioBuzzAI - Subsistema de visión Limelight 3A para detección neuronal de Polen.
 * Proporciona telemetría y coordenadas normalizadas para BioBuzzAIAuto.
 */
public class BioBuzzAI {

    private Limelight3A limelight;
    private boolean cameraAvailable = false;
    private boolean seesPollen = false;
    private double confidence = 0.0;
    private double targetXPixels = 320.0;
    private double targetArea = 0.0;

    public BioBuzzAI(HardwareMap hardwareMap) {
        try {
            limelight = hardwareMap.get(Limelight3A.class, "limelight");
            limelight.pipelineSwitch(BioBuzzConfig.PIPELINE_NEURAL_IA);
            cameraAvailable = true;
        } catch (Exception e) {
            limelight = null;
            cameraAvailable = false;
        }
    }

    public void start() {
        if (limelight != null && cameraAvailable) {
            try {
                limelight.start();
            } catch (Exception ignored) {
            }
        }
    }

    public void update() {
        seesPollen = false;
        confidence = 0.0;
        targetXPixels = 320.0;
        targetArea = 0.0;

        if (limelight == null || !cameraAvailable) return;

        try {
            LLResult result = limelight.getLatestResult();
            if (result == null || !result.isValid()) return;

            List<LLResultTypes.DetectorResult> detections = result.getDetectorResults();
            if (detections == null || detections.isEmpty()) return;

            double bestConfidence = 0.0;
            for (LLResultTypes.DetectorResult d : detections) {
                if (d == null) continue;
                String name = d.getClassName() == null ? "" : d.getClassName().toLowerCase(Locale.ROOT);
                boolean isPollen = name.contains("pollen") || name.contains("polen") || name.contains("yellow");
                if (!isPollen) continue;

                double rawConf = d.getConfidence();
                double normConf = rawConf > 1.0 ? rawConf : rawConf * 100.0; // Escala 0..100

                if (normConf > bestConfidence) {
                    bestConfidence = normConf;
                    seesPollen = true;
                    confidence = normConf;
                    targetXPixels = d.getTargetXPixels();
                    if (targetXPixels == 0.0 && d.getTargetXDegrees() != 0.0) {
                        // Limelight a veces entrega solo grados; interpolar a píxeles (campo FOV ~60 deg, ancho 640px)
                        targetXPixels = 320.0 + (d.getTargetXDegrees() / 30.0) * 320.0;
                    }
                    targetArea = d.getTargetArea();
                }
            }
        } catch (Exception ignored) {
            seesPollen = false;
        }
    }

    public boolean seesPollen() {
        return seesPollen;
    }

    public double getConfidence() {
        return confidence;
    }

    public double getX() {
        return targetXPixels;
    }

    public double getArea() {
        return targetArea;
    }

    public void stop() {
        if (limelight != null) {
            try {
                limelight.stop();
            } catch (Exception ignored) {
            }
        }
        cameraAvailable = false;
        seesPollen = false;
    }
}
