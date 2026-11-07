package org.firstinspires.ftc.teamcode;

// ============================================================================
//  BioBuzzBrain -- IA de decision a bordo.
//
//  Es un selector de comportamiento por UTILIDAD (utility-based):
//  cada estado posible calcula una puntuacion con las senales del robot
//  (vision, lanzador, tiempo de partida) y se ejecuta el de mayor puntaje,
//  con histeresis para no oscilar entre estados.
//
//  La misma logica esta implementada en Python (simulador.py) para poder
//  probarla en la laptop antes de subirla al robot.
// ============================================================================

import java.util.Locale;

/** Datos que el cerebro usa para decidir. */
class Contexto {

    public double tiempoPartida = 0.0;
    public double tiempoRestante = 120.0;
    public boolean tieneObjetivo = false;
    public boolean apuntado = false;
    public boolean lanzadorListo = false;
    public boolean disparando = false;
    public boolean tienePieza = false;
    public double distanciaCm = 0.0;
    public double tx = 0.0;
    public double confianza = 0.0;
    public double duracionPartida = 120.0;
}

/** Pesos configurables del selector de comportamiento. */
class Pesos {

    public double tengoPieza = 1.4;
    public double tengoObjetivo = 1.6;
    public double apuntado = 2.0;
    public double distanciaCorrecta = 1.8;
    public double lanzadorListo = 2.4;
    public double tiempoRestante = 0.8;
    public double sinObjetivo = 1.2;

    public double distanciaIdealCm = 180.0;
    public double margenDistanciaCm = 12.0;
    public double margenApuntadoGrados = 2.5;
    public double replegarEnUltimosS = 12.0;
}

public class BioBuzzBrain {

    /** Tiempo minimo en un estado antes de poder cambiarlo. */
    public static double HISTERESIS_S = 0.45;
    /** Margen de puntaje que debe sacar el estado nuevo para cambiarse. */
    public static double MARGEN_CAMBIO = 0.25;

    public enum Estado {
        BUSCAR,      // no hay objetivo: girar y barrer
        APUNTAR,     // objetivo descentrado: girar fino
        DISPARAR,    // centrado + lanzador a velocidad
        ACERCARSE,   // objetivo lejos
        ALEJARSE,    // objetivo demasiado cerca
        RECOGER,     // pieza a la vista, sin objetivo de tiro
        REPLEGAR,    // ultimos segundos: asegurar posicion
        MANUAL       // el driver tiene el control
    }

    private static final String[] ETIQUETAS = {
            "BUSCAR", "APUNTAR", "DISPARAR", "ACERCARSE", "ALEJARSE",
            "RECOGER", "REPLEGAR", "MANUAL"
    };

    private final Pesos pesos;
    private Estado estado = Estado.BUSCAR;
    private Estado anterior = Estado.BUSCAR;
    private double tiempoEnEstado = 0.0;
    private double historialTiempo = 0.0;

    // puntajes del ultimo ciclo (para telemetria y depuracion)
    private final double[] puntajes = new double[Estado.values().length];

    public BioBuzzBrain(Pesos pesos) {
        this.pesos = pesos == null ? new Pesos() : pesos;
    }

    public Pesos pesos() {
        return pesos;
    }

    public Estado estado() {
        return estado;
    }

    public double tiempoEnEstado() {
        return tiempoEnEstado;
    }

    public double puntaje(Estado e) {
        return puntajes[e.ordinal()];
    }

    public void reiniciar() {
        estado = Estado.BUSCAR;
        anterior = Estado.BUSCAR;
        tiempoEnEstado = 0.0;
        historialTiempo = 0.0;
    }

    /** Fuerza el modo manual (el driver manda). */
    public void modoManual(boolean activo) {
        if (activo) {
            cambiar(Estado.MANUAL);
        }
    }

    /**
     * Calcula el nuevo estado. Se llama una vez por ciclo del OpMode.
     *
     * @param tiempoPartida segundos desde el inicio del OpMode
     * @param vision        datos de la camara
     * @param lanzadorListo el shooter ya esta a velocidad
     * @param tieneObjetivo hay deteccion valida
     * @param apuntado      el objetivo esta dentro de la tolerancia
     * @param distanciaCm   distancia estimada al objetivo
     * @param disparando    el operador pidio disparar
     */
    public Estado actualizar(double tiempoPartida, VisionData vision, boolean lanzadorListo,
                             boolean tieneObjetivo, boolean apuntado, double distanciaCm,
                             boolean disparando) {

        double dt = Math.max(0.0, tiempoPartida - historialTiempo);
        historialTiempo = tiempoPartida;
        tiempoEnEstado += dt;

        Contexto c = new Contexto();
        c.tiempoPartida = tiempoPartida;
        c.duracionPartida = BioBuzzConfig.DURACION_PARTIDA_S;
        c.tiempoRestante = Math.max(0.0, c.duracionPartida - tiempoPartida);
        c.tieneObjetivo = tieneObjetivo;
        c.apuntado = apuntado;
        c.lanzadorListo = lanzadorListo;
        c.disparando = disparando;
        c.distanciaCm = distanciaCm;
        c.tx = vision == null ? 0.0 : vision.tx;
        c.confianza = vision == null ? 0.0 : vision.confianza;

        double[] p = evaluar(c);
        System.arraycopy(p, 0, puntajes, 0, p.length);

        int mejor = 0;
        for (int i = 1; i < p.length; i++) {
            if (p[i] > p[mejor]) {
                mejor = i;
            }
        }
        Estado elegido = Estado.values()[mejor];

        // histeresis: un estado nuevo debe ganar por un margen claro y ademas
        // respetar un tiempo minimo en el estado actual (evita el parloteo).
        boolean recienCambiado = tiempoEnEstado < HISTERESIS_S;
        if (recienCambiado && elegido != estado) {
            elegido = estado;
        } else if (elegido != estado
                && p[mejor] < puntajes[estado.ordinal()] + MARGEN_CAMBIO) {
            elegido = estado;
        }
        if (elegido != estado) {
            anterior = estado;
            tiempoEnEstado = 0.0;
            estado = elegido;
        }
        return estado;
    }

