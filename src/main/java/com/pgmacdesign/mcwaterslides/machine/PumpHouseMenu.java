package com.pgmacdesign.mcwaterslides.machine;

import javax.annotation.Nullable;

import com.pgmacdesign.mcwaterslides.registry.ModMenuTypes;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

/** One fuel slot + synced energy/burn/rate values (split shorts — see SplitContainerData). */
public class PumpHouseMenu extends AbstractContainerMenu {
    public static final int FUEL_SLOT = 0;
    public static final int SLOT_COUNT = 1;

    @Nullable
    private final PumpHouseBlockEntity pumpHouse;
    private final ContainerData data;

    public PumpHouseMenu(int windowId, Inventory playerInventory, FriendlyByteBuf buf) {
        this(windowId, playerInventory, clientBlockEntity(playerInventory, buf),
                new SimpleContainerData(SplitContainerData.slotCount(PumpHouseBlockEntity.DATA_COUNT)));
    }

    public PumpHouseMenu(int windowId, Inventory playerInventory, @Nullable PumpHouseBlockEntity pumpHouse) {
        this(windowId, playerInventory, pumpHouse, pumpHouse != null ? pumpHouse.containerData()
                : new SimpleContainerData(SplitContainerData.slotCount(PumpHouseBlockEntity.DATA_COUNT)));
    }

    private PumpHouseMenu(int windowId, Inventory playerInventory,
                          @Nullable PumpHouseBlockEntity pumpHouse, ContainerData data) {
        super(ModMenuTypes.PUMP_HOUSE.get(), windowId);
        this.pumpHouse = pumpHouse;
        this.data = data;

        IItemHandler fuel = pumpHouse != null ? pumpHouse.fuel() : new ItemStackHandler(SLOT_COUNT);
        addSlot(new SlotItemHandler(fuel, FUEL_SLOT, 80, 53) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return PumpHouseBlockEntity.isFuel(stack);
            }
        });

        for (int row = 0; row < 3; row++) {
            for (int col = 0; col < 9; col++) {
                addSlot(new Slot(playerInventory, col + row * 9 + 9, 8 + col * 18, 84 + row * 18));
            }
        }
        for (int col = 0; col < 9; col++) {
            addSlot(new Slot(playerInventory, col, 8 + col * 18, 142));
        }

        addDataSlots(data);
    }

    @Nullable
    private static PumpHouseBlockEntity clientBlockEntity(Inventory playerInventory, FriendlyByteBuf buf) {
        return playerInventory.player.level().getBlockEntity(buf.readBlockPos()) instanceof PumpHouseBlockEntity be
                ? be : null;
    }

    public int energy() {
        return SplitContainerData.combine(data, PumpHouseBlockEntity.DATA_ENERGY);
    }

    public int maxEnergy() {
        return Math.max(1, SplitContainerData.combine(data, PumpHouseBlockEntity.DATA_MAX_ENERGY));
    }

    public int burnRemaining() {
        return SplitContainerData.combine(data, PumpHouseBlockEntity.DATA_BURN_REMAINING);
    }

    public int burnTotal() {
        return SplitContainerData.combine(data, PumpHouseBlockEntity.DATA_BURN_TOTAL);
    }

    /** RF/t being produced right now (0 when idle). */
    public int genRate() {
        return SplitContainerData.combine(data, PumpHouseBlockEntity.DATA_RATE);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int slotIndex) {
        Slot slot = slots.get(slotIndex);
        if (!slot.hasItem()) {
            return ItemStack.EMPTY;
        }
        ItemStack moved = slot.getItem();
        ItemStack original = moved.copy();

        if (slotIndex < SLOT_COUNT) {
            if (!moveItemStackTo(moved, SLOT_COUNT, slots.size(), true)) {
                return ItemStack.EMPTY;
            }
        } else if (PumpHouseBlockEntity.isFuel(moved)) {
            if (!moveItemStackTo(moved, FUEL_SLOT, FUEL_SLOT + 1, false)) {
                return ItemStack.EMPTY;
            }
        } else {
            return ItemStack.EMPTY;
        }

        if (moved.isEmpty()) {
            slot.set(ItemStack.EMPTY);
        } else {
            slot.setChanged();
        }
        return original;
    }

    @Override
    public boolean stillValid(Player player) {
        if (pumpHouse == null || pumpHouse.getLevel() == null) {
            return false;
        }
        return pumpHouse.getLevel().getBlockEntity(pumpHouse.getBlockPos()) == pumpHouse
                && player.distanceToSqr(net.minecraft.world.phys.Vec3.atCenterOf(pumpHouse.getBlockPos())) <= 64.0;
    }
}
