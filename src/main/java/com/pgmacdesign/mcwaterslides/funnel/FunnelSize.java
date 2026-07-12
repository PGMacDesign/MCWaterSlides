package com.pgmacdesign.mcwaterslides.funnel;

import net.minecraft.util.StringRepresentable;

/**
 * The three tornado scales. Each carries the side-lying cone's geometry: mouth radius (the wide
 * entry end), exit radius (the narrow throat riders fire out of), axial length, and how far the
 * bottom line falls from mouth to exit — the ride loses only a little height, the swish does
 * the rest (LARGE: ~10-block mouth in, ~5-block throat out).
 */
public enum FunnelSize implements StringRepresentable {
    SMALL("small", 2.5, 1.5, 7, 1.0),
    MEDIUM("medium", 3.5, 2.0, 10, 1.5),
    LARGE("large", 5.0, 2.5, 13, 2.0);

    private final String name;
    private final double mouthRadius;
    private final double exitRadius;
    private final int length;
    private final double drop;

    FunnelSize(String name, double mouthRadius, double exitRadius, int length, double drop) {
        this.name = name;
        this.mouthRadius = mouthRadius;
        this.exitRadius = exitRadius;
        this.length = length;
        this.drop = drop;
    }

    public double mouthRadius() {
        return mouthRadius;
    }

    public double exitRadius() {
        return exitRadius;
    }

    public int length() {
        return length;
    }

    public double drop() {
        return drop;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
