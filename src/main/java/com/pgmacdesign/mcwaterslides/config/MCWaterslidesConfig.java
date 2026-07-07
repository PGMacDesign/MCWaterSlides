package com.pgmacdesign.mcwaterslides.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config. Keys land here with the feature that reads them; the full surface is
 * specified in docs/blocks-recipes-spec.html §5.
 */
public final class MCWaterslidesConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue SPEED_CAP;
    public static final ModConfigSpec.DoubleValue CHANNEL_DRAG;
    public static final ModConfigSpec.DoubleValue OPEN_WATER_DRAG;
    public static final ModConfigSpec.DoubleValue SLOPE_EXCHANGE;
    public static final ModConfigSpec.IntValue RIDE_CONTINUITY_TICKS;
    public static final ModConfigSpec.DoubleValue MIN_START_SPEED;
    public static final ModConfigSpec.DoubleValue JET_THRUST;
    public static final ModConfigSpec.IntValue JET_RANGE;
    public static final ModConfigSpec.IntValue JET_IDLE_RF;
    public static final ModConfigSpec.IntValue JET_PUSH_RF;
    public static final ModConfigSpec.IntValue JET_BUFFER_RF;
    public static final ModConfigSpec.IntValue MAX_PUSHED_ENTITIES_PER_JET;
    public static final ModConfigSpec.IntValue PUMP_HOUSE_RF_PER_TICK;
    public static final ModConfigSpec.IntValue PUMP_HOUSE_BURN_MULTIPLIER;
    public static final ModConfigSpec.IntValue PUMP_HOUSE_PASSIVE_RF;
    public static final ModConfigSpec.IntValue FLOOD_VALVE_RF_PER_BLOCK;
    public static final ModConfigSpec.IntValue FLOOD_VALVE_MAX_VOLUME;
    public static final ModConfigSpec.BooleanValue MOBS_RIDE;
    public static final ModConfigSpec.BooleanValue ITEMS_RIDE;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Ride physics").push("ride");
        SPEED_CAP = builder
                .comment("Momentum ceiling for slide riders, in blocks per second.",
                        "Default 22 sits deliberately under the server movement-check ceiling",
                        "(sprint ~5.6, elytra cruise ~30+).")
                .defineInRange("speedCap", 22.0, 1.0, 30.0);
        CHANNEL_DRAG = builder
                .comment("Momentum lost per tick inside channels/tubes (fraction).",
                        "Default 0.01 coasts ~100 blocks from full speed.")
                .defineInRange("channelDrag", 0.01, 0.0, 0.5);
        OPEN_WATER_DRAG = builder
                .comment("Momentum lost per tick in freeform water volumes (fraction).")
                .defineInRange("openWaterDrag", 0.02, 0.0, 0.5);
        SLOPE_EXCHANGE = builder
                .comment("Momentum gained per block descended / lost per block climbed (b/s).",
                        "Symmetric by design: an upward jet re-pays exactly what a climb costs.")
                .defineInRange("slopeExchange", 2.0, 0.0, 10.0);
        RIDE_CONTINUITY_TICKS = builder
                .comment("Airborne ticks before a ride ends (the ramp-jump window). 30 = 1.5s.")
                .defineInRange("rideContinuityTicks", 30, 0, 200);
        MIN_START_SPEED = builder
                .comment("Horizontal speed (b/s) needed to start riding a flat channel.",
                        "Slopes always start you regardless.")
                .defineInRange("minStartSpeed", 1.0, 0.0, 10.0);
        builder.pop();

        builder.comment("Riders").push("riders");
        MOBS_RIDE = builder
                .comment("Mobs ride slides and get pushed by currents (villager waterparks, pig races).")
                .define("mobsRide", true);
        ITEMS_RIDE = builder
                .comment("Dropped items get pushed by currents (slides quietly double as item transport).")
                .define("itemsRide", true);
        builder.pop();

        builder.comment("Jets").push("jet");
        JET_THRUST = builder
                .comment("Acceleration a jet's current applies to riders, in blocks/sec per second.")
                .defineInRange("thrust", 8.0, 0.0, 100.0);
        JET_RANGE = builder
                .comment("How far a jet's current travels through connected water/channels (blocks).")
                .defineInRange("range", 24, 1, 128);
        JET_IDLE_RF = builder
                .comment("RF per tick to keep a current energized with no riders in it.")
                .defineInRange("idleRf", 4, 0, Integer.MAX_VALUE);
        JET_PUSH_RF = builder
                .comment("Additional RF per tick while at least one rider is being pushed.")
                .defineInRange("pushRf", 80, 0, Integer.MAX_VALUE);
        JET_BUFFER_RF = builder
                .comment("Internal RF buffer per jet — rides don't stutter on a flickering supply.")
                .defineInRange("bufferRf", 20_000, 0, Integer.MAX_VALUE);
        MAX_PUSHED_ENTITIES_PER_JET = builder
                .comment("Per-tick entity cap per jet (mega-park performance guard).")
                .defineInRange("maxPushedEntitiesPerJet", 16, 1, 1024);
        builder.pop();

        builder.comment("Pump House").push("pumpHouse");
        PUMP_HOUSE_RF_PER_TICK = builder
                .comment("RF generated per tick while burning fuel.",
                        "Parity note: MC3DPrint's Clock Generator is 10 RF/t at a 10x burn",
                        "multiplier = 160,000 RF per coal. Defaults here (20 RF/t at 5x) deliver",
                        "the SAME total energy per coal at double rate — never an efficiency",
                        "upgrade over it, deliberately.")
                .defineInRange("pumpHouseRfPerTick", 20, 1, Integer.MAX_VALUE);
        PUMP_HOUSE_BURN_MULTIPLIER = builder
                .comment("Fuel efficiency: furnace burn time is multiplied by this",
                        "(default 5: one coal burns ~6.7 minutes = 160,000 RF at 20 RF/t).")
                .defineInRange("pumpHouseBurnMultiplier", 5, 1, 1_000);
        PUMP_HOUSE_PASSIVE_RF = builder
                .comment("Token passive RF/t while adjacent to water and not burning (lazy rivers).")
                .defineInRange("pumpHousePassiveRf", 2, 0, Integer.MAX_VALUE);
        builder.pop();

        builder.comment("Flood Valve").push("floodValve");
        FLOOD_VALVE_RF_PER_BLOCK = builder
                .comment("RF per water block placed while filling. Draining is free.")
                .defineInRange("rfPerBlock", 10, 0, Integer.MAX_VALUE);
        FLOOD_VALVE_MAX_VOLUME = builder
                .comment("Largest sealed volume a valve will fill; bigger scans report a leak.")
                .defineInRange("maxVolume", 32_768, 1, 1_000_000);
        builder.pop();

        SPEC = builder.build();
    }

    private MCWaterslidesConfig() {}
}
