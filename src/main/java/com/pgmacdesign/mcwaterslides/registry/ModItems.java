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

    public static final DeferredItem<BlockItem> CLEAR_SLIDE_CHANNEL;
    public static final DeferredItem<BlockItem> CLEAR_SLIDE_TUBE;
    public static final DeferredItem<BlockItem> JET;
    public static final DeferredItem<BlockItem> PUMP_HOUSE;
    public static final DeferredItem<BlockItem> WATER_CONDUIT;
    public static final DeferredItem<BlockItem> FLOOD_VALVE;
    public static final DeferredItem<BlockItem> SPLASH_POOL;
    public static final DeferredItem<com.pgmacdesign.mcwaterslides.entity.TubeRaftItem> INNER_TUBE;
    public static final DeferredItem<BlockItem> FUNNEL_WALL;
    public static final DeferredItem<com.pgmacdesign.mcwaterslides.funnel.FunnelCoreItem> FUNNEL_CORE_SMALL;
    public static final DeferredItem<com.pgmacdesign.mcwaterslides.funnel.FunnelCoreItem> FUNNEL_CORE_MEDIUM;
    public static final DeferredItem<com.pgmacdesign.mcwaterslides.funnel.FunnelCoreItem> FUNNEL_CORE_LARGE;

    static {
        // Channels get run-extending placement (open-top raycast lands on interior
        // faces; vanilla resolves those as "place above"). Tubes stay vanilla: their
        // lid-top click is how tall bores are stacked.
        ModBlocks.SLIDE_CHANNELS.forEach((color, block) ->
                SLIDE_CHANNEL_ITEMS.put(color, ITEMS.register(block.getId().getPath(),
                        () -> new SlideChannelBlockItem(block.get(), new Item.Properties()))));
        ModBlocks.SLIDE_TUBES.forEach((color, block) ->
                SLIDE_TUBE_ITEMS.put(color, ITEMS.registerSimpleBlockItem(block)));
        CLEAR_SLIDE_CHANNEL = ITEMS.register("clear_slide_channel",
                () -> new SlideChannelBlockItem(ModBlocks.CLEAR_SLIDE_CHANNEL.get(), new Item.Properties()));
        CLEAR_SLIDE_TUBE = ITEMS.registerSimpleBlockItem(ModBlocks.CLEAR_SLIDE_TUBE);
        JET = ITEMS.registerSimpleBlockItem(ModBlocks.JET);
        PUMP_HOUSE = ITEMS.registerSimpleBlockItem(ModBlocks.PUMP_HOUSE);
        WATER_CONDUIT = ITEMS.registerSimpleBlockItem(ModBlocks.WATER_CONDUIT);
        FLOOD_VALVE = ITEMS.registerSimpleBlockItem(ModBlocks.FLOOD_VALVE);
        SPLASH_POOL = ITEMS.registerSimpleBlockItem(ModBlocks.SPLASH_POOL);
        INNER_TUBE = ITEMS.register("inner_tube",
                () -> new com.pgmacdesign.mcwaterslides.entity.TubeRaftItem(new Item.Properties().stacksTo(1)));
        FUNNEL_WALL = ITEMS.registerSimpleBlockItem(ModBlocks.FUNNEL_WALL);
        FUNNEL_CORE_SMALL = ITEMS.register("funnel_core_small",
                () -> new com.pgmacdesign.mcwaterslides.funnel.FunnelCoreItem(ModBlocks.FUNNEL_CORE.get(),
                        com.pgmacdesign.mcwaterslides.funnel.FunnelSize.SMALL, new Item.Properties()));
        FUNNEL_CORE_MEDIUM = ITEMS.register("funnel_core_medium",
                () -> new com.pgmacdesign.mcwaterslides.funnel.FunnelCoreItem(ModBlocks.FUNNEL_CORE.get(),
                        com.pgmacdesign.mcwaterslides.funnel.FunnelSize.MEDIUM, new Item.Properties()));
        FUNNEL_CORE_LARGE = ITEMS.register("funnel_core_large",
                () -> new com.pgmacdesign.mcwaterslides.funnel.FunnelCoreItem(ModBlocks.FUNNEL_CORE.get(),
                        com.pgmacdesign.mcwaterslides.funnel.FunnelSize.LARGE, new Item.Properties()));
    }

    private ModItems() {}
}
