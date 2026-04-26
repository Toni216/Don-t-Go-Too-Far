package com.cipollomods.dontgotoofar;

import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.config.ModConfig;
import java.util.List;

/**
 * Define y registra la configuración del servidor en dontgotoofar-server.toml.
 * Expone los límites de cada zona, los multiplicadores de daño/vida/velocidad/spawn
 * por zona, y los parámetros de hordas y optimización de caché.
 */
public class ZoneConfig {

    // SERVER_SPEC es el objeto que Forge usa para leer y validar el archivo .toml.
    public static final ForgeConfigSpec SERVER_SPEC;

    /**
     * Límites de distancia para cada zona. La zona 5 no tiene límite: es todo lo que
     * supere ZONE4_MAX.
     */

    public static ForgeConfigSpec.IntValue ZONE1_MAX;
    public static ForgeConfigSpec.IntValue ZONE2_MAX;
    public static ForgeConfigSpec.IntValue ZONE3_MAX;
    public static ForgeConfigSpec.IntValue ZONE4_MAX;

    /**
     * Multiplicadores por zona. Cada zona tiene 4 valores independientes:
     * daño, vida, velocidad y spawn rate.
     */
    public static ForgeConfigSpec.DoubleValue Z1_DAMAGE, Z1_HEALTH, Z1_SPEED, Z1_SPAWN;
    public static ForgeConfigSpec.DoubleValue Z2_DAMAGE, Z2_HEALTH, Z2_SPEED, Z2_SPAWN;
    public static ForgeConfigSpec.DoubleValue Z3_DAMAGE, Z3_HEALTH, Z3_SPEED, Z3_SPAWN;
    public static ForgeConfigSpec.DoubleValue Z4_DAMAGE, Z4_HEALTH, Z4_SPEED, Z4_SPAWN;
    public static ForgeConfigSpec.DoubleValue Z5_DAMAGE, Z5_HEALTH, Z5_SPEED, Z5_SPAWN;

    public static ForgeConfigSpec.BooleanValue ZOMBIE_FIRE_IMMUNE;
    public static ForgeConfigSpec.BooleanValue HORDES_ENABLED;
    public static ForgeConfigSpec.IntValue HORDE_ZOMBIES_PER_WAVE;
    public static ForgeConfigSpec.IntValue HORDE_WAVE_INTERVAL_TICKS;
    public static ForgeConfigSpec.IntValue CACHE_RECALC_DISTANCE;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> AFFECTED_MOBS;
    public static ForgeConfigSpec.ConfigValue<List<? extends String>> HORDE_MOBS;

    /**
     * El bloque static se ejecuta una sola vez al cargar la clase.
     * Aquí definimos la estructura del archivo .toml con push/pop para las secciones.
     */
    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Danger zone distance limits").push("zones");
        ZONE1_MAX = builder.comment("Maximum distance for zone 1").defineInRange("zone1_max", 500, 1, Integer.MAX_VALUE);
        ZONE2_MAX = builder.comment("Maximum distance for zone 2").defineInRange("zone2_max", 1000, 1, Integer.MAX_VALUE);
        ZONE3_MAX = builder.comment("Maximum distance for zone 3").defineInRange("zone3_max", 1500, 1, Integer.MAX_VALUE);
        ZONE4_MAX = builder.comment("Maximum distance for zone 4").defineInRange("zone4_max", 2000, 1, Integer.MAX_VALUE);
        builder.pop();

        builder.comment("Multipliers for Zone 1 (safe zone)").push("zone1");
        Z1_DAMAGE = builder.defineInRange("damage", 1.0, 0.1, 10.0);
        Z1_HEALTH = builder.defineInRange("health", 1.0, 0.1, 10.0);
        Z1_SPEED  = builder.defineInRange("speed",  1.0, 0.1, 10.0);
        Z1_SPAWN  = builder.defineInRange("spawn_rate", 1.0, 0.1, 10.0);
        builder.pop();

        builder.comment("Multipliers for Zone 2").push("zone2");
        Z2_DAMAGE = builder.defineInRange("damage", 1.3, 0.1, 10.0);
        Z2_HEALTH = builder.defineInRange("health", 1.3, 0.1, 10.0);
        Z2_SPEED  = builder.defineInRange("speed",  1.2, 0.1, 10.0);
        Z2_SPAWN  = builder.defineInRange("spawn_rate", 1.3, 0.1, 10.0);
        builder.pop();

