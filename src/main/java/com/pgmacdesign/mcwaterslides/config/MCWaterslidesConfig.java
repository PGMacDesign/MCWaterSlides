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

        SPEC = builder.build();
    }

    private MCWaterslidesConfig() {}
}
