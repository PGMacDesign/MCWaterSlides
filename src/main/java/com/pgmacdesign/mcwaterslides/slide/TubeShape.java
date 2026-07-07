package com.pgmacdesign.mcwaterslides.slide;

import javax.annotation.Nullable;

import net.minecraft.util.StringRepresentable;
import net.minecraft.world.level.block.state.properties.RailShape;

/**
 * Tube shapes: the ten {@link RailShape} values plus {@code VERTICAL} (drop shafts and
 * jet-powered climbs — the thing rails never needed but waterslides are made of).
 */
public enum TubeShape implements StringRepresentable {
    NORTH_SOUTH("north_south", RailShape.NORTH_SOUTH),
    EAST_WEST("east_west", RailShape.EAST_WEST),
    ASCENDING_EAST("ascending_east", RailShape.ASCENDING_EAST),
    ASCENDING_WEST("ascending_west", RailShape.ASCENDING_WEST),
    ASCENDING_NORTH("ascending_north", RailShape.ASCENDING_NORTH),
    ASCENDING_SOUTH("ascending_south", RailShape.ASCENDING_SOUTH),
    SOUTH_EAST("south_east", RailShape.SOUTH_EAST),
    SOUTH_WEST("south_west", RailShape.SOUTH_WEST),
    NORTH_WEST("north_west", RailShape.NORTH_WEST),
    NORTH_EAST("north_east", RailShape.NORTH_EAST),
    VERTICAL("vertical", null);

    private final String name;
    @Nullable
    private final RailShape rail;

    TubeShape(String name, @Nullable RailShape rail) {
        this.name = name;
        this.rail = rail;
    }

    /** The equivalent rail shape, or null for {@link #VERTICAL}. */
    @Nullable
    public RailShape toRail() {
        return rail;
    }

    public static TubeShape fromRail(RailShape shape) {
        for (TubeShape tube : values()) {
            if (tube.rail == shape) {
                return tube;
            }
        }
        return NORTH_SOUTH;
    }

    @Override
    public String getSerializedName() {
        return name;
    }
}
