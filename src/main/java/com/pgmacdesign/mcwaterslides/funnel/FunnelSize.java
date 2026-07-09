package com.pgmacdesign.mcwaterslides.funnel;

import net.minecraft.util.StringRepresentable;

/**
 * The three funnel scales (S/M/L). Each carries its bowl geometry: the rim radius (half the
 * diameter promised to the builder), how tall the bowl stands above the drain, and the drain
 * radius under which a rider drops out the bottom.
 */
public enum FunnelSize implements StringRepresentable {
    // rimRadius = rideable swirl radius; the stamped wall sits ~0.9 outside it, so the outer
    // bowl diameters land on the promised ~5 / 7 / 9 blocks.
    SMALL("small", 1.6, 3, 0.8),
    MEDIUM("medium", 2.6, 4, 1.0),
    LARGE("large", 3.6, 5, 1.2);

    private final String name;
    private final double rimRadius;
    private final int bowlHeight;
    private final double drainRadius;

    FunnelSize(String name, double rimRadius, int bowlHeight, double drainRadius) {
        this.name = name;
        this.rimRadius = rimRadius;
        this.bowlHeight = bowlHeight;
        this.drainRadius = drainRadius;
    }

    public double rimRadius() {
        return rimRadius;
    }

    public int bowlHeight() {
        return bowlHeight;
    }

    public double drainRadius() {
        return drainRadius;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
