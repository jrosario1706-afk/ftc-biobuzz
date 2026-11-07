package org.firstinspires.ftc.teamcode;

// ============================================================================
//  BioBuzzVision -- lectura y filtrado avanzado de Limelight 3A.
//  Incluye filtro pasa-bajos exponencial, verificacion de staleness (edad del frame),
//  suavizado de distancia y contador de estabilidad de apuntado (Target Lock).
// ============================================================================

import com.qualcomm.hardware.limelightvision.LLResult;
import com.qualcomm.hardware.limelightvision.LLResultTypes;
import com.qualcomm.hardware.limelightvision.Limelight3A;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.Telemetry;
import org.firstinspires.ftc.robotcore.external.navigation.Pose3D;

import java.util.List;
import java.util.Locale;

public class BioBuzzVision {

    private final HardwareMap hardwareMap;
    private final Telemetry telemetry;
    private final String nombreHardware;

    private Limelight3A limelight;
    private boolean disponible = false;
    private boolean usandoNeural = true;
    private String ultimoError = "";

    private VisionData ultimo = new VisionData();
    private String targetFilter = "TODOS";
    private int stalenessCount = 0;

    // Filtros de suavizado
    private double suavizadoTx = 0.0;
    private double suavizadoTy = 0.0;
    private double suavizadoDistancia = 0.0;
    private boolean primerDato = true;
    private int cuadrosEstables = 0;

    public BioBuzzVision(HardwareMap hardwareMap, Telemetry telemetry, String nombreHardware) {
        this.hardwareMap = hardwareMap;
        this.telemetry = telemetry;
        this.nombreHardware = nombreHardware;
    }

    public void iniciar(int pipelineNeural) {
        try {
            limelight = hardwareMap.get(Limelight3A.class, nombreHardware);
            limelight.pipelineSwitch(pipelineNeural);
            limelight.start();
            disponible = true;
            usandoNeural = true;
        } catch (Exception e) {
            disponible = false;
            ultimoError = e.getClass().getSimpleName() + ": " + e.getMessage();
            if (telemetry != null) {
                telemetry.addData("Limelight", "no disponible (" + ultimoError + ")");
            }
        }
    }

    public boolean disponible() {
        return disponible;
    }

    public String estado() {
        return disponible ? (usandoNeural ? "neural" : "color") : "sin camara";
    }

    public void detener() {
        parar();
    }

    public void alternarPipeline(int neural, int color) {
        if (!disponible) {
            return;
        }
        usandoNeural = !usandoNeural;
        try {
            limelight.pipelineSwitch(usandoNeural ? neural : color);
            primerDato = true;
            cuadrosEstables = 0;
        } catch (Exception ignored) {
        }
    }

    public void fijarPipeline(int pipeline) {
        if (!disponible) {
            return;
        }
        try {
            limelight.pipelineSwitch(pipeline);
            primerDato = true;
            cuadrosEstables = 0;
        } catch (Exception ignored) {
        }
    }

    public boolean usandoNeural() {
        return usandoNeural;
    }

    public void setTargetFilter(String filter) {
        this.targetFilter = filter != null ? filter.toUpperCase(Locale.ROOT) : "TODOS";
    }

    public String getTargetFilter() {
        return targetFilter;
    }

    /** Normaliza la confianza a escala 0..1 aunque la Limelight devuelva 0..100. */
    public static double normalizarConfianza(double c) {
        if (!Double.isFinite(c)) return 0.0;
        return c > 1.0 ? c / 100.0 : c;
    }

