package com.pgmacdesign.mcwaterslides.machine;

import com.pgmacdesign.mcwaterslides.config.MCWaterslidesConfig;
import com.pgmacdesign.mcwaterslides.current.CurrentField;
import com.pgmacdesign.mcwaterslides.current.CurrentFields;
import com.pgmacdesign.mcwaterslides.registry.ModBlockEntities;
import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.ints.IntSet;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.energy.EnergyStorage;
import net.neoforged.neoforge.energy.IEnergyStorage;

/**
 * Owns one {@link CurrentField} (single-writer, published by reference swap) and the RF
 * gate. Tick order: bill THIS tick's cost first (idle + surge when riders are present),
 * then publish energized — samplers push only against a paid-up jet (no push on credit).
 *
 * Client instances register too (field sampling for prediction) but never tick: their
 * energized state comes from the {@code ENERGIZED} blockstate, and their field cache
 * revalidates on a short TTL since block updates don't invalidate client-side.
 */
public class JetBlockEntity extends BlockEntity {
    private static final int CLIENT_FIELD_TTL_TICKS = 20;

    /** Receive-only from the outside; the jet spends internally through {@link #consume}. */
    private static final class JetEnergy extends EnergyStorage {
        JetEnergy(int capacity) {
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

    private final JetEnergy energy = new JetEnergy(MCWaterslidesConfig.JET_BUFFER_RF.get());

    private volatile CurrentField field = CurrentField.EMPTY;
    private boolean fieldDirty = true;
    private long fieldComputedAt = Long.MIN_VALUE;
    private boolean energizedThisTick;
    /** Populated only when more entities than the cap sit in the field (rare path). */
    private IntSet allowedPushIds = null;

    public JetBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.JET.get(), pos, state);
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, JetBlockEntity jet) {
        if (!state.getValue(JetBlock.ENABLED)) {
            jet.retire(state, level, pos);
            return;
        }

        CurrentField current = jet.validatedField(level, pos, state);

        // Count riders now so THIS tick's whole cost is billed at a single draw point.
        int riders = 0;
        jet.allowedPushIds = null;
        if (!current.isEmpty()) {
            var entities = level.getEntitiesOfClass(LivingEntity.class, current.bounds(),
                    e -> current.contains(BlockPos.containing(e.position())));
            riders = entities.size();
            int cap = MCWaterslidesConfig.MAX_PUSHED_ENTITIES_PER_JET.get();
            if (riders > cap) {
                entities.sort(java.util.Comparator.comparingInt(Entity::getId));
                IntSet allowed = new IntOpenHashSet(cap);
                for (int i = 0; i < cap; i++) {
                    allowed.add(entities.get(i).getId());
                }
                jet.allowedPushIds = allowed;
            }
        }

        int cost = MCWaterslidesConfig.JET_IDLE_RF.get()
                + (riders > 0 ? MCWaterslidesConfig.JET_PUSH_RF.get() : 0);
        jet.energizedThisTick = !current.isEmpty() && jet.consume(cost);

        boolean energizedState = state.getValue(JetBlock.ENERGIZED);
        if (energizedState != jet.energizedThisTick) {
            level.setBlock(pos, state.setValue(JetBlock.ENERGIZED, jet.energizedThisTick), Block.UPDATE_ALL);
        }
    }

    private void retire(BlockState state, Level level, BlockPos pos) {
        energizedThisTick = false;
        allowedPushIds = null;
        if (state.getValue(JetBlock.ENERGIZED)) {
            level.setBlock(pos, state.setValue(JetBlock.ENERGIZED, false), Block.UPDATE_ALL);
        }
    }

    /** True when riders may push against this jet this tick (both sides). */
    public boolean isEnergized() {
        Level level = getLevel();
        if (level != null && level.isClientSide) {
            return getBlockState().getValue(JetBlock.ENERGIZED) && getBlockState().getValue(JetBlock.ENABLED);
        }
        return energizedThisTick;
    }

    /** Per-jet entity cap: everyone pushes unless the rare over-cap allowlist is active. */
    public boolean allowsPush(Entity entity) {
        Level level = getLevel();
        if (level != null && level.isClientSide) {
            return true; // server billing corrects momentum via the ride sync
        }
        IntSet allowed = allowedPushIds;
        return allowed == null || allowed.contains(entity.getId());
    }

    /** The current field — lazily validated on the client via TTL. */
    public CurrentField field() {
        Level level = getLevel();
        if (level != null && level.isClientSide
                && level.getGameTime() - fieldComputedAt > CLIENT_FIELD_TTL_TICKS) {
            fieldDirty = true;
        }
        if (fieldDirty && level != null) {
            field = computeNow(level);
            fieldDirty = false;
            fieldComputedAt = level.getGameTime();
        }
        return field;
    }

    private CurrentField validatedField(Level level, BlockPos pos, BlockState state) {
        if (fieldDirty) {
            field = computeNow(level);
            fieldDirty = false;
            fieldComputedAt = level.getGameTime();
        }
        return field;
    }

    private CurrentField computeNow(Level level) {
        return CurrentField.compute(level, getBlockPos(),
                getBlockState().getValue(JetBlock.FACING),
                MCWaterslidesConfig.JET_RANGE.get());
    }

    /** Idempotent: marking dirty twice schedules exactly one recompute (next sample). */
    public void invalidateField() {
        fieldDirty = true;
    }

    public IEnergyStorage energyHandler() {
        return energy;
    }

    private boolean consume(int amount) {
        return energy.consume(amount);
    }

    /** Test/support hook (gametests fill the buffer directly until the generator lands). */
    public void fillBuffer() {
        energy.setStored(Integer.MAX_VALUE);
        setChanged();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        CurrentFields.register(this);
    }

    @Override
    public void setRemoved() {
        CurrentFields.unregister(this);
        super.setRemoved();
    }

    @Override
    public void onChunkUnloaded() {
        CurrentFields.unregister(this);
        super.onChunkUnloaded();
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
