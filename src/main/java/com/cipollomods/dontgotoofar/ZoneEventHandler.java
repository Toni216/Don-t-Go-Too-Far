package com.cipollomods.dontgotoofar;

import com.mojang.brigadier.arguments.StringArgumentType;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Mob;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.event.entity.living.MobSpawnEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/**
 * @Mod.EventBusSubscriber hace que Forge registre automáticamente todos los
 * métodos @SubscribeEvent de esta clase. Sin esto, los eventos no se dispararían.
 */
@Mod.EventBusSubscriber(modid = DontGoTooFar.MOD_ID)
public class ZoneEventHandler {

    @SubscribeEvent
    public static void onMobSpawn(MobSpawnEvent.FinalizeSpawn event) {
        Mob mob = event.getEntity();
        if (!(mob.level() instanceof ServerLevel)) return;

        MobStatHandler.applyStats(mob);
    }

    @SubscribeEvent
    public static void onPositionCheck(MobSpawnEvent.PositionCheck event) {
        SpawnRateHandler.onPositionCheck(event);
    }


    @SubscribeEvent
    public static void onLivingHurt(LivingHurtEvent event) {
        if (!(event.getSource().getEntity() instanceof Mob mob)) return;
        if (!(mob.level() instanceof ServerLevel)) return;

        float newDamage = MobStatHandler.applyDamageMultiplier(mob, event.getAmount());
        event.setAmount(newDamage);
    }

    @SubscribeEvent
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        ZombieDayHandler.onLivingTick(event);
    }

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        ZombieDayHandler.onEntityJoinLevel(event);
    }

    // Solo procesamos las hordas al final del tick (Phase.END) y solo en el Overworld.
    @SubscribeEvent
    public static void onServerTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;
        if (serverLevel.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;

        HordeHandler.onServerTick(serverLevel);
    }

    // Al desconectarse un jugador limpiamos su caché para no acumular datos inútiles.
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        ZoneManager.clearCache(player.getUUID());
        HordeHandler.clearCache(player.getUUID());
    }

    @SubscribeEvent
    public static void onRegisterCommands(RegisterCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal("dgtf")

                        // Muestra la zona actual y sus multiplicadores al jugador que lo ejecuta.
                        .then(Commands.literal("info")
                                .executes(ctx -> {
                                    DontGoTooFar.LOGGER.info("[DGTF] /dgtf info ejecutado");
                                    CommandSourceStack source = ctx.getSource();
                                    if (!(source.getEntity() instanceof ServerPlayer player)) {
                                        source.sendFailure(Component.literal("Solo jugadores."));
                                        return 0;
                                    }

                                    int zone = ZoneManager.getZone(player);
                                    String zoneName = ZoneManager.getZoneName(zone);

                                    double damage = MobStatHandler.getDamageMultiplier(zone);
                                    double health = MobStatHandler.getHealthMultiplier(zone);
                                    double speed  = MobStatHandler.getSpeedMultiplier(zone);
                                    double spawn  = MobStatHandler.getSpawnMultiplier(zone);

                                    // Mandamos el mensaje directamente al jugador, sin pasar por sendSuccess
                                    player.sendSystemMessage(Component.literal(
                                            "§6[DGTF] §f" + zoneName + "\n" +
                                                    "§c Daño: §fx" + damage +
                                                    " §a Vida: §fx" + health +
                                                    " §b Velocidad: §fx" + speed +
                                                    " §e Spawn: §fx" + spawn
                                    ));

                                    return 1;
                                })
                        )

                        // Solo admins (nivel 2) pueden consultar la zona de otro jugador.
                        .then(Commands.literal("zone")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("jugador", StringArgumentType.word())
                                        .executes(ctx -> {
                                            CommandSourceStack source = ctx.getSource();
                                            if (!(source.getEntity() instanceof ServerPlayer player)) {
                                                source.sendFailure(Component.literal("Solo jugadores."));
                                                return 0;
                                            }
                                            String targetName = StringArgumentType.getString(ctx, "jugador");
                                            ServerLevel level = source.getLevel();

                                            ServerPlayer target = level.getServer()
                                                    .getPlayerList()
                                                    .getPlayerByName(targetName);

                                            if (target == null) {
                                                player.sendSystemMessage(
                                                        Component.literal("Jugador no encontrado: " + targetName)
                                                );
                                                return 0;
                                            }

                                            int zone = ZoneManager.getZone(target);
                                            String zoneName = ZoneManager.getZoneName(zone);
                                            player.sendSystemMessage(
                                                    Component.literal(targetName + " está en: " + zoneName)
                                            );
                                            return 1;
                                        })
                                )
                        )
        );
    }
}