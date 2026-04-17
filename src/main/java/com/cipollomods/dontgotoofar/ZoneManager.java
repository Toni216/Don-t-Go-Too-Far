package com.cipollomods.dontgotoofar;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class ZoneManager {

    /**
     * Guardamos la zona y la posición del último cálculo por jugador.
     * Así evitamos recalcular la zona en cada tick.
     */
    private static final Map<UUID, Integer> zoneCache = new HashMap<>();
    private static final Map<UUID, Vec3> lastCalcPos = new HashMap<>();

    /**
     * Devuelve la zona del jugador (1-5).
     * Solo recalcula si el jugador se ha movido más de cache_recalc_distance bloques.
     */
    public static int getZone(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Vec3 currentPos = player.position();
        Vec3 lastPos = lastCalcPos.get(uuid);
        int recalcDistance = ZoneConfig.CACHE_RECALC_DISTANCE.get();

        if (lastPos == null || distanceXZ(currentPos, lastPos) >= recalcDistance) {
            int zone = calculateZone(currentPos);
            zoneCache.put(uuid, zone);
            lastCalcPos.put(uuid, currentPos);
            return zone;
        }

        return zoneCache.getOrDefault(uuid, 1);
    }

    // Calcula la distancia al spawn (0,0) en el plano XZ e identifica la zona.
    private static int calculateZone(Vec3 pos) {
        double distance = Math.sqrt(pos.x * pos.x + pos.z * pos.z);

        if (distance <= ZoneConfig.ZONE1_MAX.get()) return 1;
        if (distance <= ZoneConfig.ZONE2_MAX.get()) return 2;
        if (distance <= ZoneConfig.ZONE3_MAX.get()) return 3;
        if (distance <= ZoneConfig.ZONE4_MAX.get()) return 4;
        return 5;
    }

    // Ignoramos Y porque las zonas son horizontales, no dependen de la altura.
    private static double distanceXZ(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return Math.sqrt(dx * dx + dz * dz);
    }

    public static void clearCache(UUID uuid) {
        zoneCache.remove(uuid);
        lastCalcPos.remove(uuid);
    }

    public static String getZoneName(int zone) {
        return switch (zone) {
            case 1 -> "Zona 1 — Tierras Seguras";
            case 2 -> "Zona 2 — Frontera";
            case 3 -> "Zona 3 — Tierras Salvajes";
            case 4 -> "Zona 4 — Abismo";
            case 5 -> "Zona 5 — Más Allá";
            default -> "Zona desconocida";
        };
    }
}