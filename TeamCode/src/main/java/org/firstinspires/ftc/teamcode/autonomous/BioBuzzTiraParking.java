package org.firstinspires.ftc.teamcode.autonomous;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

/**
 * Tira la pre-carga y se estaciona. Solo usa tiempos (sin odometria).
 * Traido del proyecto "ftcbioobuzzteleopia" y mejorado.
 */
@Config
@Autonomous(name = "BioBuzz Tira y Parqueo", group = "BioBuzz Simple")
public class BioBuzzTiraParking extends TimedAutoBase {

    public static double POTENCIA_AVANCE = 0.45;
    public static double POTENCIA_STRAFE = 0.40;
    public static double VELOCIDAD_SHOOTER = 1700;
    public static double TIEMPO_ALIMENTAR = 2.0;

    @Override
    protected String nombre() {
        return "AUTONOMO: TIRA Y PARQUEO";
    }

    @Override
    protected void secuencia() {
        paso("1/5 Avanzando a zona de lanzamiento");
        if (!avanzar(POTENCIA_AVANCE, 1.0)) return;

        paso("2/5 Lanzando pre-carga");
        if (!disparar(VELOCIDAD_SHOOTER, 1.5, TIEMPO_ALIMENTAR)) return;

        paso("3/5 Ajuste lateral");
        if (!strafeDerecha(POTENCIA_STRAFE, 0.8)) return;

        paso("4/5 Avanzando a zona de parqueo");
        if (!avanzar(POTENCIA_AVANCE, 1.2)) return;

        paso("5/5 Estacionamiento final");
        strafeIzquierda(POTENCIA_STRAFE, 0.7);
    }
}
