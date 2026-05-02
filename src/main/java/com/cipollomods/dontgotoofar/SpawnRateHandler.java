package com.cipollomods.dontgotoofar;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;

/**
 * Controla la tasa de spawn de mobs según la zona del jugador más cercano.
 * Con multiplicador < 1.0 cancela spawns probabilísticamente; con multiplicador > 1.0
 * spawnea mobs extra usando la parte entera como garantizados y la decimal como probabilidad.
 */
public class SpawnRateHandler {

    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        Mob mob = event.getEntity();
        if (!(mob.level() instanceof ServerLevel serverLevel)) return;

        String mobId = mob.getType().builtInRegistryHolder().key().location().toString();
        if (!ZoneConfig.AFFECTED_MOBS.get().contains(mobId)) return;

        int zone = ZoneManager.getNearestPlayerZone(mob, serverLevel);
        if (zone == -1) return;

        double multiplier = MobStatHandler.getSpawnMultiplier(zone);
        if (multiplier == 1.0) return;

        if (multiplier < 1.0) {
            double cancelChance = 1.0 - multiplier;
            if (serverLevel.random.nextDouble() < cancelChance) {
                event.setResult(Event.Result.DENY);
                DontGoTooFar.LOGGER.debug("[DGTF] Spawn bloqueado para {} en zona {} (prob. cancelación: {}%)",
                        mob.getType().toShortString(), zone, (int)(cancelChance * 100));
            }
        } else {
            // multiplier > 1.0: parte entera = extras garantizados, parte decimal = probabilidad de uno más.
            double extraSpawns = multiplier - 1.0;
            int guaranteedExtras = (int) extraSpawns;
            double partialChance = extraSpawns - guaranteedExtras;
            int totalExtras = guaranteedExtras + (serverLevel.random.nextDouble() < partialChance ? 1 : 0);

            for (int i = 0; i < totalExtras; i++) {
                Mob extra = (Mob) mob.getType().create(serverLevel);
                if (extra == null) continue;

                extra.moveTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), 0);
                serverLevel.addFreshEntity(extra);
            }

            if (totalExtras > 0) {
                DontGoTooFar.LOGGER.debug("[DGTF] Spawn extra x{} para {} en zona {} (multiplicador: {})",
                        totalExtras, mob.getType().toShortString(), zone, multiplier);
            }
        }
    }
}