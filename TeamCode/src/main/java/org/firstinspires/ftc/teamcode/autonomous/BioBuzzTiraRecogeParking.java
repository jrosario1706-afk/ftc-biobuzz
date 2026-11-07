package org.firstinspires.ftc.teamcode.autonomous;

import com.acmerobotics.dashboard.config.Config;
import com.qualcomm.robotcore.eventloop.opmode.Autonomous;

/**
 * Tira la pre-carga, recoge otra pieza, vuelve a tirar y se estaciona.
 * Solo usa tiempos (sin odometria). Traido de "ftcbioobuzzteleopia" y mejorado.
 */
@Config
@Autonomous(name = "BioBuzz Tira-Recoge-Parking", group = "BioBuzz Simple")
public class BioBuzzTiraRecogeParking extends TimedAutoBase {

    public static double POTENCIA_AVANCE = 0.45;
    public static double POTENCIA_STRAFE = 0.40;
    public static double VELOCIDAD_SHOOTER = 1700;
    public static double TIEMPO_ALIMENTAR = 1.2;

    @Override
    protected String nombre() {
        return "AUTONOMO: TIRA, RECOGE Y PARKING";
    }

    @Override
    protected void secuencia() {
        paso("1/5 Avanzar a posicion de tiro");
        if (!avanzar(POTENCIA_AVANCE, 0.9)) return;

        paso("2/5 Primer disparo (pre-carga)");
        if (!disparar(VELOCIDAD_SHOOTER, 1.5, TIEMPO_ALIMENTAR)) return;

        paso("3/5 Recogiendo pieza con intake");
        if (!recogerAvanzando(POTENCIA_AVANCE, 1.0)) return;

        paso("4/5 Segundo disparo");
        if (!disparar(VELOCIDAD_SHOOTER, 1.5, TIEMPO_ALIMENTAR)) return;

        paso("5/5 Estacionar");
        if (!avanzar(POTENCIA_AVANCE, 0.8)) return;
        strafeIzquierda(POTENCIA_STRAFE, 0.6);
    }
}
