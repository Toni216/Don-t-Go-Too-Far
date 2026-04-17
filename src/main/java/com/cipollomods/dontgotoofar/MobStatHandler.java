package com.cipollomods.dontgotoofar;

import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.Optional;

public class MobStatHandler {

    /**
     * Obtenemos el ID del mob (ej: "minecraft:zombie") y comprobamos si está
     * en la lista affected_mobs del config. Si no está, no le aplicamos nada.
     */
    private static boolean isAffected(Mob mob) {
        String mobId = mob.getType().builtInRegistryHolder().key().location().toString();
        return ZoneConfig.AFFECTED_MOBS.get().contains(mobId);
    }

    /**
     * Se llama al spawnear un mob. Aplica vida y velocidad según la zona.
     * El daño se aplica en el evento de golpe, no aquí.
     */
    public static void applyStats(Mob mob) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;
        if (!isAffected(mob)) return;

        int zone = getNearestPlayerZone(mob, serverLevel);
        if (zone == -1) return;

        applyHealth(mob, zone);
        applySpeed(mob, zone);
    }

    /**
     * Se llama desde el evento LivingHurtEvent, justo antes de aplicar el daño.
     * Devuelve el daño modificado según la zona del jugador más cercano al mob.
     */
    public static float applyDamageMultiplier(Mob mob, float originalDamage) {
        if (!(mob.level() instanceof ServerLevel serverLevel)) return originalDamage;
        if (!isAffected(mob)) return originalDamage;

        int zone = getNearestPlayerZone(mob, serverLevel);
        if (zone == -1) return originalDamage;

        double multiplier = getDamageMultiplier(zone);
        float newDamage = (float) (originalDamage * multiplier);

        DontGoTooFar.LOGGER.debug("[DGTF] {} golpea en zona {} | Daño: {} -> {}",
                mob.getType().toShortString(), zone, originalDamage, newDamage);

        return newDamage;
    }

    /**
     * Cambiamos el valor base del atributo y actualizamos la vida actual.
     * Si solo cambiáramos el base sin llamar a setHealth(), el mob aparecería
     * con la vida anterior aunque su máximo sea mayor.
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
        double multiplier = getSpeedMultiplier(zone);
        speedAttr.setBaseValue(baseSpeed * multiplier);

        DontGoTooFar.LOGGER.debug("[DGTF] {} | Velocidad: {} -> {}",
                mob.getType().toShortString(), baseSpeed, speedAttr.getValue());
    }

    /**
     * Busca el jugador superviviente más cercano al mob y devuelve su zona.
     * Devuelve -1 si no hay ningún jugador válido en el nivel.
     */
    private static int getNearestPlayerZone(Mob mob, ServerLevel level) {
        Optional<ServerPlayer> nearest = level.players().stream()
                .filter(p -> !p.isSpectator() && !p.isCreative())
                .min((a, b) -> Double.compare(a.distanceToSqr(mob), b.distanceToSqr(mob)));

        return nearest.map(ZoneManager::getZone).orElse(-1);
    }

    /**
     * Los siguientes métodos leen el multiplicador correspondiente del config
     * según la zona. El default de 1.0 no altera nada si la zona es desconocida.
     */
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