    /** Lee la camara y aplica filtrado exponencial y validacion de frescura de datos. */
    public VisionData leer(double confianzaMinima) {
        VisionData datos = new VisionData();
        if (!disponible || limelight == null) {
            ultimo = datos;
            primerDato = true;
            cuadrosEstables = 0;
            stalenessCount = 0;
            return datos;
        }
        try {
            LLResult resultado = limelight.getLatestResult();
            if (resultado == null || !resultado.isValid()) {
                ultimo = datos;
                primerDato = true;
                cuadrosEstables = 0;
                stalenessCount = 0;
                return datos;
            }

            // Validar staleness: descartar frames viejos mayores a 250ms
            if (resultado.getStaleness() > 250) {
                stalenessCount++;
                if (stalenessCount > 4) {
                    // Mas de 4 frames consecutivos congelados -> se perdio el objetivo
                    ultimo = datos;
                    primerDato = true;
                    cuadrosEstables = 0;
                    return datos;
                }
                return ultimo; // Mantener brevemente la prediccion para no saltar abruptamente
            }
            stalenessCount = 0;

            double umbral = normalizarConfianza(confianzaMinima);
            List<LLResultTypes.DetectorResult> detecciones = resultado.getDetectorResults();
            if (detecciones != null && !detecciones.isEmpty()) {
                double mejorCentrado = Double.MAX_VALUE;
                for (LLResultTypes.DetectorResult d : detecciones) {
                    if (d == null) continue;
                    double confianza = normalizarConfianza(d.getConfidence());
                    String nombre = d.getClassName() == null
                            ? "" : d.getClassName().toLowerCase(Locale.ROOT);
                    boolean esPolen = nombre.contains("pollen") || nombre.contains("polen")
                            || nombre.contains("yellow");
                    boolean esNectar = nombre.contains("nectar") || nombre.contains("red")
                            || nombre.contains("blue");
                    if (esPolen) {
                        datos.conteoPolen++;
                    }
                    if (esNectar) {
                        datos.conteoNectar++;
                    }

                    boolean aceptado;
                    if ("POLEN".equals(targetFilter)) {
                        aceptado = esPolen;
                    } else if ("NECTAR".equals(targetFilter)) {
                        aceptado = esNectar;
                    } else {
                        aceptado = esPolen || esNectar;
                    }

                    if (confianza < umbral || !aceptado) {
                        continue;
                    }
                    double tx = d.getTargetXDegrees();
                    double ty = d.getTargetYDegrees();
                    double centrado = Math.abs(tx) + 0.25 * Math.abs(ty);
                    if (centrado < mejorCentrado) {
                        mejorCentrado = centrado;
                        datos.tieneObjetivo = true;
                        datos.clase = esPolen ? "POLEN" : "NECTAR";
                        datos.confianza = confianza;

                        // Filtro pasa-bajos exponencial para mitigar jitter
                        double alpha = BioBuzzConfig.VISION_SMOOTHING_ALPHA;
                        if (primerDato) {
                            suavizadoTx = tx;
                            suavizadoTy = ty;
                            primerDato = false;
                        } else {
                            suavizadoTx = alpha * tx + (1.0 - alpha) * suavizadoTx;
                            suavizadoTy = alpha * ty + (1.0 - alpha) * suavizadoTy;
                        }

                        datos.tx = suavizadoTx;
                        datos.ty = suavizadoTy;
                    }
                }
            } else if (!usandoNeural && resultado.getTa() > 0.05) {
                // Pipeline clasico de contornos / color
                double tx = resultado.getTx();
                double ty = resultado.getTy();
                datos.tieneObjetivo = true;
                datos.clase = "COLOR";
                datos.confianza = 0.90;

                double alpha = BioBuzzConfig.VISION_SMOOTHING_ALPHA;
                if (primerDato) {
                    suavizadoTx = tx;
                    suavizadoTy = ty;
                    primerDato = false;
                } else {
                    suavizadoTx = alpha * tx + (1.0 - alpha) * suavizadoTx;
                    suavizadoTy = alpha * ty + (1.0 - alpha) * suavizadoTy;
                }
                datos.tx = suavizadoTx;
                datos.ty = suavizadoTy;
            }

            // Pose 3D y MegaTag2 (AprilTags)
            Pose3D pose = resultado.getBotpose();
            if (pose != null) {
                datos.tienePose = true;
                datos.robotX = pose.getPosition().x;
                datos.robotY = pose.getPosition().y;
                datos.robotYaw = pose.getOrientation().getYaw();
            }

            List<LLResultTypes.FiducialResult> fiduciales = resultado.getFiducialResults();
            if (fiduciales != null) {
                datos.aprilTags = fiduciales.size();
            }

            if (datos.tieneObjetivo) {
                double rawDist = distanciaPorAngulo(datos.ty);
                if (suavizadoDistancia <= 0.0) {
                    suavizadoDistancia = rawDist;
                } else {
                    suavizadoDistancia = 0.70 * rawDist + 0.30 * suavizadoDistancia;
                }
                datos.distanciaCm = suavizadoDistancia;

                if (Math.abs(datos.tx) <= BioBuzzConfig.AUTO_AIM_TOLERANCE) {
                    cuadrosEstables = Math.min(20, cuadrosEstables + 1);
                } else {
                    cuadrosEstables = Math.max(0, cuadrosEstables - 1);
                }
            } else {
                primerDato = true;
                cuadrosEstables = 0;
            }
        } catch (Exception e) {
            ultimoError = e.getClass().getSimpleName();
        }
        ultimo = datos;
        return datos;
    }

    /**
     * Distancia horizontal calculada por trigonometria usando la inclinacion ty.
     * d = (h_objetivo - h_camara) / tan(angulo_montaje + ty)
     */
    public static double distanciaPorAngulo(double ty) {
        double anguloDeg = BioBuzzConfig.LIMELIGHT_MOUNT_ANGLE_DEG + ty;
        if (anguloDeg <= 1.5) {
            return 0.0;
        }
        double angulo = Math.toRadians(anguloDeg);
        double tangente = Math.tan(angulo);
        if (tangente < 0.05) {
            return 0.0;
        }
        double deltaH = BioBuzzConfig.TARGET_HEIGHT_CM - BioBuzzConfig.LIMELIGHT_HEIGHT_CM;
        if (deltaH <= 0.0) {
            return 0.0;
        }
        double d = deltaH / tangente;
        return Range.clip(d, 0.0, 500.0);
    }

    public boolean estaApuntado(double toleranciaGrados) {
        return ultimo.tieneObjetivo && Math.abs(ultimo.tx) <= toleranciaGrados;
    }

    /** true si el objetivo ha permanecido en el centro por varios cuadros continuos */
    public boolean targetBloqueado() {
        return ultimo.tieneObjetivo && cuadrosEstables >= 3;
    }

    public VisionData ultimo() {
        return ultimo;
    }

    public String textoTelemetria() {
        if (!disponible) {
            return "sin camara";
        }
        if (!ultimo.tieneObjetivo) {
            return "buscando (" + estado() + ")";
        }
        return String.format(Locale.US, "%s %.0f%% tx %.1f° d %.0fcm %s",
                ultimo.clase, ultimo.confianza * 100, ultimo.tx, ultimo.distanciaCm,
                targetBloqueado() ? "[LOCKED]" : "");
    }

    public void parar() {
        if (limelight != null) {
            try {
                limelight.stop();
            } catch (Exception ignored) {
            }
        }
    }
}
