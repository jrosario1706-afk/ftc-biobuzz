package org.firstinspires.ftc.teamcode.hardware;

/** Un objeto detectado por la Limelight (polen, nectar, etc.). */
public class VisionTarget {
    public final String name;
    /** Grados horizontales: positivo = a la derecha del centro. */
    public final double tx;
    /** Grados verticales: positivo = arriba del centro. */
    public final double ty;
    /** Confianza 0..1. */
    public final double confidence;
    public final double area;

    /** Totales de la misma imagen (no solo de este objeto). */
    public int pollenCount;
    public int nectarCount;

    public VisionTarget(String name, double tx, double ty, double confidence, double area) {
        this.name = name;
        this.tx = tx;
        this.ty = ty;
        this.confidence = confidence;
        this.area = area;
    }

    public boolean isPollen() {
        return isPollen(name);
    }

    public boolean isNectar() {
        return isNectar(name);
    }

    /** Clases del modelo que cuentan como polen. */
    public static boolean isPollen(String lowerName) {
        return lowerName.contains("pollen") || lowerName.contains("polen")
                || lowerName.contains("yellow");
    }

    /** Clases del modelo que cuentan como nectar. */
    public static boolean isNectar(String lowerName) {
        return lowerName.contains("nectar") || lowerName.contains("n\u00e9ctar")
                || lowerName.contains("red") || lowerName.contains("blue");
    }
}
