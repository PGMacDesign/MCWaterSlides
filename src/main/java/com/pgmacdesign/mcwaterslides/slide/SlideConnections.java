package com.pgmacdesign.mcwaterslides.slide;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;

/**
 * Rail-style auto-connect for slide channels/tubes, kept deliberately oscillation-free:
 * a block's shape is a function of neighbor EXISTENCE only (never neighbor shape), so a
 * place/remove converges in a single recompute pass over the affected neighborhood.
 *
 * A direction "connects" when any slide block sits at the adjacent position on the same
 * level, one above (we ascend toward it), or one below (it ascends toward us). With more
 * than two candidates the first two in N/E/S/W order win, preferring a straight pair.
 */
public final class SlideConnections {
    private static final Direction[] ORDER = {Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST};

    private SlideConnections() {}

    public static boolean isSlide(BlockState state) {
        return state.getBlock() instanceof SlideChannelBlock;
    }

    public static RailShape computeShape(LevelReader level, BlockPos pos, Direction.Axis fallbackAxis) {
        List<Direction> dirs = new ArrayList<>(2);
        boolean[] raised = new boolean[4];

        for (int i = 0; i < ORDER.length; i++) {
            Direction d = ORDER[i];
            BlockPos side = pos.relative(d);
            boolean level0 = isSlide(level.getBlockState(side));
            boolean up = isSlide(level.getBlockState(side.above()));
            boolean down = isSlide(level.getBlockState(side.below()));
            if (level0 || up || down) {
                dirs.add(d);
                raised[i] = up && !level0;
            }
        }

        // Prefer a straight (opposite) pair when three or four sides connect.
        if (dirs.size() > 2) {
            List<Direction> picked = new ArrayList<>(2);
            for (Direction d : dirs) {
                if (dirs.contains(d.getOpposite())) {
                    picked.add(d);
                    picked.add(d.getOpposite());
                    break;
                }
            }
            if (picked.isEmpty()) {
                picked.add(dirs.get(0));
                picked.add(dirs.get(1));
            }
            dirs = picked;
        }

        if (dirs.isEmpty()) {
            return fallbackAxis == Direction.Axis.X ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
        }
        if (dirs.size() == 1) {
            Direction d = dirs.get(0);
            if (raised[indexOf(d)]) {
                return ascending(d);
            }
            return d.getAxis() == Direction.Axis.X ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
        }

        Direction a = dirs.get(0);
        Direction b = dirs.get(1);
        if (a.getAxis() == b.getAxis()) {
            // Straight; ascend toward whichever end is raised (first in order wins).
            if (raised[indexOf(a)]) return ascending(a);
            if (raised[indexOf(b)]) return ascending(b);
            return a.getAxis() == Direction.Axis.X ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
        }
        return corner(a, b);
    }

    /** Recompute this block's own shape in place (no-op when unchanged). */
    public static void refreshSelf(Level level, BlockPos pos, BlockState state) {
        if (!isSlide(state)) {
            return;
        }
        RailShape current = state.getValue(SlideChannelBlock.SHAPE);
        RailShape computed = computeShape(level, pos, axisOf(current));
        if (computed != current) {
            level.setBlock(pos, state.setValue(SlideChannelBlock.SHAPE, computed), Block.UPDATE_ALL);
        }
    }

    /** Recompute every slide block that could connect to {@code pos} (place/remove ripple). */
    public static void refreshNeighbors(Level level, BlockPos pos) {
        for (Direction d : ORDER) {
            BlockPos side = pos.relative(d);
            for (BlockPos np : new BlockPos[]{side, side.above(), side.below()}) {
                BlockState ns = level.getBlockState(np);
                if (isSlide(ns)) {
                    refreshSelf(level, np, ns);
                }
            }
        }
    }

    private static int indexOf(Direction d) {
        for (int i = 0; i < ORDER.length; i++) {
            if (ORDER[i] == d) return i;
        }
        throw new IllegalArgumentException("not horizontal: " + d);
    }

    private static Direction.Axis axisOf(RailShape shape) {
        return switch (shape) {
            case EAST_WEST, ASCENDING_EAST, ASCENDING_WEST -> Direction.Axis.X;
            default -> Direction.Axis.Z;
        };
    }

    private static RailShape ascending(Direction d) {
        return switch (d) {
            case NORTH -> RailShape.ASCENDING_NORTH;
            case SOUTH -> RailShape.ASCENDING_SOUTH;
            case EAST -> RailShape.ASCENDING_EAST;
            case WEST -> RailShape.ASCENDING_WEST;
            default -> throw new IllegalArgumentException("not horizontal: " + d);
        };
    }

    private static RailShape corner(Direction a, Direction b) {
        boolean north = a == Direction.NORTH || b == Direction.NORTH;
        boolean east = a == Direction.EAST || b == Direction.EAST;
        boolean south = a == Direction.SOUTH || b == Direction.SOUTH;
        if (south && east) return RailShape.SOUTH_EAST;
        if (south) return RailShape.SOUTH_WEST;
        if (north && east) return RailShape.NORTH_EAST;
        return RailShape.NORTH_WEST;
    }
}
