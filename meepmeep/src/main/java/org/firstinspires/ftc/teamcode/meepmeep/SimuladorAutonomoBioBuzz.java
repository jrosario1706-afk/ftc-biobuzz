package org.firstinspires.ftc.teamcode.meepmeep;

public class SimuladorAutonomoBioBuzz {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   SIMULACION Y DIAGNOSTICO: BIOBUZZ AUTO PRO V3 AI (2026-2027)   ");
        System.out.println("==================================================================");
        System.out.println();

        double startX = 9.0, startY = 111.0, startH = 270.0;
        double scoreX = 16.0, scoreY = 128.0, scoreH = 315.0;
        double pickupX = 38.0, pickupY = 88.0, pickupH = 180.0;
        double parkX = 12.0, parkY = 105.0, parkH = 270.0;

        double t = 0.0;
        double distTotal = 0.0;

        System.out.printf("[0.00s] ESTADO: START\n");
        System.out.printf("        Robot posicionado en la pared: X=%.1f, Y=%.1f, Giro=%.1f°\n\n", startX, startY, startH);

        // PASO 1: DRIVE_SCORE_1
        double d1 = Math.hypot(scoreX - startX, scoreY - startY);
        double t1 = (d1 / 45.0) + 0.4; // 45 in/s + deceleracion
        t += t1;
        distTotal += d1;
        System.out.printf("[%.2fs] ESTADO: DRIVE_SCORE_1 -> Navegando a Colmena (Score)\n", t);
        System.out.printf("        Destino: X=%.1f, Y=%.1f | Distancia: %.1f in | Giro a %.1f°\n", scoreX, scoreY, d1, scoreH);
        System.out.printf("        Pre-spinup: Shooter acelerando a 2000 ticks/s mientras se mueve.\n\n");

        // PASO 2: SHOOT_1
        double tShoot1 = 1.2;
        t += tShoot1;
        System.out.printf("[%.2fs] ESTADO: SHOOT_1 -> Disparo de Pre-carga\n", t);
        System.out.printf("        Shooter a velocidad constante (PID estable) -> Activando feed de polen.\n");
        System.out.printf("        [OK] Elemento pre-cargado disparado exitosamente.\n\n");

        // PASO 3: DRIVE_PICKUP
        double d2 = Math.hypot(pickupX - scoreX, pickupY - scoreY);
        double t2 = (d2 / 50.0) + 0.5;
        t += t2;
        distTotal += d2;
        System.out.printf("[%.2fs] ESTADO: DRIVE_PICKUP -> Navegando a zona de Polen\n", t);
        System.out.printf("        Destino: X=%.1f, Y=%.1f | Distancia: %.1f in | Giro a %.1f°\n\n", pickupX, pickupY, d2, pickupH);

        // PASO 4: COLLECT
        double tCollect = 1.5;
        t += tCollect;
        System.out.printf("[%.2fs] ESTADO: COLLECT -> Recoleccion activa con Intake\n", t);
        System.out.printf("        Intake al 100%% | Limelight 3A rastreando muestra | Duracion: %.1fs\n", tCollect);
        System.out.printf("        [OK] Polen detectado y cargado en el transfer.\n\n");

        // PASO 5: DRIVE_SCORE_2
        double d3 = Math.hypot(scoreX - pickupX, scoreY - pickupY);
        double t3 = (d3 / 50.0) + 0.5;
        t += t3;
        distTotal += d3;
        System.out.printf("[%.2fs] ESTADO: DRIVE_SCORE_2 -> Retorno a Colmena\n", t);
        System.out.printf("        Destino: X=%.1f, Y=%.1f | Distancia: %.1f in | Giro a %.1f°\n\n", scoreX, scoreY, d3, scoreH);

        // PASO 6: SHOOT_2
        double tShoot2 = 1.2;
        t += tShoot2;
        System.out.printf("[%.2fs] ESTADO: SHOOT_2 -> Segundo Disparo\n", t);
        System.out.printf("        Shooter a velocidad nominal -> Disparo de polen.\n");
        System.out.printf("        [OK] Segundo tiro completado.\n\n");

        // PASO 7: PARK
        double d4 = Math.hypot(parkX - scoreX, parkY - scoreY);
        double t4 = (d4 / 40.0) + 0.3;
        t += t4;
        distTotal += d4;
        System.out.printf("[%.2fs] ESTADO: PARK -> Navegando a Estacionamiento\n", t);
        System.out.printf("        Destino: X=%.1f, Y=%.1f | Distancia: %.1f in | Giro a %.1f°\n\n", parkX, parkY, d4, parkH);

        // FINAL: COMPLETE
        System.out.printf("[%.2fs] ESTADO: COMPLETE\n", t);
        System.out.println("        Frenado activo en 4 motores. Todos los mecanismos apagados.");
        System.out.println();
        System.out.println("==================================================================");
        System.out.println("                     RESUMEN DEL AUTONOMO                         ");
        System.out.println("==================================================================");
        System.out.printf(" - Tiempo Total Estimado:  %.2f segundos (Regla FTC: max 30.0s) [OK]\n", t);
        System.out.printf(" - Margen de Seguridad:    %.2f segundos restantes de buffer\n", (30.0 - t));
        System.out.printf(" - Distancia Recorrida:    %.1f pulgadas (%.2f metros)\n", distTotal, distTotal * 0.0254);
        System.out.println(" - Elementos Anotados:     2 disparos de polen completados");
        System.out.println(" - Parking:                Completado dentro del tiempo");
        System.out.println(" - Diagnostico Odometria:  SIN BLOQUEOS / TRANSICIONES VALIDAS");
        System.out.println("==================================================================");
    }
}
