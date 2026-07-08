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
        return state.getBlock() instanceof SlideSurface;
    }

    public static RailShape computeShape(LevelReader level, BlockPos pos, Direction.Axis fallbackAxis) {
        // Filled 2D patch (a wide slide): orient every cell uniformly along the run's
        // longer axis instead of letting corners/edges pick perpendicular shapes — that
        // mismatch is what left arbitrary internal walls in wider-than-2 builds.
        if (isFlatAreaCell(level, pos)) {
            return runAxisByExtent(level, pos) == Direction.Axis.X
                    ? RailShape.EAST_WEST : RailShape.NORTH_SOUTH;
        }

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

    /**
     * True when {@code pos} sits in a filled 2×2 (or larger) patch of same-level slides —
     * i.e. some perpendicular neighbor pair has its shared diagonal also filled. A lone
     * L-bend (diagonal empty) is a real corner and stays one.
     */
    private static boolean isFlatAreaCell(LevelReader level, BlockPos pos) {
        for (Direction a : new Direction[]{Direction.NORTH, Direction.SOUTH}) {
            for (Direction b : new Direction[]{Direction.EAST, Direction.WEST}) {
                if (isSlide(level.getBlockState(pos.relative(a)))
                        && isSlide(level.getBlockState(pos.relative(b)))
                        && isSlide(level.getBlockState(pos.relative(a).relative(b)))) {
                    return true;
                }
            }
        }
        return false;
    }

    /** The axis a wide patch runs along = whichever direction the contiguous slides reach
     *  farther (tiebreak Z), so a 2×N rectangle orients along its length, not its width. */
    private static Direction.Axis runAxisByExtent(LevelReader level, BlockPos pos) {
        int z = contiguous(level, pos, Direction.NORTH) + contiguous(level, pos, Direction.SOUTH);
        int x = contiguous(level, pos, Direction.EAST) + contiguous(level, pos, Direction.WEST);
        return x > z ? Direction.Axis.X : Direction.Axis.Z;
    }

    /** Count contiguous same-level slides walking from {@code pos} along {@code dir} (bounded). */
    private static int contiguous(LevelReader level, BlockPos pos, Direction dir) {
        int n = 0;
        BlockPos p = pos;
        for (int i = 0; i < 16; i++) {
            p = p.relative(dir);
            if (!isSlide(level.getBlockState(p))) {
                break;
            }
            n++;
        }
        return n;
    }

    /** Recompute this block's own shape + wall merges in place (no-op when unchanged). */
    public static void refreshSelf(Level level, BlockPos pos, BlockState state) {
        if (state.getBlock() instanceof SlideTubeBlock tube) {
            // Tubes own their shape logic (adds VERTICAL).
            tube.refresh(level, pos, state);
            return;
        }
        if (!(state.getBlock() instanceof SlideChannelBlock)) {
            return;
        }
        RailShape current = state.getValue(SlideChannelBlock.SHAPE);
        RailShape computed = computeShape(level, pos, axisOf(current));
        BlockState updated = withMergedWalls(level, pos,
                state.setValue(SlideChannelBlock.SHAPE, computed));
        if (updated != state) {
            level.setBlock(pos, updated, Block.UPDATE_ALL);
        }
    }

    /**
     * Drop a straight channel's perpendicular wall on any side where a parallel-shape
     * channel abuts (wide slides read as one broad surface). Existence-of-a-parallel-
     * neighbor only — the neighbor's SHAPE is itself existence-derived and already
     * settled, so this stays oscillation-free: shapes never depend on wall flags.
     * Corners/ascending keep both walls (their flags are inert, ignored by collision).
     */
    public static BlockState withMergedWalls(LevelReader level, BlockPos pos, BlockState state) {
        RailShape shape = state.getValue(SlideChannelBlock.SHAPE);
        Direction.Axis perp = perpAxisOf(shape);
        boolean neg = true;
        boolean posWall = true;
        if (perp != null) {
            Direction negDir = Direction.fromAxisAndDirection(perp, Direction.AxisDirection.NEGATIVE);
            Direction posDir = Direction.fromAxisAndDirection(perp, Direction.AxisDirection.POSITIVE);
            neg = !parallelChannelAt(level, pos.relative(negDir), shape);
            posWall = !parallelChannelAt(level, pos.relative(posDir), shape);
        }
        return state.setValue(SlideChannelBlock.WALL_NEG, neg).setValue(SlideChannelBlock.WALL_POS, posWall);
    }

    /** A channel of the same straight shape (parallel lane) sits at {@code pos}. */
    private static boolean parallelChannelAt(LevelReader level, BlockPos pos, RailShape shape) {
        BlockState s = level.getBlockState(pos);
        return s.getBlock() instanceof SlideChannelBlock && s.getValue(SlideChannelBlock.SHAPE) == shape;
    }

    /** The perpendicular (wall-bearing) axis of a straight shape, or null otherwise. */
    private static Direction.Axis perpAxisOf(RailShape shape) {
        return switch (shape) {
            case NORTH_SOUTH -> Direction.Axis.X;
            case EAST_WEST -> Direction.Axis.Z;
            default -> null;
        };
    }

    /**
     * Recompute every slide block that could connect to {@code pos} (place/remove ripple).
     * Includes the four same-level DIAGONALS: the wide-area rule ({@link #isFlatAreaCell})
     * reads a cell's diagonal, so completing a 2×2 must re-shape the diagonally-opposite
     * cell too — otherwise it keeps a stale shape and its wall never merges.
     */
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
        for (Direction a : new Direction[]{Direction.NORTH, Direction.SOUTH}) {
            for (Direction b : new Direction[]{Direction.EAST, Direction.WEST}) {
                BlockPos diag = pos.relative(a).relative(b);
                BlockState ns = level.getBlockState(diag);
                if (isSlide(ns)) {
                    refreshSelf(level, diag, ns);
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
