package com.cipollomods.dontgotoofar;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Aplica los multiplicadores de zona a los atributos de los mobs afectados.
 * La vida y la velocidad se modifican al spawnear (FinalizeSpawn), el daño
 * se intercepta en LivingHurtEvent para no alterar el atributo base del mob.
 * Solo actúa sobre los mobs definidos en affected_mobs del config.
 */
public class MobStatHandler {

    /**
     * Cache local del Set de mobs afectados para evitar convertir la lista del config
     * en cada comprobación. Se invalida si la referencia de la lista cambia.
     */
    private static Set<String> affectedMobsCache = null;
    private static List<? extends String> lastSeenList = null;

    private static Set<String> getAffectedMobs() {
        List<? extends String> current = ZoneConfig.AFFECTED_MOBS.get();
        if (affectedMobsCache == null || current != lastSeenList) {
            affectedMobsCache = new HashSet<>(ZoneConfig.AFFECTED_MOBS.get());
            lastSeenList = current;
        }
        return affectedMobsCache;
    }

    private static boolean isAffected(Mob mob) {
        String mobId = mob.getType().builtInRegistryHolder().key().location().toString();
        return getAffectedMobs().contains(mobId);
    }

    /**
     * Se llama al spawnear un mob. Aplica vida y velocidad según la zona.
     * El daño se aplica en LivingHurtEvent, no aquí.
     */
    public static void applyStats(Mob mob) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;
        if (!isAffected(mob)) return;

        // Calcular la zona por posición del mob, no por jugador cercano
        int zone = ZoneManager.getZoneByPosition(mob.position());

        applyHealth(mob, zone);
        applySpeed(mob, zone);
    }

    /**
     * Se llama desde LivingHurtEvent. Devuelve el daño modificado según la zona
     * del jugador más cercano al mob.
     */
    public static float applyDamageMultiplier(Mob mob, float originalDamage) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return originalDamage;
        if (!isAffected(mob)) return originalDamage;

        int zone = ZoneManager.getNearestPlayerZone(mob, serverLevel);
        if (zone == -1) return originalDamage;

        double multiplier = getDamageMultiplier(zone);
        float newDamage = (float) (originalDamage * multiplier);

        DontGoTooFar.LOGGER.debug("[DGTF] {} golpea en zona {} | Daño: {} -> {}",
                mob.getType().toShortString(), zone, originalDamage, newDamage);

        return newDamage;
    }

    /**
     * Cambiamos el valor base del atributo y actualizamos la vida actual.
     * Sin setHealth(), el mob aparecería con la vida anterior aunque su máximo sea mayor.
     */
    private static void applyHealth(Mob mob, int zone) {
        AttributeInstance healthAttr = mob.getAttribute(Attributes.MAX_HEALTH);
        if (healthAttr == null) return;

        double baseHealth = healthAttr.getBaseValue();
        double multiplier = getHealthMultiplier(zone);

        healthAttr.setBaseValue(baseHealth * multiplier);
        mob.setHealth((float) healthAttr.getValue());

        DontGoTooFar.LOGGER.debug("[DGTF] {} spawneado en zona {} | Vida: {} -> {}",
                mob.getType().toShortString(), zone, baseHealth, healthAttr.getValue());
    }

    private static void applySpeed(Mob mob, int zone) {
        AttributeInstance speedAttr = mob.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speedAttr == null) return;

        double baseSpeed = speedAttr.getBaseValue();
        speedAttr.setBaseValue(baseSpeed * getSpeedMultiplier(zone));

        DontGoTooFar.LOGGER.debug("[DGTF] {} | Velocidad: {} -> {}",
                mob.getType().toShortString(), baseSpeed, speedAttr.getValue());
    }

    public static double getDamageMultiplier(int zone) {
        return switch (zone) {
            case 1 -> ZoneConfig.Z1_DAMAGE.get();
            case 2 -> ZoneConfig.Z2_DAMAGE.get();
            case 3 -> ZoneConfig.Z3_DAMAGE.get();
            case 4 -> ZoneConfig.Z4_DAMAGE.get();
            case 5 -> ZoneConfig.Z5_DAMAGE.get();
            default -> 1.0;
        };
    }

    public static double getHealthMultiplier(int zone) {
        return switch (zone) {
            case 1 -> ZoneConfig.Z1_HEALTH.get();
            case 2 -> ZoneConfig.Z2_HEALTH.get();
            case 3 -> ZoneConfig.Z3_HEALTH.get();
            case 4 -> ZoneConfig.Z4_HEALTH.get();
            case 5 -> ZoneConfig.Z5_HEALTH.get();
            default -> 1.0;
        };
    }

    public static double getSpeedMultiplier(int zone) {
        return switch (zone) {
            case 1 -> ZoneConfig.Z1_SPEED.get();
            case 2 -> ZoneConfig.Z2_SPEED.get();
            case 3 -> ZoneConfig.Z3_SPEED.get();
            case 4 -> ZoneConfig.Z4_SPEED.get();
            case 5 -> ZoneConfig.Z5_SPEED.get();
            default -> 1.0;
        };
    }

    public static double getSpawnMultiplier(int zone) {
        return switch (zone) {
            case 1 -> ZoneConfig.Z1_SPAWN.get();
            case 2 -> ZoneConfig.Z2_SPAWN.get();
            case 3 -> ZoneConfig.Z3_SPAWN.get();
            case 4 -> ZoneConfig.Z4_SPAWN.get();
            case 5 -> ZoneConfig.Z5_SPAWN.get();
            default -> 1.0;
        };
    }
}