        builder.comment("Multipliers for Zone 3").push("zone3");
        Z3_DAMAGE = builder.defineInRange("damage", 1.7, 0.1, 10.0);
        Z3_HEALTH = builder.defineInRange("health", 1.7, 0.1, 10.0);
        Z3_SPEED  = builder.defineInRange("speed",  1.4, 0.1, 10.0);
        Z3_SPAWN  = builder.defineInRange("spawn_rate", 1.7, 0.1, 10.0);
        builder.pop();

        builder.comment("Multipliers for Zone 4").push("zone4");
        Z4_DAMAGE = builder.defineInRange("damage", 2.2, 0.1, 10.0);
        Z4_HEALTH = builder.defineInRange("health", 2.2, 0.1, 10.0);
        Z4_SPEED  = builder.defineInRange("speed",  1.7, 0.1, 10.0);
        Z4_SPAWN  = builder.defineInRange("spawn_rate", 2.0, 0.1, 10.0);
        builder.pop();

        builder.comment("Multipliers for Zone 5").push("zone5");
        Z5_DAMAGE = builder.defineInRange("damage", 3.0, 0.1, 10.0);
        Z5_HEALTH = builder.defineInRange("health", 3.0, 0.1, 10.0);
        Z5_SPEED  = builder.defineInRange("speed",  2.0, 0.1, 10.0);
        Z5_SPAWN  = builder.defineInRange("spawn_rate", 2.5, 0.1, 10.0);
        builder.pop();

        builder.comment("Performance optimization").push("optimization");
        CACHE_RECALC_DISTANCE = builder.comment("Blocks a player must move before their zone is recalculated")
                .defineInRange("cache_recalc_distance", 16, 1, 256);
        builder.pop();

        builder.comment("Extra mechanics").push("extras");
        ZOMBIE_FIRE_IMMUNE = builder.comment("Zombies are immune to fire damage during daytime")
                .define("zombie_fire_immune", true);
        HORDES_ENABLED = builder.comment("Enable nightly zombie hordes")
                .define("hordes_enabled", true);
        HORDE_ZOMBIES_PER_WAVE = builder.comment("Number of mobs spawned per horde wave")
                .defineInRange("zombies_per_wave", 5, 1, 50);
        HORDE_WAVE_INTERVAL_TICKS = builder.comment("Ticks between horde waves (20 ticks = 1 second)")
                .defineInRange("wave_interval_ticks", 40, 20, 1200);
        AFFECTED_MOBS = builder.comment(
                "Mobs affected by zone multipliers (format: mod:mob_id).\n" +
                "Add mobs from other mods using their namespace, e.g. 'alexsmobs:grizzly_bear'.\n" +
                "Remove any entry to exclude that mob from the zone system."
        ).defineList("affected_mobs", List.of(
                "minecraft:zombie",
                "minecraft:zombie_villager",
                "minecraft:husk",
                "minecraft:drowned",
                "minecraft:zombified_piglin",
                "minecraft:skeleton",
                "minecraft:stray",
                "minecraft:wither_skeleton",
                "minecraft:bogged",
                "minecraft:spider",
                "minecraft:cave_spider",
                "minecraft:creeper",
                "minecraft:witch",
                "minecraft:pillager",
                "minecraft:vindicator",
                "minecraft:ravager",
                "minecraft:phantom",
                "minecraft:enderman",
                "minecraft:blaze",
                "minecraft:ghast",
                "minecraft:magma_cube",
                "minecraft:slime"
        ), entry -> entry instanceof String);
        HORDE_MOBS = builder.comment("Mobs that spawn in hordes with their weights (format: mod:mob_id:weight)")
                .defineList("horde_mobs", List.of(
                        "minecraft:zombie:70",
                        "minecraft:husk:20",
                        "minecraft:drowned:10"
                ), entry -> entry instanceof String);
        builder.pop();

        SERVER_SPEC = builder.build();
    }

    public static void register() {
        ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, SERVER_SPEC, "dontgotoofar-server.toml");
    }
}