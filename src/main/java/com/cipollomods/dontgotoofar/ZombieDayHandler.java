package com.cipollomods.dontgotoofar;

import net.minecraft.world.entity.ai.goal.FleeSunGoal;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.LivingEntity;
import net.minecraftforge.event.entity.EntityJoinLevelEvent;
import net.minecraftforge.event.entity.living.LivingEvent;

/**
 * Hace que los zombies sean inmunes al fuego durante el día.
 * Al entrar al mundo se elimina su goal de huir del sol, y por tick se apaga
 * cualquier fuego activo si es de día. Ambos comportamientos son opcionales
 * y se desactivan si zombie_fire_immune está a false en el config.
 */
public class ZombieDayHandler {

    /**
     * Se llama cada tick por cada entidad viva — es caro, por eso filtramos
     * lo antes posible (config, tipo, lado) antes de hacer nada.
     */
    public static void onLivingTick(LivingEvent.LivingTickEvent event) {
        if (!ZoneConfig.ZOMBIE_FIRE_IMMUNE.get()) return;

        LivingEntity entity = event.getEntity();
        if (!(entity instanceof Zombie zombie)) return;
        if (zombie.level().isClientSide()) return;

        if (isDay(zombie.level().getDayTime()) && zombie.isOnFire()) {
            zombie.clearFire();
        }
    }

    /**
     * Cuando un zombie entra en el mundo, eliminamos su goal de huir del sol.
     * Esto hace que no busquen sombra de día y campen libremente.
     */
    public static void onEntityJoinLevel(EntityJoinLevelEvent event) {
        if (!ZoneConfig.ZOMBIE_FIRE_IMMUNE.get()) return;
        if (event.getLevel().isClientSide()) return;
        if (!(event.getEntity() instanceof Zombie zombie)) return;

        zombie.goalSelector.getAvailableGoals().removeIf(
                goal -> goal.getGoal() instanceof FleeSunGoal
        );
    }

    // En Minecraft un día dura 24000 ticks. El día va de 0 a 12999.
    private static boolean isDay(long dayTime) {
        long time = dayTime % 24000;
        return time < 13000;
    }
}