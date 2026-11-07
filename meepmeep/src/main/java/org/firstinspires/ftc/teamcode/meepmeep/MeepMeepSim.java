package org.firstinspires.ftc.teamcode.meepmeep;

import com.acmerobotics.roadrunner.geometry.Pose2d;
import com.acmerobotics.roadrunner.geometry.Vector2d;
import org.rowlandhall.meepmeep.MeepMeep;
import org.rowlandhall.meepmeep.roadrunner.DefaultBotBuilder;
import org.rowlandhall.meepmeep.roadrunner.entity.RoadRunnerBotEntity;

import javax.imageio.ImageIO;
import java.awt.Image;
import java.io.File;
import java.io.InputStream;

/**
 * ============================================================================
 * SIMULADOR MEEPMEEP: 3 TRAYECTORIAS FLUIDAS CON ACCIONES DE RECOGER Y TIRAR
 * ============================================================================
 * 1. Salida de zona inicial hacia el Submersible -> 🟢 [RECOGER]
 * 2. Regreso y alineación con la Canasta Alta -> 🎯 [TIRAR]
 * 3. Desplazamiento a la zona de estacionamiento -> 🏁 [PARK]
 * ============================================================================
 */
public class MeepMeepSim {

    public static void main(String[] args) {
        System.setProperty("sun.java2d.opengl", "true");

        MeepMeep meepMeep = new MeepMeep(800);

        Pose2d startPose = new Pose2d(-10.0, -56.0, Math.toRadians(0.0));

        RoadRunnerBotEntity myBot = new DefaultBotBuilder(meepMeep)
                .setDimensions(15.0, 16.0)
                .setConstraints(55.0, 50.0, Math.toRadians(180), Math.toRadians(180), 14.5)
                .followTrajectorySequence(drive -> drive.trajectorySequenceBuilder(startPose)

                        // 1. Salida y llegada al Submersible (Recoger)
                        .addTemporalMarker(() -> System.out.println("[TRAYECTORIA 1] Saliendo de zona inicial hacia el Submersible..."))
                        .splineTo(new Vector2d(25.0, -52.0), Math.toRadians(15.0))
                        .splineTo(new Vector2d(50.0, -48.0), Math.toRadians(55.0))
                        .splineTo(new Vector2d(58.0, -10.0), Math.toRadians(90.0))
                        .splineTo(new Vector2d(62.0, 18.0), Math.toRadians(75.0))
                        .setTangent(Math.toRadians(160.0))
                        .splineTo(new Vector2d(30.0, 32.0), Math.toRadians(165.0))
                        .splineTo(new Vector2d(-8.0, 34.0), Math.toRadians(170.0))
                        .splineToLinearHeading(new Pose2d(-16.0, 19.0, Math.toRadians(0.0)), Math.toRadians(0.0))
                        .addTemporalMarker(() -> System.out.println("🟢 [ACCIÓN: RECOGER] >> Intake encendido: Absorbiendo elementos en Submersible"))
                        .waitSeconds(1.0)

                        // 2. Regreso a la Canasta Alta (Tirar)
                        .addTemporalMarker(() -> System.out.println("[TRAYECTORIA 2] Regresando hacia la Canasta Alta..."))
                        .setTangent(Math.toRadians(120.0))
                        .splineTo(new Vector2d(-32.0, 38.0), Math.toRadians(110.0))
                        .splineTo(new Vector2d(-20.0, 62.0), Math.toRadians(90.0))
                        .splineToLinearHeading(new Pose2d(-52.0, 54.0, Math.toRadians(45.0)), Math.toRadians(210.0))
                        .addTemporalMarker(() -> System.out.println("🎯 [ACCIÓN: TIRAR] >> Shooter a velocidad nominal: Disparo efectuado en Canasta Alta!"))
                        .waitSeconds(1.0)

                        // 3. Estacionamiento (Park)
                        .addTemporalMarker(() -> System.out.println("[TRAYECTORIA 3] Dirigiéndose a zona de estacionamiento (Park)..."))
                        .setTangent(Math.toRadians(210.0))
                        .splineTo(new Vector2d(-42.0, -6.0), Math.toRadians(220.0))
                        .splineTo(new Vector2d(-60.0, -26.0), Math.toRadians(235.0))
                        .splineToLinearHeading(new Pose2d(-70.0, -45.0, Math.toRadians(270.0)), Math.toRadians(250.0))
                        .addTemporalMarker(() -> System.out.println("🏁 [ACCIÓN: PARK] >> Robot estacionado de forma segura. ¡Autónomo completado!"))
                        .build()
                );

        Image imgCampo = null;
        try {
            InputStream is = MeepMeepSim.class.getResourceAsStream("/field.png");
            if (is != null) {
                imgCampo = ImageIO.read(is);
            } else {
                File f = new File("meepmeep/src/main/resources/field.png");
                if (f.exists()) imgCampo = ImageIO.read(f);
            }
        } catch (Exception ignored) {}

        if (imgCampo != null) {
            meepMeep.setBackground(imgCampo);
        } else {
            meepMeep.setBackground(MeepMeep.Background.GRID_GRAY);
        }

        meepMeep.setDarkMode(true)
                .setBackgroundAlpha(0.95f)
                .addEntity(myBot)
                .start();
    }
}
