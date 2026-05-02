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
    private static final Map<UUID, Integer> previousZone = new HashMap<>();

    /**
     * Devuelve la zona del jugador (1-5).
     * Solo recalcula si el jugador se ha movido más de cache_recalc_distance bloques.
     * Cuando la zona cambia, guarda la anterior en previousZone para que
     * ZoneEventHandler pueda detectarlo y notificar al jugador.
     */
    public static int getZone(ServerPlayer player) {
        UUID uuid = player.getUUID();
        Vec3 currentPos = player.position();
        Vec3 lastPos = lastCalcPos.get(uuid);
        int recalcDistanceSq = ZoneConfig.CACHE_RECALC_DISTANCE.get();
        recalcDistanceSq *= recalcDistanceSq;

        if (lastPos == null || distanceXZSq(currentPos, lastPos) >= recalcDistanceSq) {
            int oldZone = zoneCache.getOrDefault(uuid, -1);
            int zone = calculateZone(currentPos);

            if (oldZone != -1 && oldZone != zone) {
                previousZone.put(uuid, oldZone);
            }

            zoneCache.put(uuid, zone);
            lastCalcPos.put(uuid, currentPos);
            return zone;
        }

        return zoneCache.getOrDefault(uuid, 1);
    }

    /**
     * Indica si el jugador acaba de cruzar una frontera de zona en el último recálculo.
     */
    public static boolean hasZoneChanged(UUID uuid) {
        return previousZone.containsKey(uuid);
    }

    /**
     * Elimina y devuelve la zona anterior del jugador.
     * Debe llamarse solo si hasZoneChanged() devuelve true.
     * Se consume al leerla para que la notificación solo se dispare una vez.
     */
    public static int consumePreviousZone(UUID uuid) {
        return previousZone.remove(uuid);
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
        double distSq = pos.x * pos.x + pos.z * pos.z;
        int z1 = ZoneConfig.ZONE1_MAX.get(), z2 = ZoneConfig.ZONE2_MAX.get(),
                z3 = ZoneConfig.ZONE3_MAX.get(), z4 = ZoneConfig.ZONE4_MAX.get();

        if (distSq <= (double) z1 * z1) return 1;
        if (distSq <= (double) z2 * z2) return 2;
        if (distSq <= (double) z3 * z3) return 3;
        if (distSq <= (double) z4 * z4) return 4;
        return 5;
    }

    private static double distanceXZSq(Vec3 a, Vec3 b) {
        double dx = a.x - b.x;
        double dz = a.z - b.z;
        return dx * dx + dz * dz;
    }

    public static void clearCache(UUID uuid) {
        zoneCache.remove(uuid);
        lastCalcPos.remove(uuid);
        previousZone.remove(uuid);
    }

    public static String getZoneName(int zone) {
        return switch (zone) {
            case 1 -> "Zone 1 — Safe Lands";
            case 2 -> "Zone 2 — Frontier";
            case 3 -> "Zone 3 — Wildlands";
            case 4 -> "Zone 4 — The Abyss";
            case 5 -> "Zone 5 — Beyond";
            default -> "Unknown Zone";
        };
    }

    /**
     * Devuelve el código de color de Minecraft para cada zona.
     * Va de verde (segura) a rojo oscuro (máximo peligro).
     */
    public static String getZoneColor(int zone) {
        return switch (zone) {
            case 1 -> "§a";
            case 2 -> "§e";
            case 3 -> "§6";
            case 4 -> "§c";
            case 5 -> "§4";
            default -> "§f";
        };
    }

    /**
     * Devuelve la zona correspondiente a una posición en el mundo, sin depender de jugadores.
     * Se usa al spawnear mobs: en ese momento puede que no haya ningún jugador en el nivel
     * todavía (carga inicial del mundo, chunks precargados, etc.), así que calcular la zona
     * por la posición del propio mob es más fiable y semánticamente correcto — el mob vive
     * en esa zona, independientemente de dónde esté el jugador en ese instante.
     */
    public static int getZoneByPosition(Vec3 pos) {
        return calculateZone(pos);
    }
}