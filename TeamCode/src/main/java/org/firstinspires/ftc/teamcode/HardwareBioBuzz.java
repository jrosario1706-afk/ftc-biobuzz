package org.firstinspires.ftc.teamcode;

// ============================================================================
//  HardwareBioBuzz -- capa de hardware compartida por todos los OpModes.
//  Concentra el acceso a motores, intake, transfer, lanzador, Limelight,
//  controles y el cerebro de decision, para que cada OpMode sea corto y legible.
//
//  Ejes del chasis (mecanum):
//      +Y = avanzar        +X = desplazar a la derecha
//      +giro = girar a la derecha (sentido horario visto desde arriba)
// ============================================================================

import com.qualcomm.robotcore.hardware.DcMotor;
import com.qualcomm.robotcore.hardware.DcMotorEx;
import com.qualcomm.robotcore.hardware.DcMotorSimple;
import com.qualcomm.robotcore.hardware.Gamepad;
import com.qualcomm.robotcore.hardware.HardwareMap;
import com.qualcomm.robotcore.util.ElapsedTime;
import com.qualcomm.robotcore.util.Range;

import org.firstinspires.ftc.robotcore.external.Telemetry;

public class HardwareBioBuzz {

    // ------------------------------------------------------------------
    // Nombres de hardware (soporta los nombres principales y alternativos)
    // ------------------------------------------------------------------
    public static final String NOMBRE_FRONT_LEFT = "frontLeft";
    public static final String NOMBRE_FRONT_RIGHT = "frontRight";
    public static final String NOMBRE_BACK_LEFT = "backLeft";
    public static final String NOMBRE_BACK_RIGHT = "backRight";
    public static final String NOMBRE_INTAKE = "intake";
    public static final String NOMBRE_SHOOTER = "shooter";
    public static final String NOMBRE_TRANSFER = "transferMotor";
    public static final String NOMBRE_LIMELIGHT = "limelight";

    /** Grados por segundo maximos del chasis (para el lazo de apuntado). */
    public static double VELOCIDAD_GIRO_MAX_DEG_S = 220.0;

    // ------------------------------------------------------------------
    // Hardware
    // ------------------------------------------------------------------
    private final HardwareMap hardwareMap;
    private final Telemetry telemetry;

    public DcMotor motorFl;
    public DcMotor motorFr;
    public DcMotor motorBl;
    public DcMotor motorBr;
    public DcMotor intake;
    public DcMotor transfer;
    public DcMotorEx shooter;

    public BioBuzzVision vision;

    // ------------------------------------------------------------------
    // Estado
    // ------------------------------------------------------------------
    private Gamepad pad1;
    private Gamepad pad2;

    private final ElapsedTime reloj = new ElapsedTime();
    private final ElapsedTime relojPaso = new ElapsedTime();
    private ElapsedTime relojRampa;

    private int pasoActual = 0;
    private boolean pasoEnCurso = false;
    private double pasoPotencia = 0.0;
    private long pasoDuracionMs = 0;

    private double potenciaLanzador = 0.0;
    private double objetivoLanzador = 0.0;

    private boolean forceShoot = false;
    private boolean yAnterior = false;

    // Control de aceleracion y suavizado
    private double lastFl = 0.0, lastBl = 0.0, lastFr = 0.0, lastBr = 0.0;
    private final ElapsedTime relojChasis = new ElapsedTime();
    private double ultimoErrorAim = 0.0;
    private double ultimaDerivadaAim = 0.0;
    private boolean tieneErrorPrevioAim = false;
    private final ElapsedTime relojAim = new ElapsedTime();
    private boolean objetivoBloqueadoPrevio = false;

    // Diagnostico de loop y bateria
    private com.qualcomm.robotcore.hardware.VoltageSensor sensorVoltaje;
    private final ElapsedTime relojLoop = new ElapsedTime();
    private double frecuenciaLoopHz = 0.0;

    public BioBuzzBrain cerebro;

