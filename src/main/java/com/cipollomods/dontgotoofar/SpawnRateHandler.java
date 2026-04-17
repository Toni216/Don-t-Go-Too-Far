package com.cipollomods.dontgotoofar;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.eventbus.api.Event;

public class SpawnRateHandler {

    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        Mob mob = event.getEntity();

        if (!(mob.level() instanceof ServerLevel serverLevel)) return;

        String mobId = mob.getType().builtInRegistryHolder().key().location().toString();
        if (!ZoneConfig.AFFECTED_MOBS.get().contains(mobId)) return;

        int zone = getNearestPlayerZone(mob, serverLevel);
        if (zone == -1) return;

        double multiplier = MobStatHandler.getSpawnMultiplier(zone);

        if (multiplier == 1.0) return;

        if (multiplier < 1.0) {
            // Zona segura o poco peligrosa: cancelamos spawns aleatoriamente.
            double cancelChance = 1.0 - multiplier;
            if (serverLevel.random.nextDouble() < cancelChance) {
                event.setResult(Event.Result.DENY);
                DontGoTooFar.LOGGER.debug("[DGTF] Spawn bloqueado para {} en zona {} (prob. cancelación: {}%)",
                        mob.getType().toShortString(), zone, (int)(cancelChance * 100));
            }
        } else {
            // multiplier > 1.0: intentamos spawnear mobs extra con probabilidad acumulada.
            // Ejemplo: 2.5 → spawna 2 extras garantizados + 50% de un tercero.
            double extraSpawns = multiplier - 1.0;

            // Parte entera: spawns extra garantizados.
            int guaranteedExtras = (int) extraSpawns;

            // Parte decimal: probabilidad de un spawn extra adicional.
            double partialChance = extraSpawns - guaranteedExtras;

            int totalExtras = guaranteedExtras + (serverLevel.random.nextDouble() < partialChance ? 1 : 0);

            for (int i = 0; i < totalExtras; i++) {
                Mob extra = (Mob) mob.getType().create(serverLevel);
                if (extra == null) continue;

                extra.moveTo(mob.getX(), mob.getY(), mob.getZ(), mob.getYRot(), 0);
                MobStatHandler.applyStats(extra);
                serverLevel.addFreshEntity(extra);
            }

            if (totalExtras > 0) {
                DontGoTooFar.LOGGER.debug("[DGTF] Spawn extra x{} para {} en zona {} (multiplicador: {})",
                        totalExtras, mob.getType().toShortString(), zone, multiplier);
            }
        }
    }

    /**
     * Busca el jugador superviviente más cercano al mob y devuelve su zona.
     * Devuelve -1 si no hay ningún jugador válido en el nivel.
     */
    private static int getNearestPlayerZone(Mob mob, ServerLevel level) {
        return level.players().stream()
                .filter(p -> !p.isSpectator() && !p.isCreative())
                .min((a, b) -> Double.compare(a.distanceToSqr(mob), b.distanceToSqr(mob)))
                .map(ZoneManager::getZone)
                .orElse(-1);
    }
}