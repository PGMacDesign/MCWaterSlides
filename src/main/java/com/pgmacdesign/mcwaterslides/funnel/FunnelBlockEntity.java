package com.pgmacdesign.mcwaterslides.funnel;

import com.pgmacdesign.mcwaterslides.config.MCWaterslidesConfig;
import com.pgmacdesign.mcwaterslides.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * A tornado core's presence marker. It stores no state (size + facing live in the blockstate,
 * the exit anchor is the block pos), never ticks, and exists purely to register/unregister in
 * {@link FunnelFields} on load and removal — the ride itself is driven per-rider by
 * {@code RideTicker} (both sides), exactly like the jet current. Passive: no RF, no energy.
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

    private Direction facing() {
        return getBlockState().getBlock() instanceof FunnelCoreBlock
                ? getBlockState().getValue(FunnelCoreBlock.FACING)
                : Direction.NORTH;
    }

    /** The exit anchor: the trough bottom at the exit plane (top-center of the core block). */
    public Vec3 anchor() {
        return new Vec3(getBlockPos().getX() + 0.5, getBlockPos().getY() + 1, getBlockPos().getZ() + 0.5);
    }

    /** Unit vector from the exit back toward the mouth (the funnel-frame +a axis). */
    public Vec3 back() {
        Direction d = facing().getOpposite();
        return new Vec3(d.getStepX(), 0, d.getStepZ());
    }

    /** Unit vector across the trough (the funnel-frame +u axis). */
    public Vec3 perp() {
        Direction d = facing().getClockWise();
        return new Vec3(d.getStepX(), 0, d.getStepZ());
    }

    public FunnelPhysics.Params params() {
        FunnelSize s = size();
        return new FunnelPhysics.Params(
                s.mouthRadius(),
                s.exitRadius(),
                s.length(),
                s.drop(),
                MCWaterslidesConfig.FUNNEL_SWING.get(),
                MCWaterslidesConfig.FUNNEL_DRAG.get(),
                MCWaterslidesConfig.FUNNEL_AXIAL_PUSH.get(),
                MCWaterslidesConfig.FUNNEL_MAX_SPEED.get());
    }

    /** True when the entity is inside the cone volume (funnel-frame test, all three axes). */
    public boolean contains(Entity entity) {
        FunnelPhysics.Params p = params();
        Vec3 anchor = anchor();
        Vec3 back = back();
        Vec3 perp = perp();
        double relX = entity.getX() - anchor.x;
        double relZ = entity.getZ() - anchor.z;
        double a = relX * back.x + relZ * back.z;
        if (a < FunnelPhysics.EXIT_MARGIN || a > p.length() + 0.75) {
            return false;
        }
        double u = relX * perp.x + relZ * perp.z;
        double r = FunnelPhysics.radiusAt(Math.max(a, 0), p);
        if (Math.abs(u) > r + 0.75) {
            return false;
        }
        double relY = entity.getY() - anchor.y;
        double bottom = FunnelPhysics.bottomAt(a, p);
        return relY >= bottom - 1.0 && relY <= bottom + r + 1.0;
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
