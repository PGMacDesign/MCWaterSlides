package com.pgmacdesign.mcwaterslides.config;

import net.neoforged.neoforge.common.ModConfigSpec;

/**
 * Common config. Keys land here with the feature that reads them; the full surface is
 * specified in docs/blocks-recipes-spec.html §5.
 */
public final class MCWaterslidesConfig {
    public static final ModConfigSpec SPEC;

    public static final ModConfigSpec.DoubleValue SPEED_CAP;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();

        builder.comment("Ride physics").push("ride");
        SPEED_CAP = builder
                .comment("Momentum ceiling for slide riders, in blocks per second.",
                        "Default 22 sits deliberately under the server movement-check ceiling",
                        "(sprint ~5.6, elytra cruise ~30+).")
                .defineInRange("speedCap", 22.0, 1.0, 30.0);
        builder.pop();

        SPEC = builder.build();
    }

    private MCWaterslidesConfig() {}
}
