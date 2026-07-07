package com.pgmacdesign.mcwaterslides.machine;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.pgmacdesign.mcwaterslides.config.MCWaterslidesConfig;
import com.pgmacdesign.mcwaterslides.registry.ModBlockEntities;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.MenuProvider;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;

/**
 * Dual-mode entry-level generator so the mod runs with zero tech mods installed.
 * Burning furnace fuel: 20 RF/t at a 5× burn multiplier — one coal = 160,000 RF, the
 * exact energy-per-coal of MC3DPrint's Clock Generator (10 RF/t at 10×) at double rate:
 * deliberately never an efficiency upgrade over it, just more watts now. Idle beside
 * water: a token passive trickle for lazy rivers.
 */
public class PumpHouseBlockEntity extends BlockEntity implements MenuProvider {
    public static final int BUFFER_CAPACITY = 100_000;

    public static final int DATA_ENERGY = 0;
    public static final int DATA_MAX_ENERGY = 1;
    public static final int DATA_BURN_REMAINING = 2;
    public static final int DATA_BURN_TOTAL = 3;
    public static final int DATA_RATE = 4;
    public static final int DATA_COUNT = 5;

    private int stored;
    private int burnRemaining;
    /** Boosted burn time of the igniting fuel; 0 when not burning. Drives the flame fill. */
    private int burnTotal;

    private final ItemStackHandler fuel = new ItemStackHandler(1) {
        @Override
        public boolean isItemValid(int slot, @Nonnull ItemStack stack) {
            return isFuel(stack);
        }

        @Override
        protected void onContentsChanged(int slot) {
            setChanged();
        }
    };

    private final IEnergyStorage energy = new IEnergyStorage() {
        @Override
        public int receiveEnergy(int maxReceive, boolean simulate) {
            return 0;
        }

        @Override
        public int extractEnergy(int maxExtract, boolean simulate) {
            int extracted = Math.min(maxExtract, stored);
            if (extracted > 0 && !simulate) {
                stored -= extracted;
                setChanged();
            }
            return extracted;
        }

        @Override
        public int getEnergyStored() {
            return stored;
        }

        @Override
        public int getMaxEnergyStored() {
            return BUFFER_CAPACITY;
        }

        @Override
        public boolean canExtract() {
            return true;
        }

        @Override
        public boolean canReceive() {
            return false;
        }
    };

