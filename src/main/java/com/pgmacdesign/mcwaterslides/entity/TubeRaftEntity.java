package com.pgmacdesign.mcwaterslides.entity;

import javax.annotation.Nullable;

import com.pgmacdesign.mcwaterslides.registry.ModEntities;
import com.pgmacdesign.mcwaterslides.registry.ModItems;
import com.pgmacdesign.mcwaterslides.ride.RideState;
import com.pgmacdesign.mcwaterslides.ride.RideTicker;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.tags.DamageTypeTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.DyeItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.phys.Vec3;

/**
 * The inner tube: a single-seat, boat-like raft. It carries no bespoke movement math — it is
 * just another rider entity, so {@link RideTicker} (generalized to {@code Entity}) flows it
 * through slides, tubes, corners, swing-valleys, jets, and funnels EXACTLY as an on-foot rider,
 * plus buoyancy so it floats on real (valve-flooded / vanilla) water. The passenger sits upright
 * with a free camera (vanilla passenger look), which is the whole point of the tube.
 *
 * Netcode is the standard vehicle model: when the local player is aboard it is the controlling
 * instance (client simulates + streams the vehicle-move packet, no rubber-band); otherwise the
 * server drives and clients interpolate via {@link #tickLerp()}.
 */
public class TubeRaftEntity extends Entity {
    private static final EntityDataAccessor<Integer> DATA_COLOR =
            SynchedEntityData.defineId(TubeRaftEntity.class, EntityDataSerializers.INT);

    /** The raft's own ride state — the passenger's player state stays idle while aboard. */
    private final RideState rideState = new RideState();

    private int lerpSteps;
    private double lerpX, lerpY, lerpZ, lerpYRot, lerpXRot;

    public TubeRaftEntity(EntityType<? extends TubeRaftEntity> type, Level level) {
        super(type, level);
        this.blocksBuilding = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_COLOR, -1);
    }

    /** Natural (undyed) when null; otherwise the dye applied by sneak-right-click. */
    @Nullable
    public DyeColor getColor() {
        int id = this.entityData.get(DATA_COLOR);
        return id < 0 ? null : DyeColor.byId(id);
    }

    public void setColor(@Nullable DyeColor color) {
        this.entityData.set(DATA_COLOR, color == null ? -1 : color.getId());
    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        return getFirstPassenger() instanceof LivingEntity living ? living : null;
    }

    @Override
    public boolean canAddPassenger(Entity passenger) {
        return getPassengers().isEmpty();
    }

    @Override
    public boolean isPickable() {
        return !isRemoved();
    }

    @Override
    public void tick() {
        super.tick();
        tickLerp();
        if (isControlledByLocalInstance()) {
            updateMotion();
            move(MoverType.SELF, getDeltaMovement());
            // Residual drag governs only when the ride engine isn't actively driving (e.g.
            // coasting to a stop on still water); while riding, RideTicker resets x/z each tick.
            setDeltaMovement(getDeltaMovement().multiply(0.99, 0.98, 0.99));
        } else {
            setDeltaMovement(Vec3.ZERO);
        }
    }

    private void updateMotion() {
        Vec3 d = getDeltaMovement();
        double surface = waterSurfaceTarget();
        if (!Double.isNaN(surface)) {
            // Buoyancy: ease toward the water surface (bob), no gravity.
            setDeltaMovement(d.x, Mth.clamp(surface - getY(), -0.15, 0.15), d.z);
        } else {
            setDeltaMovement(d.x, d.y - 0.04, d.z);
        }
        // The raft is just another rider — slides/tubes/funnels/jets drive it identically.
        RideTicker.tick(this, rideState, false, true);
    }

    /** Top-of-water y to float at, or NaN when there's no real water here (dry slide / air). */
    private double waterSurfaceTarget() {
        BlockPos p = blockPosition();
        for (BlockPos c : new BlockPos[]{p, p.below()}) {
            FluidState fs = level().getFluidState(c);
            if (fs.is(FluidTags.WATER)) {
                return c.getY() + fs.getHeight(level(), c) - 0.12;
            }
        }
        return Double.NaN;
    }

    private void tickLerp() {
        if (isControlledByLocalInstance()) {
            lerpSteps = 0;
            return;
        }
        if (lerpSteps > 0) {
            double nx = getX() + (lerpX - getX()) / lerpSteps;
            double ny = getY() + (lerpY - getY()) / lerpSteps;
            double nz = getZ() + (lerpZ - getZ()) / lerpSteps;
            float yr = getYRot() + (float) (Mth.wrapDegrees(lerpYRot - getYRot()) / lerpSteps);
            float xr = getXRot() + (float) ((lerpXRot - getXRot()) / lerpSteps);
            lerpSteps--;
            setPos(nx, ny, nz);
            setRot(yr, xr);
        }
    }

    @Override
    public void lerpTo(double x, double y, double z, float yRot, float xRot, int steps) {
        this.lerpX = x;
        this.lerpY = y;
        this.lerpZ = z;
        this.lerpYRot = yRot;
        this.lerpXRot = xRot;
        this.lerpSteps = steps;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        ItemStack held = player.getItemInHand(hand);
        // Sneak + dye recolors the placed tube (the dye-tint story, no 17 item variants).
        if (player.isSecondaryUseActive() && held.getItem() instanceof DyeItem dye) {
            if (!level().isClientSide) {
                setColor(dye.getDyeColor());
                held.consume(1, player);
            }
            return InteractionResult.sidedSuccess(level().isClientSide());
        }
        if (player.isPassenger()) {
            return InteractionResult.PASS;
        }
        if (!level().isClientSide) {
            return player.startRiding(this) ? InteractionResult.CONSUME : InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (isInvulnerableTo(source) || isRemoved()) {
            return false;
        }
        if (level().isClientSide) {
            return true;
        }
        boolean creative = source.getEntity() instanceof Player p && p.getAbilities().instabuild;
        if (!creative && !source.is(DamageTypeTags.IS_EXPLOSION)) {
            spawnAtLocation(ModItems.INNER_TUBE.get());
        }
        discard();
        return true;
    }

    @Override
    public ItemStack getPickResult() {
        return new ItemStack(ModItems.INNER_TUBE.get());
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag) {
        if (tag.contains("Color")) {
            this.entityData.set(DATA_COLOR, tag.getInt("Color"));
        }
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag) {
        tag.putInt("Color", this.entityData.get(DATA_COLOR));
    }
}
