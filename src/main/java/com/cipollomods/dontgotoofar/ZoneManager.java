package com.cipollomods.dontgotoofar;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.phys.Vec3;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Gestiona el sistema de zonas de peligro basado en la distancia al spawn (0,0) del Overworld.
 * Cachea la zona de cada jugador y solo recalcula cuando se ha movido lo suficiente,
 * minimizando el coste por tick. También centraliza la búsqueda del jugador más cercano
 * a un mob, usada por MobStatHandler y SpawnRateHandler.
 */
public class ZoneManager {

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
        int recalcDistanceSq = ZoneConfig.CACHE_RECALC_DISTANCE.get();
        recalcDistanceSq *= recalcDistanceSq;

        if (lastPos == null || distanceXZSq(currentPos, lastPos) >= recalcDistanceSq) {
            int zone = calculateZone(currentPos);
            zoneCache.put(uuid, zone);
            lastCalcPos.put(uuid, currentPos);
            return zone;
        }

        return zoneCache.getOrDefault(uuid, 1);
    }

    /**
     * Busca el jugador superviviente más cercano al mob y devuelve su zona.
     * Devuelve -1 si no hay ningún jugador válido en el nivel.
     */
    public static int getNearestPlayerZone(Mob mob, ServerLevel level) {
        return level.players().stream()
                .filter(p -> !p.isSpectator() && !p.isCreative())
                .min((a, b) -> Double.compare(a.distanceToSqr(mob), b.distanceToSqr(mob)))
                .map(ZoneManager::getZone)
                .orElse(-1);
    }

    private static int calculateZone(Vec3 pos) {
        // Comparamos distancias al cuadrado para evitar Math.sqrt en cada recálculo.
        double distSq = pos.x * pos.x + pos.z * pos.z;
        int z1 = ZoneConfig.ZONE1_MAX.get(), z2 = ZoneConfig.ZONE2_MAX.get(),
                z3 = ZoneConfig.ZONE3_MAX.get(), z4 = ZoneConfig.ZONE4_MAX.get();

        if (distSq <= (double) z1 * z1) return 1;
        if (distSq <= (double) z2 * z2) return 2;
        if (distSq <= (double) z3 * z3) return 3;
        if (distSq <= (double) z4 * z4) return 4;
        return 5;
    }

    // Ignoramos Y porque las zonas son horizontales.
    private static double distanceXZSq(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
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