    // ------------------------------------------------------------------
    // Construccion e inicializacion
    // ------------------------------------------------------------------
    public HardwareBioBuzz(HardwareMap hardwareMap, Telemetry telemetry) {
        this.hardwareMap = hardwareMap;
        this.telemetry = telemetry;
    }

    /** Deja el robot listo para operar. Falla rapido y claro si falta algo. */
    public void inicializar() {
        obtenerHardware();
        configurarHardware();
        vision = new BioBuzzVision(hardwareMap, telemetry, NOMBRE_LIMELIGHT);
        vision.iniciar(BioBuzzConfig.PIPELINE_NEURAL_IA);
        cerebro = new BioBuzzBrain(BioBuzzConfig.pesosCerebro());
        reloj.reset();
    }

    /**
     * Version tolerante a fallos: intenta cargar todo y devuelve un informe
     * con los dispositivos que no respondieron.
     */
    public String inicializarTolerante() {
        StringBuilder informe = new StringBuilder();
        motorFl = intentar(DcMotor.class, new String[]{"frontLeft", "leftFront"}, informe);
        motorFr = intentar(DcMotor.class, new String[]{"frontRight", "rightFront"}, informe);
        motorBl = intentar(DcMotor.class, new String[]{"backLeft", "leftBack", "leftRear"}, informe);
        motorBr = intentar(DcMotor.class, new String[]{"backRight", "rightBack", "rightRear"}, informe);
        intake = intentar(DcMotor.class, new String[]{"intake", "intakeMotor"}, informe);
        shooter = intentar(DcMotorEx.class, new String[]{"shooter", "shooterMotor", "flywheel"}, informe);
        transfer = intentarOpcional(DcMotor.class, "transferMotor", "transfer", "feederMotor");
        return informe.length() == 0 ? "todo el hardware respondio" : informe.toString();
    }

    private <T> T intentar(Class<? extends T> clase, String[] nombres, StringBuilder informe) {
        for (String nombre : nombres) {
            try {
                return hardwareMap.get(clase, nombre);
            } catch (Exception ignored) {
            }
        }
        informe.append("FALTA ").append(nombres[0]).append("\n");
        return null;
    }

    private void obtenerHardware() {
        motorFl = obtenerRequerido(DcMotor.class, "frontLeft", "leftFront");
        motorFr = obtenerRequerido(DcMotor.class, "frontRight", "rightFront");
        motorBl = obtenerRequerido(DcMotor.class, "backLeft", "leftBack", "leftRear");
        motorBr = obtenerRequerido(DcMotor.class, "backRight", "rightBack", "rightRear");
        intake = intentarOpcional(DcMotor.class, "intake", "intakeMotor", "intake1");
        transfer = intentarOpcional(DcMotor.class, "transferMotor", "transfer", "feederMotor");
        shooter = intentarOpcional(DcMotorEx.class, "shooter", "shooterMotor", "flywheel");

        for (com.qualcomm.robotcore.hardware.VoltageSensor vs : hardwareMap.voltageSensor) {
            sensorVoltaje = vs;
            break;
        }
    }

    private <T> T obtenerRequerido(Class<? extends T> clase, String... nombres) {
        for (String n : nombres) {
            try {
                return hardwareMap.get(clase, n);
            } catch (Exception ignored) {
            }
        }
        return hardwareMap.get(clase, nombres[0]);
    }

