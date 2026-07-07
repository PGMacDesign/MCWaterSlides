package com.pgmacdesign.mcwaterslides.machine;

import java.util.function.IntUnaryOperator;

import net.minecraft.world.inventory.ContainerData;

/**
 * Vanilla syncs ContainerData values to the client as 16-bit shorts, which silently
 * corrupts RF values (100,000 RF does not fit). This wrapper splits every logical value
 * into a low/high half — each fits in a short — and menus recombine with {@link #combine}.
 *
 * Server menus wrap the machine's live values; client menus must use a plain
 * {@code SimpleContainerData} of {@link #slotCount} so the sync packets have somewhere
 * to land. (Ported from MC3DPrint, where the live-wrapper-on-client mistake showed as a
 * GUI stuck at zero forever.)
 */
public final class SplitContainerData implements ContainerData {
    private final int logicalCount;
    private final IntUnaryOperator source;

    public SplitContainerData(int logicalCount, IntUnaryOperator source) {
        this.logicalCount = logicalCount;
        this.source = source;
    }

    @Override
    public int get(int index) {
        int value = source.applyAsInt(index >> 1);
        return (index & 1) == 0 ? value & 0xFFFF : (value >>> 16) & 0xFFFF;
    }

    @Override
    public void set(int index, int value) {
        // server-side values are live; nothing to write back
    }

    @Override
    public int getCount() {
        return slotCount(logicalCount);
    }

    public static int slotCount(int logicalCount) {
        return logicalCount * 2;
    }

    /** Recombines the two halves of a logical value (halves may arrive sign-extended). */
    public static int combine(ContainerData data, int logicalIndex) {
        int low = data.get(logicalIndex * 2) & 0xFFFF;
        int high = data.get(logicalIndex * 2 + 1) & 0xFFFF;
        return (high << 16) | low;
    }
}
