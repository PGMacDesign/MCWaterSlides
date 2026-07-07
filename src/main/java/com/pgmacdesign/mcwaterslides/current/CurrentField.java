package com.pgmacdesign.mcwaterslides.current;

import java.util.ArrayDeque;
import java.util.Deque;

import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

import com.pgmacdesign.mcwaterslides.slide.SlideSurface;

/**
 * An immutable directed current: the set of water/slide cells a jet energizes. Computed
 * once per (re)validation by a bounded BFS — a 3×3 beam along the jet's facing, walked
 * only through passable cells (real water or slide blocks) — then published by reference
 * swap. Samplers only do containment lookups; they never search the world.
 */
public final class CurrentField {
    public static final CurrentField EMPTY =
            new CurrentField(Direction.NORTH, new LongOpenHashSet(), new AABB(BlockPos.ZERO));

    private final Direction flow;
    private final LongSet cells;
    private final AABB bounds;

    private CurrentField(Direction flow, LongSet cells, AABB bounds) {
        this.flow = flow;
        this.cells = cells;
        this.bounds = bounds;
    }

    public Direction flow() {
        return flow;
    }

    public boolean isEmpty() {
        return cells.isEmpty();
    }

    public boolean contains(BlockPos pos) {
        return cells.contains(pos.asLong());
    }

    /** Entity-query bounds, slightly inflated so feet at cell edges still count. */
    public AABB bounds() {
        return bounds;
    }

    public int size() {
        return cells.size();
    }

    /**
     * Walk the current from {@code jetPos} along {@code facing}: BFS through passable
     * cells constrained to axial distance [0, range] and radial (perpendicular Chebyshev)
     * distance ≤ 1 — a 3×3 beam. The visit cap bounds work even facing an ocean.
     */
    public static CurrentField compute(Level level, BlockPos jetPos, Direction facing, int range) {
        LongSet cells = new LongOpenHashSet();
        LongSet visited = new LongOpenHashSet();
        Deque<BlockPos> queue = new ArrayDeque<>();
        int cap = (range + 1) * 9;

        BlockPos start = jetPos.relative(facing);
        queue.add(start);
        visited.add(start.asLong());

        double minX = start.getX(), minY = start.getY(), minZ = start.getZ();
        double maxX = minX, maxY = minY, maxZ = minZ;

        while (!queue.isEmpty() && cells.size() < cap) {
            BlockPos pos = queue.poll();
            if (!inBeam(jetPos, facing, pos, range) || !isPassable(level.getBlockState(pos))) {
                continue;
            }
            cells.add(pos.asLong());
            minX = Math.min(minX, pos.getX()); maxX = Math.max(maxX, pos.getX());
            minY = Math.min(minY, pos.getY()); maxY = Math.max(maxY, pos.getY());
            minZ = Math.min(minZ, pos.getZ()); maxZ = Math.max(maxZ, pos.getZ());
            for (Direction d : Direction.values()) {
                BlockPos next = pos.relative(d);
                if (visited.add(next.asLong())) {
                    queue.add(next);
                }
            }
        }

        if (cells.isEmpty()) {
            return EMPTY;
        }
        AABB bounds = new AABB(minX - 0.5, minY - 0.5, minZ - 0.5, maxX + 1.5, maxY + 1.5, maxZ + 1.5);
        return new CurrentField(facing, cells, bounds);
    }

    private static boolean inBeam(BlockPos jet, Direction facing, BlockPos pos, int range) {
        int dx = pos.getX() - jet.getX();
        int dy = pos.getY() - jet.getY();
        int dz = pos.getZ() - jet.getZ();
        int axial = dx * facing.getStepX() + dy * facing.getStepY() + dz * facing.getStepZ();
        if (axial < 1 || axial > range) {
            return false;
        }
        int rx = dx - axial * facing.getStepX();
        int ry = dy - axial * facing.getStepY();
        int rz = dz - axial * facing.getStepZ();
        return Math.max(Math.abs(rx), Math.max(Math.abs(ry), Math.abs(rz))) <= 1;
    }

    private static boolean isPassable(BlockState state) {
        return state.getFluidState().is(FluidTags.WATER)
                || state.getBlock() instanceof SlideSurface;
    }
}
