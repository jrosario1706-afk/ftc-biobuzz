package org.firstinspires.ftc.teamcode;

// ============================================================================
//  VisionData -- lectura de vision lista para usar.
//  La llena BioBuzzVision y la consumen el cerebro de IA y la telemetria.
// ============================================================================

public class VisionData {

    /** Hay una deteccion aceptada (polen o nectar) por encima de la confianza. */
    public boolean tieneObjetivo = false;

    /** "POLEN", "NECTAR" o "Ninguno". */
    public String clase = "Ninguno";

    /** Angulo horizontal al objetivo en grados (+ = a la derecha). */
    public double tx = 0.0;

    /** Angulo vertical al objetivo en grados. */
    public double ty = 0.0;

    /** Confianza del detector, 0..1. */
    public double confianza = 0.0;

    /** Distancia horizontal estimada al objetivo, en centimetros. */
    public double distanciaCm = 0.0;

    /** Pose del robot segun los AprilTags (metros y grados). */
    public boolean tienePose = false;
    public double robotX = 0.0;
    public double robotY = 0.0;
    public double robotYaw = 0.0;

    /** Cuantos AprilTags ve y cuantas detecciones de cada clase hay. */
    public int aprilTags = 0;
    public int conteoPolen = 0;
    public int conteoNectar = 0;

    @Override
    public String toString() {
        return tieneObjetivo
                ? String.format("%s %.0f%% tx=%.1f d=%.0fcm", clase, confianza * 100,
                tx, distanciaCm)
                : "sin objetivo";
    }
}