    private double[] evaluar(Contexto c) {
        int n = Estado.values().length;
        double[] p = new double[n];

        boolean lejos = c.tieneObjetivo
                && c.distanciaCm > pesos.distanciaIdealCm + pesos.margenDistanciaCm;
        boolean cerca = c.tieneObjetivo && c.distanciaCm > 0.0
                && c.distanciaCm < pesos.distanciaIdealCm - pesos.margenDistanciaCm;
        boolean distanciaOk = c.tieneObjetivo && !lejos && !cerca;
        boolean fino = c.tieneObjetivo
                && Math.abs(c.tx) <= pesos.margenApuntadoGrados;
        boolean finalPartida = c.tiempoRestante <= pesos.replegarEnUltimosS;

        // Proximidad a la distancia ideal: 1 en el centro, decae con el error.
        // Sin este termino la IA dispara a cualquier distancia, que es el fallo
        // que detecto el simulador (disparaba a 320 cm como si fueran 180).
        double errorDist = c.tieneObjetivo
                ? Math.abs(c.distanciaCm - pesos.distanciaIdealCm)
                : 1e6;
        double proxDist = Math.max(0.0, 1.0 - errorDist / 140.0);

        // --- BUSCAR: no hay objetivo -------------------------------------
        p[Estado.BUSCAR.ordinal()] = c.tieneObjetivo
                ? 0.10
                : pesos.sinObjetivo;

        // --- APUNTAR: hay objetivo pero descentrado ----------------------
        p[Estado.APUNTAR.ordinal()] = c.tieneObjetivo
                ? pesos.tengoObjetivo * (fino ? 0.25 : 1.0)
                  + pesos.apuntado * Math.min(1.0, Math.abs(c.tx) / 12.0)
                : 0.0;

        // --- DISPARAR: centrado + distancia correcta + lanzador listo ----
        p[Estado.DISPARAR.ordinal()] = c.tieneObjetivo
                ? pesos.tengoObjetivo * (fino ? 1.0 : 0.15)
                  + pesos.distanciaCorrecta * proxDist
                  + pesos.lanzadorListo * (c.lanzadorListo ? 1.0 : 0.0)
                  + pesos.tiempoRestante
                        * (1.0 - c.tiempoRestante / Math.max(1.0, c.duracionPartida))
                  + (c.disparando ? 0.8 : 0.0)
                : 0.0;

        // --- ACERCARSE / ALEJARSE ----------------------------------------
        // Cuanto peor es la distancia, mas urge corregirla.
        p[Estado.ACERCARSE.ordinal()] = lejos
                ? pesos.distanciaCorrecta + 0.6 + (1.0 - proxDist) : 0.0;
        p[Estado.ALEJARSE.ordinal()] = cerca
                ? pesos.distanciaCorrecta + 0.4 + (1.0 - proxDist) : 0.0;

        // --- RECOGER: sin objetivo de tiro pero hay pieza ----------------
        p[Estado.RECOGER.ordinal()] = (!c.tieneObjetivo && c.tienePieza)
                ? pesos.tengoPieza : 0.0;

        // --- REPLEGAR: final de partida ----------------------------------
        p[Estado.REPLEGAR.ordinal()] = finalPartida ? 1.15 + pesos.tiempoRestante : 0.0;

        // --- MANUAL: puntaje base bajo, se activa por boton --------------
        p[Estado.MANUAL.ordinal()] = 0.05;

        return p;
    }

    private void cambiar(Estado nuevo) {
        if (nuevo != estado) {
            anterior = estado;
            estado = nuevo;
            tiempoEnEstado = 0.0;
        }
    }

    public String resumen() {
        return String.format(Locale.US, "%s (%.1fs) p=%.2f", ETIQUETAS[estado.ordinal()],
                tiempoEnEstado, puntajes[estado.ordinal()]);
    }

    public String tablaPuntajes() {
        StringBuilder sb = new StringBuilder();
        for (Estado e : Estado.values()) {
            sb.append(String.format(Locale.US, "%s=%.2f ", ETIQUETAS[e.ordinal()],
                    puntajes[e.ordinal()]));
        }
        return sb.toString().trim();
    }
}