    private <T> T intentarOpcional(Class<? extends T> clase, String... nombres) {
        for (String n : nombres) {
            try {
                return hardwareMap.get(clase, n);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private void configurarHardware() {
        if (motorFl != null) {
            motorFl.setDirection(DcMotorSimple.Direction.REVERSE);
            motorFl.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            motorFl.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }
        if (motorBl != null) {
            motorBl.setDirection(DcMotorSimple.Direction.REVERSE);
            motorBl.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            motorBl.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }
        if (motorFr != null) {
            motorFr.setDirection(DcMotorSimple.Direction.FORWARD);
            motorFr.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            motorFr.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }
        if (motorBr != null) {
            motorBr.setDirection(DcMotorSimple.Direction.FORWARD);
            motorBr.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            motorBr.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }

        if (intake != null) {
            intake.setDirection(DcMotorSimple.Direction.FORWARD);
            intake.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            intake.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }
        if (transfer != null) {
            transfer.setDirection(DcMotorSimple.Direction.FORWARD);
            transfer.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.BRAKE);
            transfer.setMode(DcMotor.RunMode.RUN_WITHOUT_ENCODER);
        }
        if (shooter != null) {
            shooter.setDirection(DcMotorSimple.Direction.FORWARD);
            shooter.setZeroPowerBehavior(DcMotor.ZeroPowerBehavior.FLOAT);
            shooter.setMode(DcMotor.RunMode.STOP_AND_RESET_ENCODER);
            shooter.setMode(DcMotor.RunMode.RUN_USING_ENCODER);
            try {
                com.qualcomm.robotcore.hardware.PIDFCoefficients pidf =
                        new com.qualcomm.robotcore.hardware.PIDFCoefficients(
                                BioBuzzConfig.SHOOTER_PIDF_P,
                                BioBuzzConfig.SHOOTER_PIDF_I,
                                BioBuzzConfig.SHOOTER_PIDF_D,
                                BioBuzzConfig.SHOOTER_PIDF_F
                        );
                shooter.setPIDFCoefficients(DcMotor.RunMode.RUN_USING_ENCODER, pidf);
            } catch (Exception ignored) {
            }
        }
    }

    public void enlazarGamepads(Gamepad gamepad1, Gamepad gamepad2) {
        this.pad1 = gamepad1;
        this.pad2 = gamepad2;
    }

    public String resumenHardware() {
        StringBuilder sb = new StringBuilder();
        sb.append(motorFl != null && motorFr != null && motorBl != null && motorBr != null
                ? "chasis ok" : "chasis INCOMPLETO");
        sb.append(" | intake ").append(intake != null ? "ok" : "NO");
        sb.append(" | shooter ").append(shooter != null ? "ok" : "NO");
        if (transfer != null) sb.append(" | transfer ok");
        return sb.toString();
    }

    // ------------------------------------------------------------------
    // Tiempo
    // ------------------------------------------------------------------
    public void reiniciarTiempo() {
        reloj.reset();
    }

    public double tiempo() {
        return reloj.seconds();
    }

    public double tiempoRestante(double duracionPartida) {
        return Math.max(0.0, duracionPartida - reloj.seconds());
    }

    // ------------------------------------------------------------------
    // Controles
    // ------------------------------------------------------------------
    public void leerControles(Gamepad gamepad1, Gamepad gamepad2) {
        pad1 = gamepad1;
        pad2 = gamepad2;
    }

    private double zonaMuerta(double v) {
        return Math.abs(v) < BioBuzzConfig.DEADBAND ? 0.0 : v;
    }

    /** Curva exponencial para micro-precision cerca del centro y velocidad maxima a fondo */
    private double curvaExponencial(double v) {
        if (Math.abs(v) < BioBuzzConfig.DEADBAND) return 0.0;
        double expo = BioBuzzConfig.DRIVE_EXPO_FACTOR;
        double abs = Math.abs(v);
        double out = (1.0 - expo) * abs + expo * (abs * abs * abs);
        return Math.copySign(out, v);
    }

    public double ejeY() {
        return pad1 == null ? 0.0 : curvaExponencial(-pad1.left_stick_y);
    }

    public double ejeX() {
        return pad1 == null ? 0.0 : curvaExponencial(pad1.left_stick_x * 1.10);
    }

    public double giro() {
        return pad1 == null ? 0.0 : curvaExponencial(pad1.right_stick_x);
    }

    public boolean modoLento() {
        return pad1 != null && (pad1.left_trigger > 0.1 || pad1.left_bumper);
    }

    public boolean botonIntakeEntra() {
        return (pad1 != null && pad1.right_bumper) || (pad2 != null && pad2.right_bumper);
    }

    public boolean botonIntakeSale() {
        return (pad1 != null && pad1.left_bumper) || (pad2 != null && pad2.left_bumper);
    }

    public double gatilloLanzador() {
        double g1 = pad1 == null ? 0.0 : pad1.right_trigger;
        double g2 = pad2 == null ? 0.0 : pad2.right_trigger;
        return Math.max(g1, g2);
    }

    public boolean botonDisparar() {
        return (pad1 != null && pad1.b) || (pad2 != null && pad2.b);
    }

    public boolean botonAutoAim() {
        return pad1 != null && pad1.x;
    }

    /** Detecta flanco de subida (un solo cambio por pulsacion de Y). */
    public boolean botonCambiarPipeline() {
        boolean actual = (pad1 != null && pad1.y) || (pad2 != null && pad2.y);
        boolean flanco = actual && !yAnterior;
        yAnterior = actual;
        return flanco;
    }

    /** Devuelve true si el piloto esta moviendo algun stick o usando controles manuales. */
    public boolean pilotoActivo() {
        return Math.abs(ejeY()) > 0.0 || Math.abs(ejeX()) > 0.0 || Math.abs(giro()) > 0.0
                || botonIntakeEntra() || botonIntakeSale() || gatilloLanzador() > 0.1;
    }

    // ------------------------------------------------------------------
    // Chasis
    // ------------------------------------------------------------------
    /** Cinematica inversa mecanum con compensacion de strafe y filtro de aceleracion asimetrico. */
    public void conducir(double avance, double lateral, double giro, double escala) {
        // Compensacion cinemática de friccion en ruedas mecanum
        double strafe = lateral * BioBuzzConfig.STRAFE_MULTIPLIER;
        double fl = avance + strafe + giro;
        double bl = avance - strafe + giro;
        double fr = avance - strafe - giro;
        double br = avance + strafe - giro;

        // Desaturacion con prioridad de giro (steering priority)
        double max = Math.max(Math.max(Math.abs(fl), Math.abs(bl)),
                Math.max(Math.abs(fr), Math.abs(br)));
        if (max > 1.0) {
            fl /= max;
            bl /= max;
            fr /= max;
            br /= max;
        }

        // Filtro de rampa asimetrico (aceleracion controlada, frenado inmediato sin derrape)
        double dt = Math.max(0.001, Math.min(0.1, relojChasis.seconds()));
        relojChasis.reset();

        fl = calcularPasoRampa(fl * escala, lastFl, dt);
        bl = calcularPasoRampa(bl * escala, lastBl, dt);
        fr = calcularPasoRampa(fr * escala, lastFr, dt);
        br = calcularPasoRampa(br * escala, lastBr, dt);

        lastFl = fl; lastBl = bl; lastFr = fr; lastBr = br;

        avanceMotor(motorFl, fl);
        avanceMotor(motorBl, bl);
        avanceMotor(motorFr, fr);
        avanceMotor(motorBr, br);
    }

    private double calcularPasoRampa(double objetivo, double actual, double dt) {
        boolean frenando = Math.abs(objetivo) < Math.abs(actual) || (objetivo * actual < 0.0);
        double maxRate = frenando ? BioBuzzConfig.SLEW_RATE_DECEL : BioBuzzConfig.SLEW_RATE_ACCEL;
        double maxPaso = maxRate * dt;
        return Range.clip(objetivo, actual - maxPaso, actual + maxPaso);
    }

    public double getFactorCompensacionVoltaje() {
        if (sensorVoltaje == null) return 1.0;
        try {
            double v = sensorVoltaje.getVoltage();
            if (v < 9.5 || v > 15.0) return 1.0;
            return Range.clip(BioBuzzConfig.VOLTAJE_NOMINAL / v, 0.85, 1.25);
        } catch (Exception e) {
            return 1.0;
        }
    }

    private void avanceMotor(DcMotor motor, double potencia) {
        if (motor != null) {
            motor.setPower(Range.clip(potencia, -1.0, 1.0));
        }
    }

    /** Avanza recto a la potencia indicada durante los milisegundos dados. */
    public void avanzar(double potencia, long milisegundos) {
        conducir(potencia, 0.0, 0.0, 1.0);
        long inicio = System.currentTimeMillis();
        while (System.currentTimeMillis() - inicio < milisegundos && !Thread.currentThread().isInterrupted()) {
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        detenerChasis();
    }

    /**
     * Movimiento coordinado con accionamiento concurrente de mecanismos (shooter e intake)
     * y compensacion activa de bateria para garantizar repetibilidad en autonomo.
     */
    public void avanzarCoordinado(double avance, double lateral, double giro,
                                  double velocidadShooter, double potenciaIntake,
                                  long milisegundos) {
        double factorV = getFactorCompensacionVoltaje();
        double compAvance = Range.clip(avance * factorV, -1.0, 1.0);
        double compLateral = Range.clip(lateral * factorV, -1.0, 1.0);
        double compGiro = Range.clip(giro * factorV, -1.0, 1.0);

        long inicio = System.currentTimeMillis();
        while (System.currentTimeMillis() - inicio < milisegundos && !Thread.currentThread().isInterrupted()) {
            conducir(compAvance, compLateral, compGiro, 1.0);
            if (velocidadShooter > 0) {
                actualizarLanzador(velocidadShooter);
            }
            if (potenciaIntake != 0) {
                setIntake(potenciaIntake);
            }
            try {
                Thread.sleep(10);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        detenerChasis();
    }

    /** Freno activo inmediato reseteando el filtro slew-rate para eliminar inercia residual. */
    public void detenerChasis() {
        lastFl = 0.0;
        lastBl = 0.0;
        lastFr = 0.0;
        lastBr = 0.0;
        avanceMotor(motorFl, 0.0);
        avanceMotor(motorBl, 0.0);
        avanceMotor(motorFr, 0.0);
        avanceMotor(motorBr, 0.0);
    }

    public void girarA(double grados, double potencia) {
        double signo = Math.signum(grados);
        if (signo == 0) {
            return;
        }
        long ms = (long) (Math.abs(grados) / VELOCIDAD_GIRO_MAX_DEG_S * 1000.0 * potencia * 2.0);
        conducir(0.0, 0.0, signo * potencia, 1.0);
        try {
            Thread.sleep(Math.max(30L, ms));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        detenerChasis();
    }

    /** Gira buscando objetivo: sale antes si la vision lo encuentra centrado. */
    public boolean girarBuscandoObjetivo(double potencia, long maxMs) {
        long inicio = System.currentTimeMillis();
        double signo = 1.0;
        while (System.currentTimeMillis() - inicio < maxMs && !Thread.currentThread().isInterrupted()) {
            VisionData v = vision.leer(BioBuzzConfig.MIN_CONFIDENCE);
            if (v.tieneObjetivo) {
                detenerChasis();
                return true;
            }
            conducir(0.0, 0.0, signo * potencia, 1.0);
            try {
                Thread.sleep(15);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        detenerChasis();
        return false;
    }

    /** Prueba un motor por nombre (para el OpMode de diagnostico). */
    public void probarMotor(String nombre, double potencia) {
        detenerChasis();
        if (nombre == null) {
            return;
        }
        switch (nombre) {
            case "frontLeft": avanceMotor(motorFl, potencia); break;
            case "backLeft": avanceMotor(motorBl, potencia); break;
            case "frontRight": avanceMotor(motorFr, potencia); break;
            case "backRight": avanceMotor(motorBr, potencia); break;
            default: break;
        }
    }

    // ------------------------------------------------------------------
    // Rutina por pasos (autonomo)
    // ------------------------------------------------------------------
    public void paso(String descripcion) {
        pasoActual++;
        telemetry.addData("Paso " + pasoActual, descripcion);
        telemetry.update();
    }

    /** Arranca un paso de avance sin bloquear (el OpMode lo consulta). */
    public void iniciarPasoAvance(double potencia, long milisegundos) {
        pasoPotencia = potencia;
        pasoDuracionMs = milisegundos;
        pasoEnCurso = true;
        relojPaso.reset();
    }

    /** Actualiza el paso en curso; devuelve true mientras siga activo. */
    public boolean actualizarPasos() {
        if (!pasoEnCurso) {
            return false;
        }
        if (relojPaso.milliseconds() >= pasoDuracionMs) {
            pasoEnCurso = false;
            detenerChasis();
            return false;
        }
        conducir(pasoPotencia, 0.0, 0.0, 1.0);
        return true;
    }

    // ------------------------------------------------------------------
    // Intake y Transfer
    // ------------------------------------------------------------------
    public void controlarIntake(boolean entra, boolean sale) {
        if (intake == null) {
            return;
        }
        if (entra && !sale) {
            intake.setPower(BioBuzzConfig.INTAKE_POWER);
            if (transfer != null) transfer.setPower(1.0);
        } else if (sale && !entra) {
            intake.setPower(-BioBuzzConfig.INTAKE_POWER);
            if (transfer != null) transfer.setPower(-1.0);
        } else {
            intake.setPower(0.0);
            if (transfer != null) transfer.setPower(0.0);
        }
    }

    public void setIntake(double potencia) {
        if (intake != null) {
            intake.setPower(Range.clip(potencia, -1.0, 1.0));
        }
        if (transfer != null) {
            transfer.setPower(Range.clip(potencia, -1.0, 1.0));
        }
    }

    // ------------------------------------------------------------------
    // Lanzador
    // ------------------------------------------------------------------
    /** Devuelve la velocidad objetivo segun el gatillo manual. */
    public double controlarLanzadorManual(double maxima, double gatillo) {
        if (gatillo > 0.1) {
            objetivoLanzador = gatillo * maxima;
        } else {
            objetivoLanzador = 0.0;
        }
        return objetivoLanzador;
    }

    /**
     * Lleva el lanzador a la velocidad pedida con una rampa suave.
     * Si el objetivo es 0, desacelera por inercia (FLOAT) de inmediato para evitar
     * freno contra-electromotriz y picos de corriente.
     */
    public double actualizarLanzador(double velocidadObjetivo) {
        if (shooter == null) {
            return 0.0;
        }
        objetivoLanzador = Range.clip(velocidadObjetivo, 0.0,
                BioBuzzConfig.MAX_SHOOTER_VELOCITY);

        if (objetivoLanzador <= 0.0) {
            potenciaLanzador = 0.0;
            shooter.setVelocity(0.0);
            shooter.setPower(0.0);
            return 0.0;
        }

        if (relojRampa == null) {
            relojRampa = new ElapsedTime();
        }
        double dt = Math.min(0.2, Math.max(0.0, relojRampa.seconds()));
        relojRampa.reset();
        double delta = BioBuzzConfig.SHOOTER_ACELERACION_TICKS_S2 * dt;
        if (potenciaLanzador < objetivoLanzador) {
            potenciaLanzador = Math.min(objetivoLanzador, potenciaLanzador + delta);
        } else if (potenciaLanzador > objetivoLanzador) {
            potenciaLanzador = Math.max(objetivoLanzador, potenciaLanzador - delta);
        }
        shooter.setVelocity(potenciaLanzador);
        return potenciaLanzador;
    }

    public double velocidadLanzador() {
        return shooter == null ? 0.0 : shooter.getVelocity();
    }

    public double objetivoLanzador() {
        return objetivoLanzador;
    }

    public boolean lanzadorListo() {
        return objetivoLanzador > 100.0
                && Math.abs(velocidadLanzador() - objetivoLanzador)
                <= BioBuzzConfig.SHOOTER_TOLERANCIA_TICKS;
    }

    /** Velocidad necesaria para la distancia medida (cm). */
    public double velocidadParaDistancia(double distanciaCm) {
        double v = BioBuzzConfig.BASE_SHOOTER_VELOCITY
                + distanciaCm * BioBuzzConfig.DISTANCE_VELOCITY_GAIN;
        return Range.clip(v, 500.0, BioBuzzConfig.MAX_SHOOTER_VELOCITY);
    }

    // ------------------------------------------------------------------
    // Vision
    // ------------------------------------------------------------------
    public void actualizarVision(int pipelineNeural, int pipelineColor, double confianza) {
        if (vision == null) {
            return;
        }
        if (botonCambiarPipeline()) {
            vision.alternarPipeline(pipelineNeural, pipelineColor);
        }
        vision.leer(confianza);
    }

    public void setFiltroVision(String filtro) {
        if (vision != null) {
            vision.setTargetFilter(filtro);
        }
    }

    public VisionData datosVision() {
        return vision == null ? new VisionData() : vision.ultimo();
    }

    public String objetivoTexto() {
        return vision == null ? "sin camara" : vision.textoTelemetria();
    }

    // ------------------------------------------------------------------
    // IA de decision
    // ------------------------------------------------------------------
    /**
     * Bucle cerrado PD con compensacion de friccion estatica (kS) y filtrado de derivada
     * para eliminar el 'derivative kick' y jitter en auto-aiming con Limelight 3A.
     */
    public double calcularGiroAutoAim(VisionData v) {
        if (v == null || !v.tieneObjetivo) {
            ultimoErrorAim = 0.0;
            ultimaDerivadaAim = 0.0;
            tieneErrorPrevioAim = false;
            return 0.0;
        }
        double error = v.tx;
        if (Math.abs(error) <= BioBuzzConfig.AUTO_AIM_TOLERANCE) {
            ultimoErrorAim = error;
            ultimaDerivadaAim = 0.0;
            tieneErrorPrevioAim = false;
            if (!objetivoBloqueadoPrevio) {
                if (pad1 != null) pad1.rumble(0.6, 0.6, 120);
                if (pad2 != null) pad2.rumble(0.6, 0.6, 120);
                objetivoBloqueadoPrevio = true;
            }
            return 0.0;
        }
        objetivoBloqueadoPrevio = false;

        double dt = Math.max(0.005, Math.min(0.1, relojAim.seconds()));
        relojAim.reset();

        double derivada = 0.0;
        if (tieneErrorPrevioAim && dt < 0.1) {
            double rawDerivada = (error - ultimoErrorAim) / dt;
            // Filtro pasa-bajos exponencial para desacoplar ruido de imagen
            derivada = 0.70 * rawDerivada + 0.30 * ultimaDerivadaAim;
            ultimaDerivadaAim = derivada;
        } else {
            tieneErrorPrevioAim = true;
            ultimaDerivadaAim = 0.0;
        }
        ultimoErrorAim = error;

        // Suavizado continuo de friccion estatica (kS) para eliminar oscilacion tipo chatter
        double factorFriccion = Math.min(1.0, Math.max(0.0, (Math.abs(error) - BioBuzzConfig.AUTO_AIM_TOLERANCE) / 1.2));
        double kS = Math.signum(error) * BioBuzzConfig.AUTO_AIM_KS * factorFriccion;
        double kP = error * BioBuzzConfig.AUTO_AIM_KP;
        double kD = Range.clip(derivada * BioBuzzConfig.AUTO_AIM_KD, -0.15, 0.15);

        return Range.clip(kS + kP + kD, -BioBuzzConfig.AUTO_AIM_MAX_TURN, BioBuzzConfig.AUTO_AIM_MAX_TURN);
    }

    public void pensar() {
        if (cerebro == null) {
            return;
        }
        VisionData v = datosVision();
        double distanciaCm = v.distanciaCm;
        boolean apuntado = v.tieneObjetivo
                && Math.abs(v.tx) <= BioBuzzConfig.AUTO_AIM_TOLERANCE + 1.0;

        // Si el piloto esta conduciendo manualmente (y no presiona Auto-Aim ni Force Shoot),
        // la IA solo observa y calcula su estado sin pisar los motores del piloto.
        if (pilotoActivo() && !botonAutoAim() && !forceShoot) {
            cerebro.modoManual(true);
            return;
        }

        // Acelerar el shooter en cuanto vemos un objetivo (antes esperaba a lanzadorListo()
        // para encenderlo, lo cual era un bloqueo circular).
        double objetivo = 0.0;
        if (v.tieneObjetivo || forceShoot) {
            objetivo = velocidadParaDistancia(distanciaCm);
        }
        actualizarLanzador(objetivo);

        double giroIA = 0.0;
        if (v.tieneObjetivo && Math.abs(v.tx) > BioBuzzConfig.AUTO_AIM_TOLERANCE) {
            giroIA = Range.clip(v.tx * BioBuzzConfig.AUTO_AIM_KP,
                    -BioBuzzConfig.AUTO_AIM_MAX_TURN, BioBuzzConfig.AUTO_AIM_MAX_TURN);
        }

        cerebro.actualizar(tiempo(), v, lanzadorListo(), v.tieneObjetivo,
                apuntado, distanciaCm, objetivo > 0.0);

        switch (cerebro.estado()) {
            case DISPARAR:
                conducir(0.0, 0.0, giroIA, 1.0);
                // Alimentar pieza hacia el shooter cuando ya alcanzo las RPM
                setIntake(lanzadorListo() ? BioBuzzConfig.INTAKE_POWER : 0.0);
                break;
            case APUNTAR:
                conducir(0.0, 0.0, giroIA, 1.0);
                setIntake(0.0);
                break;
            case ACERCARSE:
                conducir(0.45, 0.0, giroIA, 1.0);
                setIntake(0.0);
                break;
            case ALEJARSE:
                conducir(-0.35, 0.0, giroIA, 1.0);
                setIntake(0.0);
                break;
            case BUSCAR:
                conducir(0.0, 0.0, 0.22, 1.0);
                setIntake(0.0);
                break;
            case RECOGER:
                setIntake(BioBuzzConfig.INTAKE_POWER);
                conducir(0.22, 0.0, giroIA, 1.0);
                break;
            default:
                setIntake(0.0);
                break;
        }
    }

    public void estadoForzado(boolean forzar) {
        forceShoot = forzar;
    }

    public void reiniciarCerebro() {
        if (cerebro != null) {
            cerebro.reiniciar();
        }
        reiniciarTiempo();
    }

    public String cerebroResumen() {
        return cerebro == null ? "-" : cerebro.resumen();
    }

    // ------------------------------------------------------------------
    // Telemetria y parada
    // ------------------------------------------------------------------
    public void publicarTelemetria() {
        double dtLoop = relojLoop.seconds();
        relojLoop.reset();
        if (dtLoop > 0.0) {
            double hzActual = 1.0 / dtLoop;
            frecuenciaLoopHz = frecuenciaLoopHz == 0.0 ? hzActual : (0.90 * frecuenciaLoopHz + 0.10 * hzActual);
        }
        double voltaje = sensorVoltaje != null ? sensorVoltaje.getVoltage() : 0.0;

        telemetry.addData("[SISTEMA]", "%.0f Hz | %.2f V | T: %.1fs", frecuenciaLoopHz, voltaje, tiempo());
        telemetry.addData("[VISIÓN]", objetivoTexto());
        telemetry.addData("[CEREBRO]", cerebroResumen());
        telemetry.addData("[SHOOTER]", "%.0f -> %.0f %s", velocidadLanzador(), objetivoLanzador, lanzadorListo() ? "[LISTO]" : "cargando");
        if (vision != null) {
            VisionData v = vision.ultimo();
            telemetry.addData("[AIM]", "tx: %.2f° | ty: %.2f° | d: %.1fcm", v.tx, v.ty, v.distanciaCm);
            telemetry.addData("[PIEZAS]", "Polen: %d | Nectar: %d", v.conteoPolen, v.conteoNectar);
        }
    }

    public double getVoltajeBateria() {
        return sensorVoltaje != null ? sensorVoltaje.getVoltage() : 0.0;
    }

    public double getFrecuenciaLoopHz() {
        return frecuenciaLoopHz;
    }

    public void parar() {
        detenerChasis();
        setIntake(0.0);
        if (shooter != null) {
            try {
                shooter.setVelocity(0.0);
                shooter.setPower(0.0);
            } catch (Exception ignored) {
                // el hardware puede haberse desconectado al cerrar
            }
        }
        if (vision != null) {
            vision.parar();
        }
    }
}
