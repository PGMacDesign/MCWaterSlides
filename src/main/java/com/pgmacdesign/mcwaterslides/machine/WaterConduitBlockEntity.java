package com.pgmacdesign.mcwaterslides.machine;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import com.pgmacdesign.mcwaterslides.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Conduit transport (MC3DPrint cable pattern, single currency): each tick a conduit
 * pulls from adjacent extractable sources into a one-tick buffer, then pushes across
 * its network to reachable acceptors. Network MEMBERSHIP (positions only) is refloooded
 * at most every {@link #RECOMPUTE_INTERVAL} ticks — energy levels are always read live,
 * so bounded staleness applies only to topology changes (~5s), never to charge state.
 */
public class WaterConduitBlockEntity extends BlockEntity {
    public static final int TRANSFER_RATE = 256;
    private static final int MAX_NETWORK = 4096;   // runaway-flood backstop
    private static final int RECOMPUTE_INTERVAL = 100;

    private int buffered;
    private Map<BlockPos, Direction> acceptorFaces;
    private long lastRecompute = Long.MIN_VALUE;

    private final IEnergyStorage energy = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            int accepted = Math.min(maxReceive, TRANSFER_RATE - buffered);
            if (accepted > 0 && !simulate) {
                buffered += accepted;
            }
            return Math.max(0, accepted);
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            return 0; // conduits push; nothing siphons a wire
        }

        @Override
        public int getEnergyStored() {
            return buffered;
        }

        @Override
        public int getMaxEnergyStored() {
            return TRANSFER_RATE;
        }

        @Override
        public boolean canExtract() {
            return false;
        }

        @Override
        public boolean canReceive() {
            return true;
        }
    };

    public WaterConduitBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.WATER_CONDUIT.get(), pos, state);
    }

    public IEnergyStorage energyHandler() {
        return energy;
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, WaterConduitBlockEntity conduit) {
        conduit.transfer(level, pos);
    }

    private void transfer(Level level, BlockPos pos) {
        // 1) Pull from adjacent, non-conduit, extractable sources (always live).
        for (Direction dir : Direction.values()) {
            if (buffered >= TRANSFER_RATE) {
                break;
            }
            BlockPos neighborPos = pos.relative(dir);
            if (level.getBlockEntity(neighborPos) instanceof WaterConduitBlockEntity) {
                continue;
            }
            IEnergyStorage src = level.getCapability(Capabilities.EnergyStorage.BLOCK,
                    neighborPos, dir.getOpposite());
            if (src == null || !src.canExtract()) {
                continue;
            }
            int pulled = src.extractEnergy(TRANSFER_RATE - buffered, false);
            buffered += pulled;
        }

        if (buffered <= 0) {
            return;
        }

        // 2) Push to network acceptors (membership throttled; energy moves every tick).
        ensureFresh(level, pos);
        for (Map.Entry<BlockPos, Direction> entry : acceptorFaces.entrySet()) {
            if (buffered <= 0) {
                break;
            }
            IEnergyStorage acceptor = level.getCapability(Capabilities.EnergyStorage.BLOCK,
                    entry.getKey(), entry.getValue());
            if (acceptor != null && acceptor.canReceive()) {
                buffered -= acceptor.receiveEnergy(buffered, false);
            }
        }
    }

    /** BFS the conduit graph, collecting non-conduit energy acceptors on its faces. */
    private void ensureFresh(Level level, BlockPos origin) {
        long now = level.getGameTime();
        if (acceptorFaces != null && now - lastRecompute < RECOMPUTE_INTERVAL) {
            return;
        }
        Map<BlockPos, Direction> acceptors = new LinkedHashMap<>();
        Set<BlockPos> visited = new HashSet<>();
        Deque<BlockPos> queue = new ArrayDeque<>();
        queue.add(origin);
        visited.add(origin);

        while (!queue.isEmpty() && visited.size() < MAX_NETWORK) {
            BlockPos pos = queue.poll();
            for (Direction dir : Direction.values()) {
                BlockPos next = pos.relative(dir);
                if (!visited.add(next)) {
                    continue;
                }
                if (level.getBlockEntity(next) instanceof WaterConduitBlockEntity) {
                    queue.add(next);
                } else {
                    Direction face = dir.getOpposite();
                    IEnergyStorage e = level.getCapability(Capabilities.EnergyStorage.BLOCK, next, face);
                    if (e != null && e.canReceive()) {
                        acceptors.put(next.immutable(), face);
                    }
                }
            }
        }
        this.acceptorFaces = acceptors;
        this.lastRecompute = now;
    }
}
