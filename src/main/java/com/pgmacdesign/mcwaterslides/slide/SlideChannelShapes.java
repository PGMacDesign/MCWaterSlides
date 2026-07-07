package com.pgmacdesign.mcwaterslides.slide;

import java.util.EnumMap;
import java.util.Map;

import net.minecraft.world.level.block.state.properties.RailShape;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.level.block.Block;

/**
 * Collision/outline shapes per {@link RailShape}. Floor 2px + 2px side walls up to 14px
 * (the rendered waterline sits at 12px, brimming just under the wall tops). Ascending
 * runs are four 4px steps — each rise is under the 9.6px auto-step, so entities walk up
 * smoothly without jump spam.
 */
final class SlideChannelShapes {
    private static final Map<RailShape, VoxelShape> SHAPES = new EnumMap<>(RailShape.class);

    private SlideChannelShapes() {}

    static VoxelShape shape(RailShape shape) {
        return SHAPES.get(shape);
    }

    private static final double WALL_H = 14;

    static {
        VoxelShape floor = Block.box(0, 0, 0, 16, 2, 16);

        // Straight north-south: walls along east/west edges.
        VoxelShape ns = Shapes.or(floor,
                Block.box(0, 2, 0, 2, WALL_H, 16),
                Block.box(14, 2, 0, 16, WALL_H, 16));
        // Straight east-west: walls along north/south edges.
        VoxelShape ew = Shapes.or(floor,
                Block.box(0, 2, 0, 16, WALL_H, 2),
                Block.box(0, 2, 14, 16, WALL_H, 16));
        SHAPES.put(RailShape.NORTH_SOUTH, ns);
        SHAPES.put(RailShape.EAST_WEST, ew);

        // Corners: outer walls on the two closed sides. SOUTH_EAST exits south+east →
        // walls on north + west.
        SHAPES.put(RailShape.SOUTH_EAST, corner(true, true, false, false));
        SHAPES.put(RailShape.SOUTH_WEST, corner(true, false, false, true));
        SHAPES.put(RailShape.NORTH_WEST, corner(false, false, true, true));
        SHAPES.put(RailShape.NORTH_EAST, corner(false, true, true, false));

        // Ascending: four steps rising along the axis toward the named direction.
        SHAPES.put(RailShape.ASCENDING_SOUTH, ascendingZ(false));
        SHAPES.put(RailShape.ASCENDING_NORTH, ascendingZ(true));
        SHAPES.put(RailShape.ASCENDING_EAST, ascendingX(false));
        SHAPES.put(RailShape.ASCENDING_WEST, ascendingX(true));
    }

    /** Walls on the flagged sides (north wall, west wall, south wall, east wall). */
    private static VoxelShape corner(boolean north, boolean west, boolean south, boolean east) {
        VoxelShape shape = Block.box(0, 0, 0, 16, 2, 16);
        if (north) shape = Shapes.or(shape, Block.box(0, 2, 0, 16, WALL_H, 2));
        if (south) shape = Shapes.or(shape, Block.box(0, 2, 14, 16, WALL_H, 16));
        if (west) shape = Shapes.or(shape, Block.box(0, 2, 0, 2, WALL_H, 16));
        if (east) shape = Shapes.or(shape, Block.box(14, 2, 0, 16, WALL_H, 16));
        return shape;
    }

    /** Steps rising toward +Z (south); {@code reverse} flips to rise toward -Z (north). Walls on east/west. */
    private static VoxelShape ascendingZ(boolean reverse) {
        VoxelShape shape = Shapes.or(
                Block.box(0, 2, 0, 2, 16, 16),
                Block.box(14, 2, 0, 16, 16, 16));
        for (int i = 0; i < 4; i++) {
            double z0 = i * 4, z1 = z0 + 4;
            double top = 2 + i * 4;
            if (reverse) {
                double t = 16 - z1; z1 = 16 - z0; z0 = t;
            }
            shape = Shapes.or(shape, Block.box(0, 0, z0, 16, top, z1));
        }
        return shape;
    }

    /** Steps rising toward +X (east); {@code reverse} flips to rise toward -X (west). Walls on north/south. */
    private static VoxelShape ascendingX(boolean reverse) {
        VoxelShape shape = Shapes.or(
                Block.box(0, 2, 0, 16, 16, 2),
                Block.box(0, 2, 14, 16, 16, 16));
        for (int i = 0; i < 4; i++) {
            double x0 = i * 4, x1 = x0 + 4;
            double top = 2 + i * 4;
            if (reverse) {
                double t = 16 - x1; x1 = 16 - x0; x0 = t;
            }
            shape = Shapes.or(shape, Block.box(x0, 0, 0, x1, top, 16));
        }
        return shape;
    }
}
