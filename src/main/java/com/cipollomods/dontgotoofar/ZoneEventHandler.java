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
 * Punto de entrada de todos los eventos de Forge para el mod.
 * Delega cada evento al handler correspondiente y registra los comandos /dgtf.
 * La anotación @Mod.EventBusSubscriber hace que Forge registre automáticamente
 * todos los métodos @SubscribeEvent de esta clase.
 */
@Mod.EventBusSubscriber(modid = DontGoTooFar.MOD_ID)
public class ZoneEventHandler {

    @SubscribeEvent
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (event.getLevel().isClientSide()) return;
        if (event.getEntity() instanceof Mob mob) {
            MobStatHandler.applyStats(mob);
        }
        ZombieDayHandler.onEntityJoinLevel(event);
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
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.player instanceof ServerPlayer player)) return;
        if (player.isSpectator() || player.isCreative()) return;

        int zone = ZoneManager.getZone(player);

        if (ZoneManager.hasZoneChanged(player.getUUID())) {
            ZoneManager.consumePreviousZone(player.getUUID());
            String color = ZoneManager.getZoneColor(zone);
            String name = ZoneManager.getZoneName(zone);
            player.displayClientMessage(Component.literal(color + name), true);
        }
    }

    @SubscribeEvent
    public static void onServerTick(TickEvent.LevelTickEvent event) {
        if (event.phase != TickEvent.Phase.END) return;
        if (!(event.level instanceof ServerLevel serverLevel)) return;
        if (serverLevel.dimension() != net.minecraft.world.level.Level.OVERWORLD) return;

        HordeHandler.onServerTick(serverLevel);
    }

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

                        .then(Commands.literal("info")
                                .executes(ctx -> {
                                    DontGoTooFar.LOGGER.info("[DGTF] /dgtf info ejecutado");
                                    CommandSourceStack source = ctx.getSource();
                                    if (!(source.getEntity() instanceof ServerPlayer player)) {
                                        source.sendFailure(Component.literal("Players only."));
                                        return 0;
                                    }

                                    int zone = ZoneManager.getZone(player);
                                    String color = ZoneManager.getZoneColor(zone);
                                    String zoneName = ZoneManager.getZoneName(zone);

                                    double damage = MobStatHandler.getDamageMultiplier(zone);
                                    double health = MobStatHandler.getHealthMultiplier(zone);
                                    double speed  = MobStatHandler.getSpeedMultiplier(zone);
                                    double spawn  = MobStatHandler.getSpawnMultiplier(zone);

                                    player.sendSystemMessage(Component.literal(
                                            "§6[DGTF] " + color + zoneName + "\n" +
                                                    "§c Damage: §fx" + damage +
                                                    " §a Health: §fx" + health +
                                                    " §b Speed: §fx" + speed +
                                                    " §e Spawn: §fx" + spawn
                                    ));

                                    return 1;
                                })
                        )

                        .then(Commands.literal("zone")
                                .requires(src -> src.hasPermission(2))
                                .then(Commands.argument("jugador", StringArgumentType.word())
                                        .executes(ctx -> {
                                            CommandSourceStack source = ctx.getSource();
                                            if (!(source.getEntity() instanceof ServerPlayer player)) {
                                                source.sendFailure(Component.literal("Players only."));
                                                return 0;
                                            }
                                            String targetName = StringArgumentType.getString(ctx, "jugador");
                                            ServerLevel level = source.getLevel();

                                            ServerPlayer target = level.getServer()
                                                    .getPlayerList()
                                                    .getPlayerByName(targetName);

                                            if (target == null) {
                                                player.sendSystemMessage(
                                                        Component.literal("Player not found: " + targetName)
                                                );
                                                return 0;
                                            }

                                            int zone = ZoneManager.getZone(target);
                                            String color = ZoneManager.getZoneColor(zone);
                                            String zoneName = ZoneManager.getZoneName(zone);
                                            player.sendSystemMessage(
                                                    Component.literal(targetName + " is in: " + color + zoneName)
                                            );
                                            return 1;
                                        })
                                )
                        )
        );
    }
}