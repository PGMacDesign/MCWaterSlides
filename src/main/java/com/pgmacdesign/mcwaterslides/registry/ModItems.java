package com.pgmacdesign.mcwaterslides.registry;

import java.util.LinkedHashMap;
import java.util.Map;


import com.pgmacdesign.mcwaterslides.MCWaterSlides;
import com.pgmacdesign.mcwaterslides.slide.SlideChannelBlockItem;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.neoforged.neoforge.registries.DeferredItem;
import net.neoforged.neoforge.registries.DeferredRegister;

public final class ModItems {
    public static final DeferredRegister.Items ITEMS =
            DeferredRegister.createItems(MCWaterSlides.MOD_ID);

    /** Keyed like {@link ModBlocks#SLIDE_CHANNELS} — null = the natural channel. */
    public static final Map<DyeColor, DeferredItem<BlockItem>> SLIDE_CHANNEL_ITEMS = new LinkedHashMap<>();
    public static final Map<DyeColor, DeferredItem<BlockItem>> SLIDE_TUBE_ITEMS = new LinkedHashMap<>();

    public static final DeferredItem<BlockItem> JET;
    public static final DeferredItem<BlockItem> PUMP_HOUSE;
    public static final DeferredItem<BlockItem> WATER_CONDUIT;
    public static final DeferredItem<BlockItem> FLOOD_VALVE;
    public static final DeferredItem<BlockItem> SPLASH_POOL;

    static {
        // Channels get run-extending placement (open-top raycast lands on interior
        // faces; vanilla resolves those as "place above"). Tubes stay vanilla: their
        // lid-top click is how tall bores are stacked.
        ModBlocks.SLIDE_CHANNELS.forEach((color, block) ->
                SLIDE_CHANNEL_ITEMS.put(color, ITEMS.register(block.getId().getPath(),
                        () -> new SlideChannelBlockItem(block.get(), new Item.Properties()))));
        ModBlocks.SLIDE_TUBES.forEach((color, block) ->
                SLIDE_TUBE_ITEMS.put(color, ITEMS.registerSimpleBlockItem(block)));
        JET = ITEMS.registerSimpleBlockItem(ModBlocks.JET);
        PUMP_HOUSE = ITEMS.registerSimpleBlockItem(ModBlocks.PUMP_HOUSE);
        WATER_CONDUIT = ITEMS.registerSimpleBlockItem(ModBlocks.WATER_CONDUIT);
        FLOOD_VALVE = ITEMS.registerSimpleBlockItem(ModBlocks.FLOOD_VALVE);
        SPLASH_POOL = ITEMS.registerSimpleBlockItem(ModBlocks.SPLASH_POOL);
    }

    private ModItems() {}
}
