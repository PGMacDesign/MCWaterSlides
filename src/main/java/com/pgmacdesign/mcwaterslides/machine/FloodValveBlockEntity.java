package com.pgmacdesign.mcwaterslides.machine;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;

import javax.annotation.Nullable;

import com.pgmacdesign.mcwaterslides.config.MCWaterslidesConfig;
import com.pgmacdesign.mcwaterslides.registry.ModBlockEntities;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import it.unimi.dsi.fastutil.longs.LongSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * The fill/drain state machine behind {@link FloodValveBlock}. Mode is derived from the
 * POWERED blockstate each tick; the volume scan is a bounded BFS through air/water cells
 * — cap hit = leak, reported with the escape position instead of a partial fill.
 */
public class FloodValveBlockEntity extends BlockEntity {
    private static final int BLOCKS_PER_TICK = 8;
    private static final int LEAK_REPORT_COOLDOWN = 100;

    private final ValveEnergy energy = new ValveEnergy(20_000);

    @Nullable
    private List<BlockPos> scannedRegion;
    private boolean scanDirty = true;
    @Nullable
    private BlockPos lastLeak;
    private long lastLeakReport = Long.MIN_VALUE;

    private static final class ValveEnergy extends EnergyStorage {
        ValveEnergy(int capacity) {
            super(capacity, 1024, 0);
        }

        boolean consume(int amount) {
            if (energy < amount) {
                return false;
            }
            energy -= amount;
            return true;
        }

        void setStored(int amount) {
            energy = Math.max(0, Math.min(amount, capacity));
        }
    }

