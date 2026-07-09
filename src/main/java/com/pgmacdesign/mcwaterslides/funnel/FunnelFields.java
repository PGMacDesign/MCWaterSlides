package com.pgmacdesign.mcwaterslides.funnel;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import javax.annotation.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;

/**
 * Per-level registry of loaded funnel cores, so a rider can find "the funnel I'm inside" with a
 * containment check instead of a world scan. Mirrors {@link com.pgmacdesign.mcwaterslides.current.CurrentFields}:
 * registered from the block entity's {@code onLoad} and retired from {@code setRemoved}/unload on
 * BOTH sides — the client needs it too so the local player predicts the swirl (no rubber-band).
 */
public final class FunnelFields {
    private static final Map<Level, Map<BlockPos, FunnelBlockEntity>> FUNNELS = new WeakHashMap<>();

    private FunnelFields() {}

    public static synchronized void register(FunnelBlockEntity funnel) {
        FUNNELS.computeIfAbsent(funnel.getLevel(), l -> new ConcurrentHashMap<>())
                .put(funnel.getBlockPos().immutable(), funnel);
    }

    public static synchronized void unregister(FunnelBlockEntity funnel) {
        Map<BlockPos, FunnelBlockEntity> funnels = FUNNELS.get(funnel.getLevel());
        if (funnels != null) {
            funnels.remove(funnel.getBlockPos(), funnel);
        }
    }

    private static synchronized Map<BlockPos, FunnelBlockEntity> funnelsIn(Level level) {
        Map<BlockPos, FunnelBlockEntity> funnels = FUNNELS.get(level);
        return funnels == null ? Map.of() : funnels;
    }

    /** The funnel whose bowl currently contains this entity, or null. */
    @Nullable
    public static FunnelBlockEntity at(Level level, Entity entity) {
        for (FunnelBlockEntity funnel : funnelsIn(level).values()) {
            if (funnel.contains(entity)) {
                return funnel;
            }
        }
        return null;
    }
}
