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
    private static final Map<RailShape, VoxelShape> WALLS = new EnumMap<>(RailShape.class);
    /** Straight shapes × (wallNeg, wallPos) — wide slides drop shared walls. */
    private static final Map<RailShape, VoxelShape[]> STRAIGHT_VARIANTS = new EnumMap<>(RailShape.class);

    private SlideChannelShapes() {}

    static VoxelShape shape(RailShape shape) {
        return SHAPES.get(shape);
    }

    /**
     * Shape honoring dropped side walls (parallel-lane merges). Neg = west wall on a
     * north-south run, north wall on an east-west run; pos = the other side. Non-straight
     * shapes ignore the flags.
     */
    static VoxelShape shape(RailShape shape, boolean wallNeg, boolean wallPos) {
        VoxelShape[] variants = STRAIGHT_VARIANTS.get(shape);
        if (variants == null) {
            return SHAPES.get(shape);
        }
        return variants[(wallNeg ? 1 : 0) | (wallPos ? 2 : 0)];
    }

    /** Just the side walls of a flat shape (no floor) — tube bores drop floors/lids. */
    static VoxelShape walls(RailShape shape) {
        return WALLS.get(shape);
    }

    static final VoxelShape FLOOR = Block.box(0, 0, 0, 16, 2, 16);

    private static final double WALL_H = 14;

    static {
        // Straight north-south: walls along west (neg X) / east (pos X) edges.
        VoxelShape nsNeg = Block.box(0, 2, 0, 2, WALL_H, 16);
        VoxelShape nsPos = Block.box(14, 2, 0, 16, WALL_H, 16);
        WALLS.put(RailShape.NORTH_SOUTH, Shapes.or(nsNeg, nsPos));
        STRAIGHT_VARIANTS.put(RailShape.NORTH_SOUTH, straightVariants(nsNeg, nsPos));
        // Straight east-west: walls along north (neg Z) / south (pos Z) edges.
        VoxelShape ewNeg = Block.box(0, 2, 0, 16, WALL_H, 2);
        VoxelShape ewPos = Block.box(0, 2, 14, 16, WALL_H, 16);
        WALLS.put(RailShape.EAST_WEST, Shapes.or(ewNeg, ewPos));
        STRAIGHT_VARIANTS.put(RailShape.EAST_WEST, straightVariants(ewNeg, ewPos));

        // Corners: outer walls on the two closed sides. SOUTH_EAST exits south+east →
        // walls on north + west.
        WALLS.put(RailShape.SOUTH_EAST, cornerWalls(true, true, false, false));
        WALLS.put(RailShape.SOUTH_WEST, cornerWalls(true, false, false, true));
        WALLS.put(RailShape.NORTH_WEST, cornerWalls(false, false, true, true));
        WALLS.put(RailShape.NORTH_EAST, cornerWalls(false, true, true, false));

        WALLS.forEach((rail, walls) -> SHAPES.put(rail, Shapes.or(FLOOR, walls)));

        // Ascending: four steps rising along the axis toward the named direction.
        SHAPES.put(RailShape.ASCENDING_SOUTH, ascendingZ(false));
        SHAPES.put(RailShape.ASCENDING_NORTH, ascendingZ(true));
        SHAPES.put(RailShape.ASCENDING_EAST, ascendingX(false));
        SHAPES.put(RailShape.ASCENDING_WEST, ascendingX(true));
    }

    /** Floor + optional neg/pos walls, indexed (wallNeg ? 1 : 0) | (wallPos ? 2 : 0). */
    private static VoxelShape[] straightVariants(VoxelShape neg, VoxelShape pos) {
        return new VoxelShape[]{
                FLOOR,
                Shapes.or(FLOOR, neg),
                Shapes.or(FLOOR, pos),
                Shapes.or(FLOOR, neg, pos),
        };
    }

    /** Walls on the flagged sides (north wall, west wall, south wall, east wall). */
    private static VoxelShape cornerWalls(boolean north, boolean west, boolean south, boolean east) {
        VoxelShape shape = Shapes.empty();
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