    public FloodValveBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FLOOD_VALVE.get(), pos, state);
    }

    public IEnergyStorage energyHandler() {
        return energy;
    }

    /** Test/support hook (mirrors the jet's). */
    public void fillBuffer() {
        energy.setStored(Integer.MAX_VALUE);
        setChanged();
    }

    @Nullable
    public BlockPos lastLeak() {
        return lastLeak;
    }

    /** Live readout for the right-click status message. Runs a fresh bounded scan. */
    public record Status(boolean powered, int energy, int maxEnergy, int volume, @Nullable BlockPos leak) {}

    public Status status(BlockState state) {
        boolean powered = state.getValue(FloodValveBlock.POWERED);
        int e = energy.getEnergyStored();
        int max = energy.getMaxEnergyStored();
        Level level = getLevel();
        if (level == null) {
            return new Status(powered, e, max, 0, lastLeak);
        }
        ScanResult r = scan(level, getBlockPos(), state.getValue(FloodValveBlock.FACING));
        return new Status(powered, e, max, r.cells.size(), r.leak);
    }

    public void invalidateScan() {
        scanDirty = true;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, FloodValveBlockEntity valve) {
        boolean filling = state.getValue(FloodValveBlock.POWERED);
        if (filling) {
            valve.tickFill(level, pos, state);
        } else {
            valve.tickDrain(level, pos, state);
        }
    }

    private void tickFill(Level level, BlockPos pos, BlockState state) {
        List<BlockPos> region = region(level, pos, state);
        if (region == null) {
            reportLeak(level, pos);
            return;
        }
        int placed = 0;
        for (BlockPos cell : region) {
            if (placed >= BLOCKS_PER_TICK) {
                break;
            }
            BlockState cellState = level.getBlockState(cell);
            if (cellState.getFluidState().is(FluidTags.WATER) && cellState.getFluidState().isSource()) {
                continue; // idempotent: already water, no charge
            }
            if (!cellState.isAir() && !cellState.getFluidState().is(FluidTags.WATER)) {
                // World changed under the cached scan — rescan next tick, never write stale cells.
                scanDirty = true;
                return;
            }
            // RF billed per placed block, before placement.
            if (!energy.consume(MCWaterslidesConfig.FLOOD_VALVE_RF_PER_BLOCK.get())) {
                return; // pause until powered; resumes exactly here
            }
            level.setBlock(cell, Blocks.WATER.defaultBlockState(), 3);
            placed++;
        }
    }

    private void tickDrain(Level level, BlockPos pos, BlockState state) {
        List<BlockPos> region = drainRegion(level, pos, state);
        if (region.isEmpty()) {
            return;
        }
        int removed = 0;
        // Top-down so the column empties like a real tank.
        for (int i = region.size() - 1; i >= 0 && removed < BLOCKS_PER_TICK; i--) {
            BlockPos cell = region.get(i);
            BlockState cellState = level.getBlockState(cell);
            if (cellState.getFluidState().is(FluidTags.WATER) && cellState.getBlock() == Blocks.WATER) {
                level.setBlock(cell, Blocks.AIR.defaultBlockState(), 3);
                removed++;
            }
        }
    }

    /** Sealed region (sorted bottom-up), or null when the volume leaks/overflows the cap. */
    @Nullable
    private List<BlockPos> region(Level level, BlockPos pos, BlockState state) {
        if (!scanDirty && scannedRegion != null) {
            return scannedRegion;
        }
        ScanResult result = scan(level, pos, state.getValue(FloodValveBlock.FACING));
        scanDirty = false;
        if (result.leak != null) {
            scannedRegion = null;
            lastLeak = result.leak;
            return null;
        }
        lastLeak = null;
        scannedRegion = result.cells;
        return scannedRegion;
    }

    /** Drain never requires a seal: bounded best-effort cleanup from a fresh scan. */
    private List<BlockPos> drainRegion(Level level, BlockPos pos, BlockState state) {
        ScanResult result = scan(level, pos, state.getValue(FloodValveBlock.FACING));
        return result.cells;
    }

    private record ScanResult(List<BlockPos> cells, @Nullable BlockPos leak) {}

    /**
     * Bounded BFS through air/water from the valve face. Cells are returned sorted
     * bottom-up (fill cascades from the floor). Cap hit = leak at the escape frontier.
     */
    private ScanResult scan(Level level, BlockPos valvePos, Direction facing) {
        int cap = MCWaterslidesConfig.FLOOD_VALVE_MAX_VOLUME.get();
        List<BlockPos> cells = new ArrayList<>();
        LongSet visited = new LongOpenHashSet();
        Deque<BlockPos> queue = new ArrayDeque<>();

        BlockPos start = valvePos.relative(facing);
        queue.add(start);
        visited.add(start.asLong());

        while (!queue.isEmpty()) {
            BlockPos cell = queue.poll();
            BlockState cellState = level.getBlockState(cell);
            boolean passable = cellState.isAir() || cellState.getFluidState().is(FluidTags.WATER);
            if (!passable) {
                continue; // solid boundary
            }
            if (cells.size() >= cap) {
                return new ScanResult(List.of(), cell); // escaped: this is where water got out
            }
            cells.add(cell.immutable());
            for (Direction d : Direction.values()) {
                BlockPos next = cell.relative(d);
                if (visited.add(next.asLong())) {
                    queue.add(next);
                }
            }
        }

        cells.sort(Comparator.<BlockPos>comparingInt(BlockPos::getY)
                .thenComparingInt(c -> c.distManhattan(start)));
        return new ScanResult(cells, null);
    }

    private void reportLeak(Level level, BlockPos pos) {
        if (lastLeak == null || !(level instanceof ServerLevel serverLevel)) {
            return;
        }
        if (level.getGameTime() - lastLeakReport < LEAK_REPORT_COOLDOWN) {
            return;
        }
        lastLeakReport = level.getGameTime();
        serverLevel.sendParticles(ParticleTypes.CAMPFIRE_COSY_SMOKE,
                lastLeak.getX() + 0.5, lastLeak.getY() + 0.5, lastLeak.getZ() + 0.5,
                12, 0.2, 0.4, 0.2, 0.01);
        for (ServerPlayer player : serverLevel.players()) {
            if (player.distanceToSqr(pos.getX(), pos.getY(), pos.getZ()) <= 24 * 24) {
                player.displayClientMessage(Component.translatable("message.mcwaterslides.flood_valve_leak",
                        lastLeak.getX(), lastLeak.getY(), lastLeak.getZ()), true);
            }
        }
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", energy.getEnergyStored());
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        energy.setStored(tag.getInt("Energy"));
    }
}
