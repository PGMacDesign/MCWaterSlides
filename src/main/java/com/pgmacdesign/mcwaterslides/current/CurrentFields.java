package com.pgmacdesign.mcwaterslides.current;

import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.ConcurrentHashMap;

import com.pgmacdesign.mcwaterslides.machine.JetBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Per-level registry of loaded jets, used by riders to sample thrust with containment
 * lookups (never a world search). Registered from {@code onLoad}, retired from
 * {@code setRemoved}/unload — both idempotent map ops, on BOTH sides: the client
 * registers its own jet BEs so the local player predicts thrust smoothly (the
 * bubble-column principle: block-driven motion evaluated on each side locally).
 */
public final class CurrentFields {
    // Weak on the level key so dimensions/integrated-server worlds unload cleanly.
    private static final Map<Level, Map<BlockPos, JetBlockEntity>> JETS = new WeakHashMap<>();

    private CurrentFields() {}

    public static synchronized void register(JetBlockEntity jet) {
        JETS.computeIfAbsent(jet.getLevel(), l -> new ConcurrentHashMap<>())
                .put(jet.getBlockPos().immutable(), jet);
    }

    public static synchronized void unregister(JetBlockEntity jet) {
        Map<BlockPos, JetBlockEntity> jets = JETS.get(jet.getLevel());
        if (jets != null) {
            jets.remove(jet.getBlockPos(), jet);
        }
    }

    private static synchronized Map<BlockPos, JetBlockEntity> jetsIn(Level level) {
        Map<BlockPos, JetBlockEntity> jets = JETS.get(level);
        return jets == null ? Map.of() : jets;
    }

    /**
     * Invalidate any jet whose field bounds contain a changed block. This is the cache-
     * coherence guard for edits DEEP inside a field — the jet's own neighborChanged only
     * sees adjacent changes. Wired to NeighborNotifyEvent (server side).
     */
    public static void invalidateAt(Level level, BlockPos pos) {
        Vec3 point = Vec3.atCenterOf(pos);
        for (JetBlockEntity jet : jetsIn(level).values()) {
            if (!jet.field().isEmpty() && jet.field().bounds().contains(point)) {
                jet.invalidateField();
            }
        }
    }

    /**
     * Aggregate thrust (blocks/sec per second, as a vector) acting on an entity's feet
     * from every energized jet whose field contains them.
     */
    public static Vec3 sampleThrust(Level level, Entity entity, double thrustPerSecond) {
        BlockPos feet = BlockPos.containing(entity.position());
        Vec3 thrust = Vec3.ZERO;
        for (JetBlockEntity jet : jetsIn(level).values()) {
            if (jet.isEnergized() && jet.allowsPush(entity) && jet.field().contains(feet)) {
                thrust = thrust.add(Vec3.atLowerCornerOf(jet.field().flow().getNormal()).scale(thrustPerSecond));
            }
        }
        return thrust;
    }
}
