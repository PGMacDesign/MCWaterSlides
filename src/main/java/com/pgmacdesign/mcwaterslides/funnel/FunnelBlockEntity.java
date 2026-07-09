package com.pgmacdesign.mcwaterslides.funnel;

import com.pgmacdesign.mcwaterslides.config.MCWaterslidesConfig;
import com.pgmacdesign.mcwaterslides.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A funnel core's presence marker. It stores no state (size lives in the blockstate, center is the
 * block pos), never ticks, and exists purely to register/unregister in {@link FunnelFields} on load
 * and removal — the swirl itself is driven per-rider by {@code RideTicker} (both sides), exactly
 * like the jet current. Passive: no RF, no energy.
 */
public class FunnelBlockEntity extends BlockEntity {
    public FunnelBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.FUNNEL_CORE.get(), pos, state);
    }

    private FunnelSize size() {
        return getBlockState().getBlock() instanceof FunnelCoreBlock
                ? getBlockState().getValue(FunnelCoreBlock.SIZE)
                : FunnelSize.MEDIUM;
    }

    /** The swirl axis: (centerX, drain-lip Y, centerZ). */
    public Vec3 axis() {
        return new Vec3(getBlockPos().getX() + 0.5, getBlockPos().getY(), getBlockPos().getZ() + 0.5);
    }

    public FunnelPhysics.Params params() {
        FunnelSize s = size();
        return new FunnelPhysics.Params(
                MCWaterslidesConfig.FUNNEL_PULL.get(),
                MCWaterslidesConfig.FUNNEL_DRAG.get(),
                s.rimRadius(),
                s.bowlHeight(),
                s.drainRadius(),
                MCWaterslidesConfig.FUNNEL_MAX_SPEED.get());
    }

    /** True when the entity is inside this funnel's bowl cylinder (rim radius × bowl height). */
    public boolean contains(Entity entity) {
        FunnelSize s = size();
        double dx = entity.getX() - (getBlockPos().getX() + 0.5);
        double dz = entity.getZ() - (getBlockPos().getZ() + 0.5);
        double y = entity.getY();
        double baseY = getBlockPos().getY();
        return dx * dx + dz * dz <= (s.rimRadius() + 0.6) * (s.rimRadius() + 0.6)
                && y >= baseY - 0.1 && y <= baseY + s.bowlHeight() + 1.5;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        FunnelFields.register(this);
    }

    @Override
    public void setRemoved() {
        FunnelFields.unregister(this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        FunnelFields.unregister(this);
        super.onChunkUnloaded();
    }
}