    public PumpHouseBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.PUMP_HOUSE.get(), pos, state);
    }

    public static int ratePerTick() {
        return MCWaterslidesConfig.PUMP_HOUSE_RF_PER_TICK.get();
    }

    public static int burnMultiplier() {
        return MCWaterslidesConfig.PUMP_HOUSE_BURN_MULTIPLIER.get();
    }

    public static int passiveRate() {
        return MCWaterslidesConfig.PUMP_HOUSE_PASSIVE_RF.get();
    }

    public static boolean isFuel(ItemStack stack) {
        return stack.getBurnTime(RecipeType.SMELTING) > 0;
    }

    public int storedEnergy() {
        return stored;
    }

    /** RF/t being produced right now: burn rate, passive trickle, or 0. */
    public int currentRate() {
        if (burnRemaining > 0) {
            return ratePerTick();
        }
        Level level = getLevel();
        return level != null && hasAdjacentWater(level, getBlockPos()) ? passiveRate() : 0;
    }

    public ContainerData containerData() {
        return new SplitContainerData(DATA_COUNT, this::dataValue);
    }

    private int dataValue(int index) {
        return switch (index) {
            case DATA_ENERGY -> stored;
            case DATA_MAX_ENERGY -> BUFFER_CAPACITY;
            case DATA_BURN_REMAINING -> burnRemaining;
            case DATA_BURN_TOTAL -> burnTotal;
            case DATA_RATE -> currentRate();
            default -> 0;
        };
    }

    public ItemStackHandler fuel() {
        return fuel;
    }

    /** Test hook + hand-feed support; returns boosted burn time or 0 if rejected. */
    public int addFuel(ItemStack held) {
        if (!isFuel(held)) {
            return 0;
        }
        ItemStack one = held.copyWithCount(1);
        if (!fuel.insertItem(0, one, false).isEmpty()) {
            return 0;
        }
        held.shrink(1);
        return one.getBurnTime(RecipeType.SMELTING) * burnMultiplier();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, PumpHouseBlockEntity be) {
        be.tick(level, pos, state);
    }

    private void tick(Level level, BlockPos pos, BlockState state) {
        int before = stored;

        // Ignite the next fuel item only when there's room to use the output.
        if (burnRemaining <= 0 && stored < BUFFER_CAPACITY) {
            ItemStack next = fuel.extractItem(0, 1, false);
            if (!next.isEmpty()) {
                burnRemaining = next.getBurnTime(RecipeType.SMELTING) * burnMultiplier();
                burnTotal = burnRemaining;
                // burnable containers (lava bucket) leave their empty container behind
                ItemStack remainder = next.getCraftingRemainingItem();
                if (!remainder.isEmpty() && fuel.getStackInSlot(0).isEmpty()) {
                    fuel.setStackInSlot(0, remainder);
                }
            }
        }

        if (burnRemaining > 0) {
            burnRemaining--;
            stored = Math.min(stored + ratePerTick(), BUFFER_CAPACITY);
            if (burnRemaining <= 0) {
                burnTotal = 0;
            }
        } else if (stored < BUFFER_CAPACITY && hasAdjacentWater(level, pos)) {
            stored = Math.min(stored + passiveRate(), BUFFER_CAPACITY);
        }

        // Push to adjacent receivers (jets, conduits, other mods' machines).
        for (Direction direction : Direction.values()) {
            if (stored <= 0) {
                break;
            }
            IEnergyStorage handler = level.getCapability(Capabilities.EnergyStorage.BLOCK,
                    pos.relative(direction), direction.getOpposite());
            if (handler != null && handler.canReceive()) {
                stored -= handler.receiveEnergy(stored, false);
            }
        }

        boolean lit = burnRemaining > 0;
        if (state.getValue(PumpHouseBlock.LIT) != lit) {
            level.setBlock(pos, state.setValue(PumpHouseBlock.LIT, lit), Block.UPDATE_ALL);
        }

        if (stored != before || burnRemaining > 0) {
            setChanged();
        }
    }

    /** Any adjacent water counts — rivers are source blocks; strict "flowing" would exclude them. */
    public static boolean hasAdjacentWater(Level level, BlockPos pos) {
        for (Direction direction : Direction.values()) {
            if (level.getFluidState(pos.relative(direction)).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    public IEnergyStorage energyHandler() {
        return energy;
    }

    /** Fuel handler on every face (hopper feed). */
    public IItemHandler itemHandler(@Nullable Direction side) {
        return fuel;
    }

    @Override
    public Component getDisplayName() {
        return getBlockState().getBlock().getName();
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int windowId, Inventory playerInventory, Player player) {
        return new PumpHouseMenu(windowId, playerInventory, this);
    }

    @Override
    protected void saveAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.saveAdditional(tag, registries);
        tag.putInt("Energy", stored);
        tag.putInt("BurnRemaining", burnRemaining);
        tag.putInt("BurnTotal", burnTotal);
        tag.put("Fuel", fuel.serializeNBT(registries));
    }

    @Override
    protected void loadAdditional(CompoundTag tag, HolderLookup.Provider registries) {
        super.loadAdditional(tag, registries);
        stored = tag.getInt("Energy");
        burnRemaining = tag.getInt("BurnRemaining");
        burnTotal = tag.getInt("BurnTotal");
        if (tag.contains("Fuel")) {
            fuel.deserializeNBT(registries, tag.getCompound("Fuel"));
        }
    }
}
