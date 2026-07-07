package com.pgmacdesign.mcwaterslides.registry;

import java.util.LinkedHashMap;
import java.util.Map;


import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MCWaterSlides.MOD_ID);

    /** Keyed like {@link ModBlocks#SLIDE_CHANNELS} — null = the natural channel. */
    public static final Map<DyeColor, DeferredItem<BlockItem>> SLIDE_CHANNEL_ITEMS = new LinkedHashMap<>();

    public static final DeferredItem<BlockItem> JET;

    static {
        ModBlocks.SLIDE_CHANNELS.forEach((color, block) ->
                SLIDE_CHANNEL_ITEMS.put(color, ITEMS.registerSimpleBlockItem(block)));
        JET = ITEMS.registerSimpleBlockItem(ModBlocks.JET);
    }

    private ModItems() {}
}
