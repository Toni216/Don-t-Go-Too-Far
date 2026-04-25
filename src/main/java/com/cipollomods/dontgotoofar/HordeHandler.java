package com.cipollomods.dontgotoofar;

import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class HordeHandler {

    /**
     * Guardamos el tick de la última oleada y las oleadas restantes por jugador.
     * Usamos UUID como clave para identificar a cada jugador de forma única.
     */
    private static final Map<UUID, Long> lastWaveTick = new HashMap<>();
    private static final Map<UUID, Integer> wavesRemaining = new HashMap<>();

    public static void onServerTick(ServerLevel level) {
        if (!ZoneConfig.HORDES_ENABLED.get()) return;

        // Al amanecer reseteamos las hordas para que vuelvan a empezar la noche siguiente.
        if (!isNight(level)) {
            resetHordes(level);
            return;
        }

        long currentTick = level.getGameTime();
        int intervalTicks = ZoneConfig.HORDE_WAVE_INTERVAL_TICKS.get();

        for (ServerPlayer player : level.players()) {
            if (player.isSpectator() || player.isCreative()) continue;

            UUID uuid = player.getUUID();
            int zone = ZoneManager.getZone(player);
            int maxWaves = getMaxWaves(zone);

            // Primera vez que vemos a este jugador esta noche: inicializamos su horda.
            if (!wavesRemaining.containsKey(uuid)) {
                wavesRemaining.put(uuid, maxWaves);
            }

            if (wavesRemaining.get(uuid) <= 0) continue;

            long lastWave = lastWaveTick.getOrDefault(uuid, 0L);
            if (currentTick - lastWave >= intervalTicks) {
                spawnWave(level, player, zone);
                lastWaveTick.put(uuid, currentTick);
                wavesRemaining.put(uuid, wavesRemaining.get(uuid) - 1);
            }
        }
    }

    private static void spawnWave(ServerLevel level, ServerPlayer player, int zone) {
        int mobsPerWave = ZoneConfig.HORDE_ZOMBIES_PER_WAVE.get();
        BlockPos playerPos = player.blockPosition();
        List<EntityType<?>> pool = buildSpawnPool(level);

        if (pool.isEmpty()) {
            DontGoTooFar.LOGGER.warn("[DGTF] Horda: pool de mobs vacío, revisa horde_mobs en el config.");
            return;
        }

        for (int i = 0; i < mobsPerWave; i++) {
            // Posición aleatoria en un radio de 16 bloques alrededor del jugador.
            int offsetX = (level.random.nextInt(32) - 16);
            int offsetZ = (level.random.nextInt(32) - 16);
            BlockPos spawnPos = findSafeSpawnPos(level, playerPos.offset(offsetX, 0, offsetZ));
            if (spawnPos == null) continue;

            EntityType<?> entityType = pool.get(level.random.nextInt(pool.size()));
            Mob mob = (Mob) entityType.create(level);
            if (mob == null) continue;

            mob.moveTo(spawnPos.getX() + 0.5, spawnPos.getY(), spawnPos.getZ() + 0.5, 0, 0);
            // FIX: applyStats se elimina aquí. El evento FinalizeSpawn en ZoneEventHandler
            // lo llama automáticamente al hacer addFreshEntity, así que aplicarlo aquí
            // causaba que los stats se duplicasen (doble vida, doble velocidad, etc.).
            level.addFreshEntity(mob);
        }

        DontGoTooFar.LOGGER.debug("[DGTF] Horda: oleada spawneada para {} en zona {} | Oleadas restantes: {}",
                player.getName().getString(), zone, wavesRemaining.get(player.getUUID()));
    }

    /**
     * Construye una lista ponderada de EntityType a partir de la config.
     * Cada entrada tiene formato "namespace:mob_id:peso" (ej: "minecraft:zombie:70").
     * Añadimos el EntityType tantas veces como indique su peso, luego elegimos al azar.
     * Así un peso de 70 sobre 100 total da exactamente un 70% de probabilidad.
     */
    private static List<EntityType<?>> buildSpawnPool(ServerLevel level) {
        List<EntityType<?>> pool = new ArrayList<>();

        for (String entry : ZoneConfig.HORDE_MOBS.get()) {
            String[] parts = entry.split(":");
            if (parts.length != 3) {
                DontGoTooFar.LOGGER.warn("[DGTF] Entrada inválida en horde_mobs: '{}' (formato: mod:mob_id:peso)", entry);
                continue;
            }

            String namespace = parts[0];
            String path = parts[1];
            int weight;

            try {
                weight = Integer.parseInt(parts[2]);
            } catch (NumberFormatException e) {
                DontGoTooFar.LOGGER.warn("[DGTF] Peso inválido en horde_mobs: '{}'", entry);
                continue;
            }

            ResourceLocation rl = ResourceLocation.fromNamespaceAndPath(namespace, path);
            EntityType<?> entityType = level.registryAccess()
                    .registryOrThrow(net.minecraft.core.registries.Registries.ENTITY_TYPE)
                    .get(rl);

            if (entityType == null) {
                DontGoTooFar.LOGGER.warn("[DGTF] Mob no encontrado: '{}'", rl);
                continue;
            }

            for (int i = 0; i < weight; i++) {
                pool.add(entityType);
            }
        }

        return pool;
    }

    /**
     * Busca una posición segura bajando hasta 5 bloques desde la posición dada.
     * Necesita suelo sólido debajo y dos bloques de aire para que el mob quepa.
     */
    private static BlockPos findSafeSpawnPos(ServerLevel level, BlockPos pos) {
        for (int dy = 0; dy >= -5; dy--) {
            BlockPos check = pos.offset(0, dy, 0);
            BlockState floor = level.getBlockState(check.below());
            BlockState feet = level.getBlockState(check);
            BlockState head = level.getBlockState(check.above());

            if (floor.isSolidRender(level, check.below())
                    && feet.isAir()
                    && head.isAir()) {
                return check;
            }
        }
        return null;
    }

    // El número de oleadas por noche escala con la zona: zona 1 = 1 oleada, zona 5 = 5 oleadas.
    private static int getMaxWaves(int zone) {
        return switch (zone) {
            case 1 -> 1;
            case 2 -> 2;
            case 3 -> 3;
            case 4 -> 4;
            case 5 -> 5;
            default -> 1;
        };
    }

    private static boolean isNight(ServerLevel level) {
        long time = level.getDayTime() % 24000;
        return time >= 13000 && time <= 23000;
    }

    private static void resetHordes(ServerLevel level) {
        for (ServerPlayer player : level.players()) {
            UUID uuid = player.getUUID();
            wavesRemaining.remove(uuid);
            // FIX: también limpiamos lastWaveTick al amanecer, para que la primera
            // oleada de la noche siguiente no espere el tiempo residual del día anterior.
            lastWaveTick.remove(uuid);
        }
    }

    public static void clearCache(UUID uuid) {
        lastWaveTick.remove(uuid);
        wavesRemaining.remove(uuid);
    }
}